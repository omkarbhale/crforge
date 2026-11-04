/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.crforge.core.battle.data.GameTables;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.battle.DataVersions;
import org.crforge.desktop.battle.EntityView;
import org.crforge.desktop.render.ViewOrientation;
import org.crforge.desktop.replay.ReplayFile;
import org.crforge.desktop.replay.ReplayPlayer;

/**
 * What the visualizer draws, frame by frame, as the controls are used: the real application ({@link
 * CRForgeGame}, its screens, workspace and renderer) on {@link HeadlessGdx}, driven by a fixed
 * script of keys, buttons and mouse moves through the input the window gives it, with a fixed delta
 * time. A line of the trace holds the frame's tick and the digest of the OpenGL calls that drew it.
 *
 * <p>Two runs of a build give the same trace, so {@link VisualizerGoldenTest} holds the visualizer
 * to the trace committed under {@code src/test/resources/golden}. {@code ./gradlew
 * :desktop:visualizerGolden} writes it anew, for a change that means to alter what is drawn or a
 * data version bump.
 */
final class VisualizerTrace {

  /** The folder of the committed traces, relative to the resources root. */
  static final String GOLDEN = "golden/";

  /** The window the launcher opens. */
  private static final int WIDTH = 1120;

  private static final int HEIGHT = 1040;

  /** The foreground rate the launcher asks for. */
  private static final float DELTA = 1 / 60f;

  /** One frame of a fast forward: the most time a frame's stage acts on, at full speed. */
  private static final float FAST_DELTA = 0.1f;

  private final List<String> lines = new ArrayList<>();

  private final HeadlessGdx gdx = new HeadlessGdx(WIDTH, HEIGHT, DELTA);

  private CRForgeGame game;

  private int frames;

  private String pending = "";

  /** The file a scenario's trace is committed in, on the tables of a data version. */
  static String resource(String scenario, String version) {
    return GOLDEN + "visualizer-" + scenario + "-" + version + ".trace";
  }

  /**
   * The Ladder screen: pause and step, the toolbar's buttons, a card of each side played by mouse,
   * every overlay on and off with the mouse on the arena, inspection, annotations, the refused
   * flip, the data version key, speed, the model and ability keys without a model, a reset, then a
   * replay dropped on the window.
   */
  static List<String> debug(GameTables tables, Path tablesFolder, Path replay) {
    VisualizerTrace trace = new VisualizerTrace();
    DataVersions versions = versions(tables, tablesFolder);
    BattleSession first = versions.ladder();
    trace.open(new CRForgeGame(versions, first));
    trace.frames(3);
    trace.key(Input.Keys.SPACE, "SPACE");
    trace.frames(2);
    trace.key(Input.Keys.PERIOD, ".");
    trace.frames(1);
    trace.button("Step [.]");
    trace.frames(1);
    trace.button("Tile grid");
    trace.frames(2);
    trace.button("Tile grid");
    trace.frames(1);
    trace.button("Data details");
    trace.frames(2);
    trace.button("Data details");
    trace.frames(1);
    // Past the battle's start cooldown, so the cards can be played.
    for (int i = 0; i < 400 && first.cardUnavailableReason(0, 0) != null; i++) {
      trace.key(Input.Keys.PERIOD, ".");
    }
    trace.frames(2);
    trace.key(Input.Keys.NUM_1, "1");
    trace.hover(ViewOrientation.SIDE_0, 6500, 8500);
    trace.frames(2);
    trace.key(Input.Keys.M, "M");
    trace.frames(1);
    trace.key(Input.Keys.M, "M");
    trace.frames(1);
    trace.arena(ViewOrientation.SIDE_0, 6500, 8500);
    trace.frames(2);
    trace.key(Input.Keys.NUM_5, "5");
    trace.hover(ViewOrientation.SIDE_0, 11500, 23500);
    trace.frames(2);
    trace.arena(ViewOrientation.SIDE_0, 11500, 23500);
    trace.frames(2);
    trace.key(Input.Keys.SPACE, "SPACE");
    trace.key(Input.Keys.PLUS, "+");
    trace.frames(120);
    trace.overlays(ViewOrientation.SIDE_0, new int[] {6500, 12500});
    trace.key(Input.Keys.I, "I");
    trace.inspectTroop(first, ViewOrientation.SIDE_0);
    trace.frames(3);
    trace.key(Input.Keys.T, "T");
    trace.frames(2);
    trace.key(Input.Keys.T, "T");
    trace.key(Input.Keys.F, "F");
    trace.frames(1);
    trace.key(Input.Keys.K, "K");
    trace.key(Input.Keys.J, "J");
    trace.frames(1);
    trace.key(Input.Keys.K, "K");
    trace.key(Input.Keys.J, "J");
    trace.key(Input.Keys.Q, "Q");
    trace.key(Input.Keys.W, "W");
    trace.frames(1);
    trace.key(Input.Keys.MINUS, "-");
    trace.key(Input.Keys.V, "V");
    trace.framesShowingFolder(2);
    trace.key(Input.Keys.R, "R");
    trace.frames(5);
    trace.note("dropped replay");
    trace.game.filesDropped(new String[] {replay.toString()});
    trace.frames(3);
    return trace.close();
  }

  /**
   * The replay viewer on the example replay: speeds, every overlay, the mouse and inspection, the
   * flip and annotations, pause, a step each way, a restart, and the whole battle to its end.
   */
  static List<String> replay(GameTables tables, Path tablesFolder, Path file) throws IOException {
    VisualizerTrace trace = new VisualizerTrace();
    DataVersions versions = versions(tables, tablesFolder);
    ReplayFile replay = ReplayFile.read(file, tables);
    if (!replay.playable()) {
      throw new IllegalStateException("the example replay is refused: " + replay.refusals());
    }
    trace.open(new CRForgeGame(versions, null, replay));
    trace.frames(30);
    for (int i = 0; i < 3; i++) {
      trace.key(Input.Keys.PLUS, "+");
    }
    trace.frames(60);
    trace.overlays(ViewOrientation.SIDE_1, new int[] {9500, 12500});
    trace.key(Input.Keys.F, "F");
    trace.frames(4);
    trace.key(Input.Keys.T, "T");
    trace.frames(2);
    trace.key(Input.Keys.T, "T");
    trace.key(Input.Keys.F, "F");
    trace.frames(2);
    trace.key(Input.Keys.SPACE, "SPACE");
    trace.frames(2);
    trace.key(Input.Keys.PERIOD, ".");
    trace.frames(2);
    trace.key(Input.Keys.COMMA, ",");
    trace.frames(2);
    trace.key(Input.Keys.SPACE, "SPACE");
    trace.key(Input.Keys.MINUS, "-");
    trace.frames(20);
    trace.key(Input.Keys.R, "R");
    trace.frames(10);
    trace.key(Input.Keys.A, "A");
    trace.key(Input.Keys.D, "D");
    trace.key(Input.Keys.PLUS, "+");
    trace.gdx.delta(FAST_DELTA);
    // The rest of the battle, a bounded loop in case it never finishes.
    for (int i = 0; i < 10_000 && !trace.playerOf().finished(); i++) {
      trace.frames(1);
    }
    if (!trace.playerOf().finished()) {
      throw new IllegalStateException("the example replay did not finish");
    }
    trace.gdx.delta(DELTA);
    trace.frames(5);
    return trace.close();
  }

  /** Writes the committed traces anew: {@code visualizerGolden <resources folder>}. */
  public static void main(String[] args) throws IOException {
    GameTables tables = GameTables.loadConfigured();
    Path folder = Path.of(System.getProperty("crforge.gameTables"));
    Path example = Path.of(System.getProperty("crforge.exampleReplay"));
    Path out = Path.of(args[0]);
    write(out, "debug", tables, debug(tables, folder, example));
    write(out, "replay", tables, replay(tables, folder, example));
  }

  private static void write(Path out, String scenario, GameTables tables, List<String> trace)
      throws IOException {
    Path file = out.resolve(resource(scenario, tables.version()));
    Files.createDirectories(file.getParent());
    Files.write(file, trace, StandardCharsets.UTF_8);
    System.out.println("Wrote " + trace.size() + " frames to " + file);
  }

  private static DataVersions versions(GameTables tables, Path folder) {
    // One version, and no path or provenance of this machine: the trace holds what is drawn.
    return new DataVersions(
        folder.getParent(),
        List.of(tables.version()),
        folder,
        tables,
        "visualizer trace",
        tables.version());
  }

  private void open(CRForgeGame next) {
    game = next;
    game.create();
    game.resize(WIDTH, HEIGHT);
    record("setup");
  }

  private void frames(int count) {
    for (int i = 0; i < count; i++) {
      game.render();
      frames++;
      record(String.valueOf(frames));
    }
  }

  /**
   * Frames that show this machine's tables folder, as the note of a data version switch does: they
   * run, and the trace records that they did, but what they draw is not the same on another
   * machine.
   */
  private void framesShowingFolder(int count) {
    for (int i = 0; i < count; i++) {
      game.render();
      frames++;
      gdx.endFrame();
      lines.add(frames + " tick=" + tick() + " gl=machine-path" + after());
      pending = "";
    }
  }

  private void record(String label) {
    HeadlessGdx.Frame frame = gdx.endFrame();
    lines.add(
        label
            + " tick="
            + tick()
            + " calls="
            + frame.calls()
            + " draws="
            + frame.draws()
            + " gl="
            + frame.digest()
            + after());
    pending = "";
  }

  /** The inputs given since the last frame, as the end of a line. */
  private String after() {
    return pending.isEmpty() ? "" : " after=" + pending.trim();
  }

  private void note(String input) {
    pending += " " + input.replace(' ', '_');
  }

  private void key(int key, String name) {
    note(name);
    Gdx.input.getInputProcessor().keyDown(key);
  }

  private void button(String text) {
    note(text);
    WorkspaceSmoke.click(text, true);
  }

  private void hover(ViewOrientation view, int x, int y) {
    note("hover");
    WorkspaceSmoke.hoverUnit(view, x, y);
  }

  private void arena(ViewOrientation view, int x, int y) {
    note("click");
    WorkspaceSmoke.clickUnit(view, x, y);
  }

  /** Each overlay's key, on then off, with the mouse on a point of the arena. */
  private void overlays(ViewOrientation view, int[] point) {
    hover(view, point[0], point[1]);
    frames(1);
    int[] keys = {
      Input.Keys.P,
      Input.Keys.O,
      Input.Keys.D,
      Input.Keys.A,
      Input.Keys.H,
      Input.Keys.G,
      Input.Keys.N
    };
    for (int key : keys) {
      key(key, Input.Keys.toString(key));
      frames(3);
      key(key, Input.Keys.toString(key));
      frames(1);
    }
  }

  private void inspectTroop(BattleSession session, ViewOrientation view) {
    EntityView troop =
        BattleAdapter.frame(session).entities().stream()
            .filter(entity -> entity.kind() == EntityView.Kind.TROOP)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("no troop came of the plays"));
    arena(view, troop.x(), troop.y());
  }

  private List<String> close() {
    game.dispose();
    record("dispose");
    return List.copyOf(lines);
  }

  private ReplayPlayer playerOf() {
    return player(game.getScreen());
  }

  /** The tick on screen, read as the screens hold their battle, -1 for a refused replay. */
  private int tick() {
    try {
      Screen screen = game.getScreen();
      Field session = fieldOf(screen, "session");
      if (session != null) {
        return ((BattleSession) session.get(screen)).tick();
      }
      BattleSession playing = player(screen).getSession();
      return playing == null ? -1 : playing.tick();
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private static ReplayPlayer player(Screen screen) {
    try {
      return (ReplayPlayer) fieldOf(screen, "player").get(screen);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Field fieldOf(Screen screen, String name) {
    try {
      Field field = screen.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      return null;
    }
  }
}
