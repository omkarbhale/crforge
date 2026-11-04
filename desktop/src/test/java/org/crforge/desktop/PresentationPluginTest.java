/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.battle.DataVersions;
import org.crforge.desktop.replay.ReplayFile;
import org.crforge.desktop.replay.ReplayPlayer;
import org.crforge.desktop.screen.ReplayGameScreen;
import org.crforge.presentation.BattleStart;
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
    assertThat(plugin.starts).containsExactly(new BattleStart(tileMapWidth(), tileMapHeight(), 0));
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
}
