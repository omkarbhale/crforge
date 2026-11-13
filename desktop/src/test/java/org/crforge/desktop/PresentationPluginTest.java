/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.pathfinding.grid.TileMap;
import org.crforge.core.util.GameUnits;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.battle.DataVersions;
import org.crforge.desktop.battle.EntityView;
import org.crforge.desktop.presentation.PresentationHost;
import org.crforge.desktop.render.BattleRenderer;
import org.crforge.desktop.render.BattleWorkspace;
import org.crforge.desktop.render.RenderConstants;
import org.crforge.desktop.render.ViewOrientation;
import org.crforge.desktop.render.WorkspaceViewport;
import org.crforge.desktop.replay.ReplayFile;
import org.crforge.desktop.replay.ReplayPlayer;
import org.crforge.desktop.screen.ReplayGameScreen;
import org.crforge.presentation.ArenaProjection;
import org.crforge.presentation.BattleStart;
import org.crforge.presentation.EntityFrame;
import org.crforge.presentation.EntityKind;
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
 * A plugin found by {@link java.util.ServiceLoader} on the real replay screen, headless ({@link
 * HeadlessGdx}): what it is told and when, and that its failures stay its own.
 */
class PresentationPluginTest {

  private static final int WIDTH = 1120;

  private static final int HEIGHT = 1040;

  /**
   * Tiles the hover tests point into, three quarters of the way across and along each: the corners,
   * both sides' halves, and the far edge, where a whole-rectangle mapping of a smaller drawn arena
   * is furthest off.
   */
  private static final int[][] TILES = {
    {0, 0}, {17, 0}, {0, 31}, {17, 31}, {4, 27}, {9, 15}, {13, 6}
  };

  private static GameTables tables;

  private static Path example;

  @TempDir Path services;

  private ClassLoader original;

  private HeadlessGdx gdx;

  private CRForgeGame game;

  @BeforeAll
  static void read() throws IOException {
    tables = GameTables.loadConfigured();
    example = Path.of(System.getProperty("crforge.exampleReplay"));
    assumeTrue(
        ReplayFile.read(example, tables).playable(),
        "the example replay plays only on the tables of its own data version");
  }

  @BeforeEach
  void setUp() {
    original = Thread.currentThread().getContextClassLoader();
    Recording.last = null;
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
  @DisplayName("with no plugin, the screen hangs nothing on the battle")
  void noPluginAddsNothing() throws Exception {
    open();
    run(10);
    assertThat(Recording.last).isNull();
    assertThat(stepListener(session())).isNull();
  }

  @Test
  @DisplayName("a plugin hears the battle's start, then every tick in order at any speed")
  void everyTick() throws Exception {
    install(Recording.class);
    open();
    for (int i = 0; i < 3; i++) {
      key(Input.Keys.PLUS);
    }
    run(30);

    Recording plugin = Recording.last;
    // A replay's start: the example gives no tower skin data, and its seed, 1.
    assertThat(plugin.starts)
        .containsExactly(new BattleStart(tileMapWidth(), tileMapHeight(), 0, List.of(), 1, true));
    assertThat(session().tick()).as("several ticks a frame at 8x").isGreaterThan(30);
    assertThat(plugin.ticks()).first().isEqualTo(0);
    assertContiguous(plugin.ticks());
    assertThat(plugin.ticks()).last().isEqualTo(session().tick());
    assertThat(plugin.presents).hasSize(30);
    assertThat(plugin.presents.get(0).viewportWidth()).isPositive();
    assertThat(plugin.presents.get(0).bottomSide()).as("a replay opens on side 1's view").isOne();
    assertThat(plugin.frames.get(plugin.frames.size() - 1).entities()).isNotEmpty();
  }

  @Test
  @DisplayName("a pause stops the ticks while the display goes on refreshing")
  void pause() throws Exception {
    install(Recording.class);
    open();
    run(5);
    key(Input.Keys.SPACE);
    Recording plugin = Recording.last;
    int ticks = plugin.frames.size();
    int presents = plugin.presents.size();
    run(5);
    assertThat(plugin.frames).hasSize(ticks);
    assertThat(plugin.presents).hasSize(presents + 5);
  }

  @Test
  @DisplayName("a restart and a step back start a battle: only the tick reached is told")
  void restartAndStepBack() throws Exception {
    install(Recording.class);
    open();
    run(40);
    Recording plugin = Recording.last;
    int reached = session().tick();
    assertThat(reached).isGreaterThan(2);

    key(Input.Keys.COMMA);
    int before = plugin.frames.size();
    run(1);
    assertThat(plugin.starts.get(plugin.starts.size() - 1).tick()).isEqualTo(reached - 1);
    assertThat(plugin.frames).hasSize(before + 1);
    assertThat(plugin.frames.get(before).tick()).isEqualTo(reached - 1);

    key(Input.Keys.R);
    run(1);
    assertThat(plugin.starts).hasSize(3);
    assertThat(plugin.starts.get(2).tick()).isZero();
  }

  @Test
  @DisplayName("a frame a plugin kept stays as it was while the battle runs on")
  void keptFrameIsDetached() throws Exception {
    install(Recording.class);
    open();
    run(5);
    TickFrame kept = Recording.last.frames.get(0);
    String before = kept.toString();
    run(30);
    assertThat(kept.toString()).isEqualTo(before);
  }

  @Test
  @DisplayName(
      "a plugin that fails is closed and the built-in visualizer draws; the battle runs on")
  void failingTick() throws Exception {
    install(FailsOnTick.class);
    open();
    run(1);
    assertThat(Recording.last.closed).isTrue();
    assertFailureContained();
  }

  @Test
  @DisplayName("a plugin that fails to draw is closed and the built-in visualizer draws")
  void failingPresent() throws Exception {
    install(FailsOnPresent.class);
    open();
    HeadlessGdx.Frame frame = run(1);
    assertThat(Recording.last.closed).isTrue();
    assertThat(frame.draws()).as("the arena of the frame the plugin failed on").isPositive();
    assertFailureContained();
  }

  @Test
  @DisplayName("a plugin named but not loadable leaves the built-in visualizer drawing")
  void notLoadable() throws Exception {
    installNamed("org.crforge.desktop.NoSuchPlugin");
    open();
    assertThat(run(1).draws()).isPositive();
    assertThat(stepListener(session())).isNull();
  }

  @Test
  @DisplayName("with no plugin, a hover maps across the arena's whole rectangle, as before")
  void hoverWithoutPlugin() throws Exception {
    open();
    run(1);
    key(Input.Keys.SPACE);
    run(1);
    assertThat(host().drawnArena()).isNull();
    assertHoversAcrossRectangle();
    key(Input.Keys.F);
    run(1);
    assertHoversAcrossRectangle();
  }

  @Test
  @DisplayName("a plugin that reports no projection keeps the whole-rectangle hover")
  void hoverWithoutProjection() throws Exception {
    install(Recording.class);
    open();
    run(1);
    key(Input.Keys.SPACE);
    run(1);
    assertThat(host().drawnArena()).isNull();
    assertHoversAcrossRectangle();
  }

  @Test
  @DisplayName(
      "a plugin's projection maps a hover and a click back to the battle position drawn there")
  void hoverThroughProjection() throws Exception {
    install(Projecting.class);
    open();
    run(1);
    key(Input.Keys.SPACE);
    run(1);
    Projecting plugin = (Projecting) Recording.last;
    assertThat(plugin.drawn.scaleY()).as("a replay opens on side 1's view").isNegative();
    assertHoversThroughProjection(plugin);

    // The click picks the tower drawn under the mouse, not the one the rectangle has there.
    int half = tileMapHeight() / 2;
    EntityFrame tower =
        plugin.frames.get(plugin.frames.size() - 1).entities().stream()
            .filter(entity -> entity.kind() == EntityKind.TOWER)
            .filter(entity -> entity.y() > half)
            .findFirst()
            .orElseThrow();
    PresentContext context = plugin.presents.get(plugin.presents.size() - 1);
    click(
        context.viewportX() + plugin.drawn.pixelX(tower.x()),
        context.viewportY() + plugin.drawn.pixelY(tower.y()));
    assertThat(selected()).isEqualTo(tower.id());

    // Flipped, the plugin draws the other view and the hover follows it from its next refresh.
    key(Input.Keys.F);
    run(1);
    assertThat(plugin.drawn.scaleX()).as("side 0's view mirrors the width").isNegative();
    assertHoversThroughProjection(plugin);
  }

  @Test
  @DisplayName("a plugin whose projection fails is closed, and the hover is the rectangle's again")
  void failingProjection() throws Exception {
    install(FailsOnProjection.class);
    open();
    run(1);
    assertThat(Recording.last.closed).isTrue();
    assertThat(host().drawnArena()).isNull();
    assertFailureContained();
    key(Input.Keys.SPACE);
    run(1);
    assertHoversAcrossRectangle();
  }

  @Test
  @DisplayName("a projection a plugin cannot make is refused, and the plugin closed")
  void refusedProjection() throws Exception {
    install(FlatProjection.class);
    open();
    run(1);
    assertThat(Recording.last.closed).isTrue();
    assertThat(host().drawnArena()).isNull();
    assertFailureContained();
  }

  @Test
  @DisplayName("with no plugin, the host is asked no pick and a click picks as before")
  void pickWithoutPlugin() throws Exception {
    open();
    run(1);
    key(Input.Keys.SPACE);
    run(1);
    assertThat(host().entityAt(WIDTH / 2f, HEIGHT / 2f)).isEqualTo(PresentationPlugin.NO_ENTITY);

    // The far tower's ground point, mapped across the arena's whole rectangle.
    PresentContext context = workspace().presentContext();
    ViewOrientation view = ViewOrientation.of(tileMap(), context.bottomSide());
    int half = tileMapHeight() / 2;
    EntityView tower =
        BattleAdapter.frame(session()).entities().stream()
            .filter(entity -> entity.kind() == EntityView.Kind.TOWER)
            .filter(entity -> entity.y() > half)
            .findFirst()
            .orElseThrow();
    float x = view.px(tower.x());
    float y = view.py(tower.y()) - RenderConstants.BOTTOM_UI_HEIGHT;
    click(
        context.viewportX() + x / WorkspaceViewport.WORLD_WIDTH * context.viewportWidth(),
        context.viewportY() + y / WorkspaceViewport.WORLD_HEIGHT * context.viewportHeight());
    assertThat(selected()).isEqualTo(tower.id());
    assertThat(hoverDrawn()).isEqualTo(PresentationPlugin.NO_ENTITY);
  }

  @Test
  @DisplayName("a plugin's pick answers a click and a hover; when it names none, the host picks")
  void pluginPicks() throws Exception {
    install(Picking.class);
    open();
    run(1);
    key(Input.Keys.SPACE);
    run(1);
    Picking plugin = (Picking) Recording.last;
    PresentContext context = plugin.presents.get(plugin.presents.size() - 1);
    EntityFrame far = farTower(plugin);
    int half = tileMapHeight() / 2;
    EntityFrame near =
        plugin.frames.get(plugin.frames.size() - 1).entities().stream()
            .filter(entity -> entity.kind() == EntityKind.TOWER)
            .filter(entity -> entity.y() < half)
            .findFirst()
            .orElseThrow();

    // The plugin says it drew the near tower where the host has the far one's ground point: its
    // answer wins, and it is asked in its rectangle's pixels.
    plugin.atX = plugin.drawn.pixelX(far.x());
    plugin.atY = plugin.drawn.pixelY(far.y());
    plugin.answer = near.id();
    click(context.viewportX() + plugin.atX, context.viewportY() + plugin.atY);
    assertThat(selected()).isEqualTo(near.id());
    float[] asked = plugin.asked.get(plugin.asked.size() - 1);
    assertThat(asked[0]).isCloseTo(plugin.atX, within(0.5f));
    assertThat(asked[1]).isCloseTo(plugin.atY, within(0.5f));
    assertThat(hoverDrawn()).as("the click's hover").isEqualTo(near.id());

    // Naming none, the host picks the tower whose ground point is under the mouse.
    plugin.answer = PresentationPlugin.NO_ENTITY;
    click(context.viewportX() + plugin.atX, context.viewportY() + plugin.atY);
    assertThat(selected()).isEqualTo(far.id());
    assertThat(hoverDrawn()).isEqualTo(PresentationPlugin.NO_ENTITY);
  }

  @Test
  @DisplayName("a plugin whose pick fails is closed, and the host picks")
  void failingPick() throws Exception {
    install(FailsOnPick.class);
    open();
    run(1);
    key(Input.Keys.SPACE);
    run(1);
    Projecting plugin = (Projecting) Recording.last;
    PresentContext context = plugin.presents.get(plugin.presents.size() - 1);
    EntityFrame far = farTower(plugin);
    click(
        context.viewportX() + plugin.drawn.pixelX(far.x()),
        context.viewportY() + plugin.drawn.pixelY(far.y()));
    assertThat(plugin.closed).isTrue();
    assertThat(selected()).isEqualTo(far.id());
    assertThat(host().entityAt(WIDTH / 2f, HEIGHT / 2f)).isEqualTo(PresentationPlugin.NO_ENTITY);
    key(Input.Keys.SPACE);
    assertFailureContained();
  }

  /**
   * A point three quarters into a tile, in game units: in its second routing cell, clear of any
   * edge that a rounding could put on either side.
   */
  private static float inside(int tile) {
    return tile * GameUnits.UNITS_PER_TILE + 0.75f * GameUnits.UNITS_PER_TILE;
  }

  /** Each of {@link #TILES} hovered where the arena's whole rectangle draws a point in it. */
  private void assertHoversAcrossRectangle() throws Exception {
    PresentContext context = workspace().presentContext();
    ViewOrientation view = ViewOrientation.of(tileMap(), context.bottomSide());
    for (int[] tile : TILES) {
      float x = view.px(inside(tile[0]));
      float y = view.py(inside(tile[1])) - RenderConstants.BOTTOM_UI_HEIGHT;
      hover(
          context.viewportX() + x / WorkspaceViewport.WORLD_WIDTH * context.viewportWidth(),
          context.viewportY() + y / WorkspaceViewport.WORLD_HEIGHT * context.viewportHeight());
      assertHovered(tile);
    }
  }

  /** Each of {@link #TILES} hovered where the plugin's projection drew a point in it. */
  private void assertHoversThroughProjection(Projecting plugin) throws Exception {
    PresentContext context = plugin.presents.get(plugin.presents.size() - 1);
    for (int[] tile : TILES) {
      hover(
          context.viewportX() + plugin.drawn.pixelX(inside(tile[0])),
          context.viewportY() + plugin.drawn.pixelY(inside(tile[1])));
      assertHovered(tile);
    }
  }

  private void assertHovered(int[] tile) throws Exception {
    String name = "tile " + tile[0] + ", " + tile[1];
    assertThat(screenInt("hoverTileX")).as(name).isEqualTo(tile[0]);
    assertThat(screenInt("hoverTileY")).as(name).isEqualTo(tile[1]);
    // Three quarters into a tile is in the second of its two routing cells on each axis.
    assertThat(screenInt("hoverCellX")).as(name).isEqualTo(2 * tile[0] + 1);
    assertThat(screenInt("hoverCellY")).as(name).isEqualTo(2 * tile[1] + 1);
  }

  /** The mouse moves to a window point, its row counted from the window's bottom. */
  private static void hover(float x, float y) {
    int screenX = Math.round(x);
    int screenY = HEIGHT - Math.round(y);
    Gdx.input.setCursorPosition(screenX, screenY);
    Gdx.input.getInputProcessor().mouseMoved(screenX, screenY);
  }

  /** A left click at a window point, its row counted from the window's bottom. */
  private static void click(float x, float y) {
    hover(x, y);
    Gdx.input
        .getInputProcessor()
        .touchDown(Math.round(x), HEIGHT - Math.round(y), 0, Input.Buttons.LEFT);
  }

  private void assertFailureContained() throws Exception {
    Recording plugin = Recording.last;
    int heard = plugin.frames.size() + plugin.presents.size();
    int tick = session().tick();
    run(20);
    assertThat(session().tick()).isGreaterThan(tick);
    assertThat(session().getHalted()).isNull();
    assertThat(plugin.frames.size() + plugin.presents.size()).as("no more calls").isEqualTo(heard);
    assertThat(stepListener(session())).isNull();
  }

  private static void assertContiguous(List<Integer> ticks) {
    for (int i = 1; i < ticks.size(); i++) {
      assertThat(ticks.get(i)).as("tick %d of the stream", i).isEqualTo(ticks.get(i - 1) + 1);
    }
  }

  private void install(Class<? extends PresentationPlugin> plugin) throws IOException {
    installNamed(plugin.getName());
  }

  /** Puts a plugin's service file on the thread's class loader, as a plugin's jar would. */
  private void installNamed(String plugin) throws IOException {
    Path file = services.resolve("META-INF/services/" + PresentationPlugin.class.getName());
    Files.createDirectories(file.getParent());
    Files.writeString(file, plugin + "\n");
    Thread.currentThread()
        .setContextClassLoader(new URLClassLoader(new URL[] {services.toUri().toURL()}, original));
  }

  private void open() throws IOException {
    Path folder = Path.of(System.getProperty("crforge.gameTables"));
    DataVersions versions =
        new DataVersions(
            folder.getParent(),
            List.of(tables.version()),
            folder,
            tables,
            "test",
            tables.version());
    game = new CRForgeGame(versions, null, ReplayFile.read(example, tables));
    game.create();
    game.resize(WIDTH, HEIGHT);
    gdx.endFrame();
  }

  /** Runs frames; the last one's calls. */
  private HeadlessGdx.Frame run(int frames) {
    HeadlessGdx.Frame last = null;
    for (int i = 0; i < frames; i++) {
      game.render();
      last = gdx.endFrame();
    }
    return last;
  }

  private static void key(int key) {
    Gdx.input.getInputProcessor().keyDown(key);
  }

  private BattleSession session() throws Exception {
    Field field = ReplayGameScreen.class.getDeclaredField("player");
    field.setAccessible(true);
    return ((ReplayPlayer) field.get(game.getScreen())).getSession();
  }

  private TileMap tileMap() throws Exception {
    return session().getBattle().getWorld().getTileMap();
  }

  private Object screenField(String name) throws Exception {
    Field field = ReplayGameScreen.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(game.getScreen());
  }

  private int screenInt(String name) throws Exception {
    return (Integer) screenField(name);
  }

  private PresentationHost host() throws Exception {
    return (PresentationHost) screenField("presentation");
  }

  private BattleWorkspace workspace() throws Exception {
    return (BattleWorkspace) screenField("workspace");
  }

  /** The entity the workspace inspects. */
  private int selected() throws Exception {
    Field field = BattleWorkspace.class.getDeclaredField("selectedId");
    field.setAccessible(true);
    return (Integer) field.get(workspace());
  }

  /** A tower on side 1's half, from the plugin's last tick. */
  private EntityFrame farTower(Recording plugin) throws Exception {
    int half = tileMapHeight() / 2;
    return plugin.frames.get(plugin.frames.size() - 1).entities().stream()
        .filter(entity -> entity.kind() == EntityKind.TOWER)
        .filter(entity -> entity.y() > half)
        .findFirst()
        .orElseThrow();
  }

  /** The unit a plugin named under the mouse at the last hover, as the renderer holds it. */
  private int hoverDrawn() throws Exception {
    Field field = BattleRenderer.class.getDeclaredField("hoverDrawn");
    field.setAccessible(true);
    return (Integer) field.get(screenField("renderer"));
  }

  private int tileMapWidth() throws Exception {
    return session().getBattle().getWorld().getTileMap().widthUnits();
  }

  private int tileMapHeight() throws Exception {
    return session().getBattle().getWorld().getTileMap().heightUnits();
  }

  private static Object stepListener(BattleSession session) throws Exception {
    Field field = BattleSession.class.getDeclaredField("stepListener");
    field.setAccessible(true);
    return field.get(session);
  }

  /** Remembers everything it is told. */
  public static class Recording implements PresentationPlugin {

    static Recording last;

    final List<BattleStart> starts = new ArrayList<>();

    final List<TickFrame> frames = new ArrayList<>();

    final List<PresentContext> presents = new ArrayList<>();

    boolean closed;

    public Recording() {
      last = this;
    }

    List<Integer> ticks() {
      return frames.stream().map(TickFrame::tick).toList();
    }

    @Override
    public String id() {
      return "recording";
    }

    @Override
    public void battleStarted(BattleStart battle) {
      starts.add(battle);
    }

    @Override
    public void tick(TickFrame frame) {
      frames.add(frame);
    }

    @Override
    public void present(PresentContext context) {
      presents.add(context);
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  /** Fails on the first tick it hears. */
  public static class FailsOnTick extends Recording {
    @Override
    public void tick(TickFrame frame) {
      throw new IllegalStateException("a plugin's bug");
    }
  }

  /** Fails on the first refresh. */
  public static class FailsOnPresent extends Recording {
    @Override
    public void present(PresentContext context) {
      throw new IllegalStateException("a plugin's bug");
    }
  }

  /**
   * Draws the arena smaller than its rectangle and off its centre, as a game camera does: 36,000
   * game units along the arena's length fill the rectangle's height, and the width is drawn at
   * 0.839 / 1.036 of that scale, each axis reversed as the view of its side turns it.
   */
  public static class Projecting extends Recording {

    ArenaProjection drawn;

    private int width;

    private int length;

    @Override
    public void battleStarted(BattleStart battle) {
      super.battleStarted(battle);
      width = battle.arenaWidthUnits();
      length = battle.arenaHeightUnits();
    }

    @Override
    public void present(PresentContext context) {
      super.present(context);
      float along = context.viewportHeight() / 36_000f;
      float across = along * 0.839f / 1.036f;
      float scaleX = context.bottomSide() == 0 ? -across : across;
      float scaleY = context.bottomSide() == 1 ? -along : along;
      float centreX = context.viewportWidth() / 2f + 13;
      float centreY = context.viewportHeight() / 2f - 7;
      drawn =
          new ArenaProjection(
              centreX - scaleX * width / 2f, centreY - scaleY * length / 2f, scaleX, scaleY);
    }

    @Override
    public ArenaProjection arenaProjection() {
      return drawn;
    }
  }

  /** Draws as {@link Projecting} does and names {@link #answer} drawn at one pixel. */
  public static class Picking extends Projecting {

    final List<float[]> asked = new ArrayList<>();

    float atX = Float.NaN;

    float atY = Float.NaN;

    int answer = NO_ENTITY;

    @Override
    public int entityAt(float pixelX, float pixelY) {
      asked.add(new float[] {pixelX, pixelY});
      return Math.abs(pixelX - atX) <= 1 && Math.abs(pixelY - atY) <= 1 ? answer : NO_ENTITY;
    }
  }

  /** Fails when asked what it drew under the mouse. */
  public static class FailsOnPick extends Projecting {
    @Override
    public int entityAt(float pixelX, float pixelY) {
      throw new IllegalStateException("a plugin's bug");
    }
  }

  /** Fails when asked where it drew. */
  public static class FailsOnProjection extends Recording {
    @Override
    public ArenaProjection arenaProjection() {
      throw new IllegalStateException("a plugin's bug");
    }
  }

  /** Reports an arena drawn flat, which no point can be mapped back through. */
  public static class FlatProjection extends Recording {
    @Override
    public ArenaProjection arenaProjection() {
      return new ArenaProjection(0, 0, 0, 1);
    }
  }
}
