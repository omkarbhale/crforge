/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.pathfinding.grid.TileMap;
import org.crforge.core.util.GameUnits;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.battle.DataVersions;
import org.crforge.desktop.presentation.PresentationHost;
import org.crforge.desktop.render.BattleWorkspace;
import org.crforge.desktop.render.RenderConstants;
import org.crforge.desktop.render.ViewOrientation;
import org.crforge.desktop.render.WorkspaceViewport;
import org.crforge.desktop.screen.DebugGameScreen;
import org.crforge.presentation.BattleStart;
import org.crforge.presentation.CardCommand;
import org.crforge.presentation.PresentContext;
import org.crforge.presentation.PresentationPlugin;
import org.crforge.presentation.TickFrame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A plugin that draws the hand plays from it on the real debug screen, headless ({@link
 * HeadlessGdx}): its commands go through the screen's own selection and play, under the same
 * checks, and a plugin that asks nothing leaves the screen as it was.
 */
class PluginCardPlayTest {

  private static final int WIDTH = 1120;

  private static final int HEIGHT = 1040;

  /** The tile the plays land on: side 0's half, clear of its towers. */
  private static final int TILE_X = 4;

  private static final int TILE_Y = 10;

  /** Ticks the battle runs after a play, past its deploy. */
  private static final int STEPS = 90;

  private static GameTables tables;

  @TempDir Path services;

  private ClassLoader original;

  private HeadlessGdx gdx;

  private CRForgeGame game;

  @BeforeAll
  static void read() throws IOException {
    tables = GameTables.loadConfigured();
  }

  @BeforeEach
  void setUp() {
    original = Thread.currentThread().getContextClassLoader();
    Commanding.last = null;
    gdx = new HeadlessGdx(WIDTH, HEIGHT, 1 / 60f);
  }

  @AfterEach
  void tearDown() {
    Thread.currentThread().setContextClassLoader(original);
    if (game != null) {
      game.dispose();
    }
  }

  @Test
  @DisplayName("with no plugin, the host asks nothing of the hand and a release does nothing")
  void noPluginAsksNothing() throws Exception {
    open();
    assertThat(host().cardCommandAt(WIDTH / 2f, HEIGHT / 2f, false)).isNull();
    assertThat(host().cardCommandAt(WIDTH / 2f, HEIGHT / 2f, true)).isNull();
    PresentContext context = workspace().presentContext();
    assertThat(release(centreX(context), centreY(context))).isFalse();
    assertThat(screenInt("selectedSlot")).isEqualTo(-1);
  }

  @Test
  @DisplayName("a plugin's select and play give the same battle as the hand key and a click")
  void pluginPlaysAsTheHostDoes() throws Exception {
    // The host's own: the hand key, then a click on the tile, as the built-in visualizer has it.
    open();
    int slot = playableSlot();
    key(Input.Keys.NUM_1 + slot);
    assertThat(screenInt("selectedSlot")).isEqualTo(slot);
    run(1);
    PresentContext context = workspace().presentContext();
    assertThat(context.selectedSide()).isZero();
    assertThat(context.selectedSlot()).isEqualTo(slot);
    ViewOrientation view = ViewOrientation.of(tileMap(), context.bottomSide());
    float x = view.px(inside(TILE_X));
    float y = view.py(inside(TILE_Y)) - RenderConstants.BOTTOM_UI_HEIGHT;
    click(
        context.viewportX() + x / WorkspaceViewport.WORLD_WIDTH * context.viewportWidth(),
        context.viewportY() + y / WorkspaceViewport.WORLD_HEIGHT * context.viewportHeight());
    assertThat(screenInt("selectedSlot")).as("deselected after the play").isEqualTo(-1);
    String host = played();
    game.dispose();
    game = null;

    // The plugin's: a press on its drawn card selects it, a release on the arena plays it there.
    install(Commanding.class);
    open();
    Commanding plugin = Commanding.last;
    assertThat(playableSlot()).isEqualTo(slot);
    context = plugin.presents.get(plugin.presents.size() - 1);
    assertThat(context.selectedSlot()).isEqualTo(PresentContext.NO_SELECTION);
    plugin.onPress = new CardCommand.Select(0, slot);
    plugin.onRelease = new CardCommand.Play(inside(TILE_X), inside(TILE_Y));
    float pressX = context.viewportX() + 20;
    float pressY = context.viewportY() + 30;
    assertThat(press(pressX, pressY)).isTrue();
    assertThat(plugin.asked.get(plugin.asked.size() - 1))
        .as("asked in its rectangle's pixels, for a press")
        .containsExactly(20, 30, 0);
    assertThat(screenInt("selectedSide")).isZero();
    assertThat(screenInt("selectedSlot")).isEqualTo(slot);
    run(1);
    assertThat(plugin.presents.get(plugin.presents.size() - 1).selectedSlot())
        .as("the selection told back")
        .isEqualTo(slot);
    assertThat(release(centreX(context), centreY(context))).isTrue();
    assertThat(plugin.asked.get(plugin.asked.size() - 1)[2]).as("a release").isEqualTo(1);
    assertThat(screenInt("selectedSlot")).as("deselected after the play").isEqualTo(-1);
    run(1);
    assertThat(plugin.presents.get(plugin.presents.size() - 1).selectedSlot())
        .isEqualTo(PresentContext.NO_SELECTION);

    assertThat(played()).isEqualTo(host);
    assertThat(plugin.closed).isFalse();
  }

  @Test
  @DisplayName("the host refuses what its own buttons and clicks could not do")
  void refusals() throws Exception {
    install(Commanding.class);
    open();
    Commanding plugin = Commanding.last;
    PresentContext context = plugin.presents.get(plugin.presents.size() - 1);
    float atX = centreX(context);
    float atY = centreY(context);
    int slot = playableSlot();
    int tick = session().tick();

    // No card selected: nothing is played.
    plugin.onPress = new CardCommand.Play(inside(TILE_X), inside(TILE_Y));
    assertThat(press(atX, atY)).isTrue();
    assertThat(plays()).isZero();

    // No such side or slot: the selection stays as it was.
    for (CardCommand.Select select :
        List.of(
            new CardCommand.Select(2, 0),
            new CardCommand.Select(-1, 0),
            new CardCommand.Select(0, 4),
            new CardCommand.Select(0, -1))) {
      plugin.onPress = select;
      assertThat(press(atX, atY)).isTrue();
      assertThat(screenInt("selectedSlot")).as("%s", select).isEqualTo(-1);
    }

    // A position off the arena, or none: the card stays selected and is not played.
    plugin.onPress = new CardCommand.Select(0, slot);
    press(atX, atY);
    for (CardCommand.Play play :
        List.of(
            new CardCommand.Play(-1, inside(TILE_Y)),
            new CardCommand.Play(inside(TILE_X), tileMap().heightUnits()),
            new CardCommand.Play(Float.NaN, inside(TILE_Y)),
            new CardCommand.Play(inside(TILE_X), Float.POSITIVE_INFINITY))) {
      plugin.onPress = play;
      assertThat(press(atX, atY)).isTrue();
      assertThat(screenInt("selectedSlot")).as("%s", play).isEqualTo(slot);
    }
    assertThat(plays()).isZero();

    // A deselect lets the card go.
    plugin.onPress = new CardCommand.Deselect();
    press(atX, atY);
    assertThat(screenInt("selectedSlot")).isEqualTo(-1);
    assertThat(session().tick()).as("paused throughout").isEqualTo(tick);
  }

  @Test
  @DisplayName("a plugin that asks nothing leaves the click to the host, and a play to inspection")
  void nothingAskedLeavesTheClick() throws Exception {
    install(Commanding.class);
    open();
    Commanding plugin = Commanding.last;
    PresentContext context = plugin.presents.get(plugin.presents.size() - 1);
    int slot = playableSlot();

    // Asking nothing, the host's click plays the card its key selected, on the tile under it.
    key(Input.Keys.NUM_1 + slot);
    ViewOrientation view = ViewOrientation.of(tileMap(), context.bottomSide());
    float x = view.px(inside(TILE_X));
    float y = view.py(inside(TILE_Y)) - RenderConstants.BOTTOM_UI_HEIGHT;
    click(
        context.viewportX() + x / WorkspaceViewport.WORLD_WIDTH * context.viewportWidth(),
        context.viewportY() + y / WorkspaceViewport.WORLD_HEIGHT * context.viewportHeight());
    assertThat(plugin.asked).isNotEmpty();
    assertThat(plays()).isOne();
    assertThat(release(centreX(context), centreY(context))).isFalse();

    // A play asked while inspecting is an inspect click.
    key(Input.Keys.I);
    assertThat(workspace().isInspecting()).isTrue();
    plugin.onPress = new CardCommand.Play(inside(TILE_X), inside(TILE_Y));
    assertThat(press(centreX(context), centreY(context))).isTrue();
    assertThat(workspace().isInspecting()).isTrue();
    assertThat(plays()).isOne();
  }

  @Test
  @DisplayName("a plugin whose hand fails is closed, and the host acts on its own")
  void failingCommand() throws Exception {
    install(FailsOnCommand.class);
    open();
    Commanding plugin = Commanding.last;
    PresentContext context = plugin.presents.get(plugin.presents.size() - 1);
    int slot = playableSlot();
    key(Input.Keys.NUM_1 + slot);
    ViewOrientation view = ViewOrientation.of(tileMap(), context.bottomSide());
    float x = view.px(inside(TILE_X));
    float y = view.py(inside(TILE_Y)) - RenderConstants.BOTTOM_UI_HEIGHT;
    click(
        context.viewportX() + x / WorkspaceViewport.WORLD_WIDTH * context.viewportWidth(),
        context.viewportY() + y / WorkspaceViewport.WORLD_HEIGHT * context.viewportHeight());
    assertThat(plugin.closed).isTrue();
    assertThat(plays()).as("the click played as the host's").isOne();
    assertThat(host().cardCommandAt(WIDTH / 2f, HEIGHT / 2f, true)).isNull();
  }

  /** Three quarters into a tile, in game units: clear of any edge a rounding could cross. */
  private static float inside(int tile) {
    return tile * GameUnits.UNITS_PER_TILE + 0.75f * GameUnits.UNITS_PER_TILE;
  }

  private static float centreX(PresentContext context) {
    return context.viewportX() + context.viewportWidth() / 2f;
  }

  private static float centreY(PresentContext context) {
    return context.viewportY() + context.viewportHeight() / 2f;
  }

  /**
   * Side 0's first hand slot whose card it can play, stepping the paused battle one tick at a time
   * until there is one: a battle's opening ticks take no play.
   */
  private int playableSlot() throws Exception {
    for (int step = 0; step < 600; step++) {
      for (int slot = 0; slot < 4; slot++) {
        if (session().cardUnavailableReason(0, slot) == null) {
          return slot;
        }
      }
      key(Input.Keys.PERIOD);
    }
    throw new AssertionError(
        "no card of side 0 is playable: " + session().cardUnavailableReason(0, 0));
  }

  /** The plays the battle was given. */
  private int plays() throws Exception {
    Field field = BattleSession.class.getDeclaredField("playsGiven");
    field.setAccessible(true);
    return (Integer) field.get(session());
  }

  /**
   * Runs the battle on from the play and says what came of it: the session's messages, and each
   * entity's id, name, side, place and hit points at every step.
   */
  private String played() throws Exception {
    assertThat(plays()).isOne();
    int from = session().tick();
    StringBuilder out = new StringBuilder(String.join("\n", session().messages()));
    for (int i = 0; i < STEPS; i++) {
      key(Input.Keys.PERIOD);
      out.append('\n')
          .append(session().tick())
          .append(": ")
          .append(
              BattleAdapter.frame(session()).entities().stream()
                  .map(
                      entity ->
                          entity.id()
                              + " "
                              + entity.name()
                              + " "
                              + entity.side()
                              + " "
                              + entity.x()
                              + ","
                              + entity.y()
                              + " "
                              + entity.hitPoints())
                  .collect(Collectors.joining("; ")));
    }
    assertThat(session().tick()).isEqualTo(from + STEPS);
    return out.toString();
  }

  /** The mouse moves to a window point, its row counted from the window's bottom. */
  private static void hover(float x, float y) {
    int screenX = Math.round(x);
    int screenY = HEIGHT - Math.round(y);
    Gdx.input.setCursorPosition(screenX, screenY);
    Gdx.input.getInputProcessor().mouseMoved(screenX, screenY);
  }

  /** A left click at a window point, its row counted from the window's bottom: press, release. */
  private static void click(float x, float y) {
    press(x, y);
    release(x, y);
  }

  private static boolean press(float x, float y) {
    hover(x, y);
    return Gdx.input
        .getInputProcessor()
        .touchDown(Math.round(x), HEIGHT - Math.round(y), 0, Input.Buttons.LEFT);
  }

  private static boolean release(float x, float y) {
    hover(x, y);
    return Gdx.input
        .getInputProcessor()
        .touchUp(Math.round(x), HEIGHT - Math.round(y), 0, Input.Buttons.LEFT);
  }

  private static void key(int key) {
    Gdx.input.getInputProcessor().keyDown(key);
  }

  /** Puts a plugin's service file on the thread's class loader, as a plugin's jar would. */
  private void install(Class<? extends PresentationPlugin> plugin) throws IOException {
    Path file = services.resolve("META-INF/services/" + PresentationPlugin.class.getName());
    Files.createDirectories(file.getParent());
    Files.writeString(file, plugin.getName() + "\n");
    Thread.currentThread()
        .setContextClassLoader(new URLClassLoader(new URL[] {services.toUri().toURL()}, original));
  }

  /** Opens the debug screen on a new Ladder battle, paused at its first tick, drawn once. */
  private void open() throws Exception {
    Path folder = Path.of(System.getProperty("crforge.gameTables"));
    DataVersions versions =
        new DataVersions(
            folder.getParent(),
            List.of(tables.version()),
            folder,
            tables,
            "test",
            tables.version());
    game = new CRForgeGame(versions, versions.ladder());
    game.create();
    game.resize(WIDTH, HEIGHT);
    gdx.endFrame();
    key(Input.Keys.SPACE);
    run(1);
    assertThat(session().tick()).isZero();
  }

  private void run(int frames) {
    for (int i = 0; i < frames; i++) {
      game.render();
      gdx.endFrame();
    }
  }

  private Object screenField(String name) throws Exception {
    Field field = DebugGameScreen.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(game.getScreen());
  }

  private int screenInt(String name) throws Exception {
    return (Integer) screenField(name);
  }

  private BattleSession session() throws Exception {
    return (BattleSession) screenField("session");
  }

  private TileMap tileMap() throws Exception {
    return session().getBattle().getWorld().getTileMap();
  }

  private PresentationHost host() throws Exception {
    return (PresentationHost) screenField("presentation");
  }

  private BattleWorkspace workspace() throws Exception {
    return (BattleWorkspace) screenField("workspace");
  }

  /** Answers a press and a release with the commands set, and remembers each ask. */
  public static class Commanding implements PresentationPlugin {

    static Commanding last;

    final List<PresentContext> presents = new ArrayList<>();

    /** Each ask: the pixel column and row, and 1 for a release, 0 for a press. */
    final List<int[]> asked = new ArrayList<>();

    CardCommand onPress;

    CardCommand onRelease;

    boolean closed;

    public Commanding() {
      last = this;
    }

    @Override
    public String id() {
      return "commanding";
    }

    @Override
    public void battleStarted(BattleStart battle) {}

    @Override
    public void tick(TickFrame frame) {}

    @Override
    public void present(PresentContext context) {
      presents.add(context);
    }

    @Override
    public CardCommand cardCommandAt(float pixelX, float pixelY, boolean released) {
      asked.add(new int[] {Math.round(pixelX), Math.round(pixelY), released ? 1 : 0});
      return released ? onRelease : onPress;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  /** Fails when asked what a press or a release asks of the hand. */
  public static class FailsOnCommand extends Commanding {
    @Override
    public CardCommand cardCommandAt(float pixelX, float pixelY, boolean released) {
      throw new IllegalStateException("a plugin's bug");
    }
  }
}
