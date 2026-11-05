/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.crforge.core.battle.GameData;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.battle.unit.CharacterEntity;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.crforge.core.battle.unit.WorldEntity;
import org.crforge.core.battle.unit.WorldObserver;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.replay.ReplayFile;
import org.crforge.desktop.replay.ReplayPlayer;
import org.crforge.presentation.BattleStart;
import org.crforge.presentation.DeathCause;
import org.crforge.presentation.DeathFrame;
import org.crforge.presentation.EntityFrame;
import org.crforge.presentation.EntityState;
import org.crforge.presentation.PresentContext;
import org.crforge.presentation.PresentationPlugin;
import org.crforge.presentation.StatusFrame;
import org.crforge.presentation.StrikeFrame;
import org.crforge.presentation.TickFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The deaths, strikes, statuses and clones a plugin's frames tell, from battles on the tables. */
class TickEventLogTest {

  private static final int LEVEL = 11;

  private static GameTables tables;

  @BeforeAll
  static void read() {
    tables = GameTables.loadConfigured();
  }

  /** A battle with the log on it, read as the host reads it: a frame after each step. */
  private static final class Played implements WorldObserver {

    final Standard1v1Battle battle;

    final BattleSession session;

    final TickEventLog log = new TickEventLog();

    final List<TickFrame> frames = new ArrayList<>();

    /** Where each entity that left stood as it left, read from the live object. */
    final Map<Integer, int[]> leftAt = new HashMap<>();

    /** The row of each entity that left. */
    final Map<Integer, String> leftRows = new HashMap<>();

    Played() {
      this(tables);
    }

    Played(GameTables on) {
      battle = new Standard1v1Battle(on, LEVEL, false);
      session = BattleSession.of(battle);
      battle.getWorld().addObserver(log);
      battle.getWorld().addObserver(this);
      session.setStepListener(
          () ->
              frames.add(
                  TickFrames.of(
                      BattleAdapter.frame(session), log.drainDeaths(), log.drainStrikes())));
    }

    @Override
    public void entityRemoved(int tick, WorldEntity removed) {
      leftAt.put(removed.getId(), new int[] {removed.getView().getX(), removed.getView().getY()});
      leftRows.put(removed.getId(), removed.getData().name());
    }

    Played play(int tick, String card, int side, int x, int y) {
      return play(tick, card, LEVEL, side, x, y);
    }

    Played play(int tick, String card, int level, int side, int x, int y) {
      battle.play(
          tick,
          battle.getWorld().getRecords().card(card),
          level,
          side,
          x,
          y,
          card + "@" + tick + "/" + side);
      return this;
    }

    Played run(int ticks) {
      for (int i = 0; i < ticks && session.step(); i++) {
        assertThat(session.getHalted()).isNull();
      }
      assertThat(session.getHalted()).as("the battle played on").isNull();
      return this;
    }

    List<DeathFrame> deaths() {
      return frames.stream().flatMap(frame -> frame.deaths().stream()).toList();
    }

    List<StrikeFrame> strikes() {
      return frames.stream().flatMap(frame -> frame.strikes().stream()).toList();
    }

    List<DeathFrame> deathsOf(String row) {
      return deaths().stream().filter(death -> death.name().equals(row)).toList();
    }

    /** The id an entity of a row had, in the first frame that holds one of its side. */
    int idOf(String row, int side) {
      return frames.stream()
          .flatMap(frame -> frame.entities().stream())
          .filter(entity -> entity.name().equals(row) && entity.side() == side)
          .findFirst()
          .orElseThrow()
          .id();
    }
  }

  @Test
  @DisplayName("a duel tells each hit with its attacker, and the loser's death where it fell")
  void duel() {
    Played played =
        new Played().play(1, "Knight", 0, 4500, 13500).play(1, "Knight", 1, 4500, 18500).run(400);

    int blue = played.idOf("Knight", 0);
    int red = played.idOf("Knight", 1);
    List<StrikeFrame> knights =
        played.strikes().stream()
            .filter(s -> (s.attackerId() == blue || s.attackerId() == red))
            .toList();
    assertThat(knights).isNotEmpty();
    assertThat(knights).noneMatch(StrikeFrame::special);
    assertThat(knights)
        .contains(new StrikeFrame(blue, red, false), new StrikeFrame(red, blue, false));

    // The winner falls to a tower later: each death told once, where its own entity stood.
    List<DeathFrame> deaths = played.deathsOf("Knight");
    assertThat(deaths).hasSize(2);
    for (DeathFrame death : deaths) {
      assertThat(death.cause()).isEqualTo(DeathCause.KILLED);
      assertThat(death.side()).isEqualTo(death.id() == blue ? 0 : 1);
      assertThat(new int[] {death.x(), death.y()}).isEqualTo(played.leftAt.get(death.id()));
      assertThat(played.frames.get(tellingFrame(played, death) - 1).entities())
          .as("it stood the tick before")
          .anyMatch(entity -> entity.id() == death.id());
    }
  }

  private static int tellingFrame(Played played, DeathFrame death) {
    for (int i = 0; i < played.frames.size(); i++) {
      if (played.frames.get(i).deaths().contains(death)) {
        return i;
      }
    }
    throw new AssertionError("not told: " + death);
  }

  @Test
  @DisplayName("a charge's first hit and a dash's landing are special, the hits after them are not")
  void specialHits() {
    Played prince =
        new Played().play(1, "Prince", 0, 4500, 9000).play(100, "Golem", 1, 4500, 15000).run(400);
    int princeId = prince.idOf("Prince", 0);
    List<StrikeFrame> princeHits =
        prince.strikes().stream().filter(s -> s.attackerId() == princeId).toList();
    assertThat(princeHits).as("the Prince's").hasSizeGreaterThan(1);
    assertThat(princeHits.get(0).special()).as("its charge's hit").isTrue();
    assertThat(princeHits.get(1).special()).as("the next, standing").isFalse();

    Played bandit =
        new Played().play(1, "Assassin", 0, 4500, 11000).play(1, "Golem", 1, 4500, 15500).run(200);
    int banditId = bandit.idOf("Assassin", 0);
    assertThat(bandit.strikes())
        .as("the Bandit's dash")
        .anyMatch(s -> s.attackerId() == banditId && s.special());
  }

  @Test
  @DisplayName("a unit whose hit ends it dies by its own hit")
  void kamikaze() {
    Played played =
        new Played()
            .play(1, "FireSpirits", 0, 4500, 13500)
            .play(1, "Knight", 1, 4500, 18500)
            .run(100);
    assertThat(played.deathsOf("FireSpirits"))
        .isNotEmpty()
        .allMatch(death -> death.cause() == DeathCause.KAMIKAZE);
  }

  @Test
  @DisplayName("a building's lifetime running out is a decay")
  void decay() {
    Played played = new Played().play(1, "Cannon", 0, 4500, 9000).run(800);
    assertThat(played.deathsOf("Cannon"))
        .singleElement()
        .extracting(DeathFrame::cause)
        .isEqualTo(DeathCause.DECAY);
  }

  @Test
  @DisplayName("a bomb left behind ends by its own course")
  void expired() {
    Played played =
        new Played()
            .play(1, "Balloon", 0, 3500, 25000)
            .play(1, "MegaMinion", 1, 3500, 27000)
            .play(1, "Musketeer", 1, 2500, 27000)
            .run(900);
    assertThat(played.deathsOf("Balloon")).as("the Balloon fell").hasSize(1);
    assertThat(played.deathsOf("BalloonBomb"))
        .singleElement()
        .extracting(DeathFrame::cause)
        .isEqualTo(DeathCause.EXPIRED);
  }

  @Test
  @DisplayName("riders go with their parent")
  void riders() {
    Played played =
        new Played()
            .play(1, "GoblinGiant", 0, 4500, 13000)
            .play(1, "Pekka", 1, 4500, 18000)
            .run(600);
    assertThat(played.deathsOf("GoblinGiant"))
        .singleElement()
        .extracting(DeathFrame::cause)
        .isEqualTo(DeathCause.KILLED);
    assertThat(played.deaths())
        .as("its riders")
        .anyMatch(death -> death.cause() == DeathCause.PARENT_LEFT);
  }

  @Test
  @DisplayName("an entity dies once, under the first cause told")
  void diesOnce() {
    Played played = new Played().play(1, "Knight", 0, 4500, 9000).run(5);
    CharacterEntity knight =
        played.battle.getBattle().getHolder().entities().stream()
            .filter(CharacterEntity.class::isInstance)
            .map(CharacterEntity.class::cast)
            .filter(entity -> entity.getData().name().equals("Knight"))
            .findFirst()
            .orElseThrow();

    played.log.decayDied(5, knight, 1);
    played.log.entityDied(5, knight, null);

    assertThat(played.log.drainDeaths())
        .singleElement()
        .extracting(DeathFrame::cause)
        .isEqualTo(DeathCause.DECAY);
    assertThat(played.log.drainDeaths()).as("drained").isEmpty();
  }

  /** The egg's row is written as {@code BattlePhoenixTest} writes it, to hatch once and leave. */
  @Test
  @DisplayName("a limit exit is no death: the egg that hatched leaves without one")
  void limitExitIsNoDeath(@TempDir Path folder) throws IOException {
    GameTables hatching =
        GameData.altered(
            folder,
            "characters",
            rows ->
                GameData.columns(rows, "PhoenixEgg")
                    .put("SpawnCharacter", "PhoenixNoRespawn")
                    .put("SpawnNumber", 1)
                    .put("SpawnLimit", 1)
                    .put("DeployTime", 1000)
                    .put("SpawnStartTime", 3800)
                    .put("SpawnPauseTime", 4300)
                    .put("DestroyAtLimit", true));
    Played played = new Played(hatching);
    played.battle.deploy(
        0, played.battle.getWorld().getRecords().unit("PhoenixEgg"), LEVEL, 0, 3500, 20000);
    played.run(200);

    assertThat(played.leftRows).as("the egg left").containsValue("PhoenixEgg");
    assertThat(played.deathsOf("PhoenixEgg")).isEmpty();
  }

  @Test
  @DisplayName("a morph is no death: the unit that surfaced leaves without one")
  void morphIsNoDeath() {
    Played played = new Played().play(1, "GoblinDrill", 0, 9000, 22000).run(200);
    assertThat(played.leftRows).as("the digger left").containsValue("GoblinDrillDig");
    assertThat(played.deathsOf("GoblinDrillDig")).isEmpty();
  }

  @Test
  @DisplayName("a buff stands on a frame under its row, with its timers and the caster's side")
  void statuses() {
    Played played =
        new Played().play(1, "Knight", 0, 4500, 9000).play(40, "Freeze", 1, 4500, 9000).run(60);
    int knight = played.idOf("Knight", 0);
    EntityFrame frozen =
        played.frames.get(played.frames.size() - 1).entities().stream()
            .filter(entity -> entity.id() == knight)
            .findFirst()
            .orElseThrow();
    assertThat(frozen.statuses()).isNotEmpty();
    StatusFrame freeze =
        frozen.statuses().stream()
            .filter(status -> status.name().equals("Freeze"))
            .findFirst()
            .orElseThrow();
    assertThat(freeze.side()).as("the caster's").isOne();
    assertThat(freeze.remainingMs()).isPositive().isLessThanOrEqualTo(freeze.totalMs());
    assertThat(played.frames.get(5).entities())
        .filteredOn(entity -> entity.id() == knight)
        .allMatch(entity -> entity.statuses().isEmpty());
  }

  /**
   * A plugin can begin on a battle at any tick, as it does on a step back, which rebuilds the
   * session: its first frame alone has to say which unit is the clone, with no setup seen.
   */
  @Test
  @DisplayName("a clone is flagged on a plugin's first frame, past its setup, and is no buff row")
  void cloneOnFirstFrame(@TempDir Path services) throws IOException {
    Played played =
        new Played().play(1, "Knight", 0, 4500, 9000).play(40, "Clone", 0, 4500, 9000).run(120);
    assertThat(played.frames.stream().flatMap(frame -> frame.entities().stream()))
        .as("the clone's setup, before the plugin begins")
        .anyMatch(entity -> entity.isClone() && entity.state() == EntityState.CLONE_SETUP);

    try (URLClassLoader loader = recordingPlugin(services)) {
      PresentationHost host = PresentationHost.discover(null, loader);
      host.follow(played.session);
      played.run(1);
      host.close();
    }

    assertThat(Recording.last.frames).hasSize(2);
    for (TickFrame frame : Recording.last.frames) {
      assertThat(frame.entities()).noneMatch(entity -> entity.state() == EntityState.CLONE_SETUP);
      List<EntityFrame> knights =
          frame.entities().stream().filter(entity -> entity.name().equals("Knight")).toList();
      assertThat(knights).as("the Knight and its clone").hasSize(2);
      assertThat(knights).filteredOn(EntityFrame::isClone).as("the clone alone").hasSize(1);
      assertThat(frame.entities()).filteredOn(EntityFrame::isClone).hasSize(1);
      assertThat(knights).allMatch(knight -> knight.statuses().isEmpty());
    }
    assertThat(
            played.frames.stream()
                .flatMap(frame -> frame.entities().stream())
                .flatMap(entity -> entity.statuses().stream())
                .map(StatusFrame::name))
        .as("only buff rows, its setup's among them")
        .isNotEmpty()
        .allMatch(tables.table("character_buffs")::has);
  }

  @Test
  @DisplayName("a stopped log collects nothing more")
  void stopped() {
    Played played = new Played().play(1, "Knight", 0, 4500, 13500);
    played.play(1, "Knight", 1, 4500, 18500);
    played.log.stop();
    played.run(400);
    assertThat(played.deaths()).isEmpty();
    assertThat(played.strikes()).isEmpty();
  }

  /**
   * The proof that a plugin leaves the battle alone: the real host, its frames and its event log
   * follow the example replay to its end, and every value of every tick stays that of the same
   * replay played with no plugin at all. Hit points, positions, targets, buffs, elixir, hands and
   * clocks are compared, so a change anywhere in the battle shows in them.
   */
  @Test
  @DisplayName("the battle with a plugin stays the battle without one at every tick")
  void pluginChangesNothing(@TempDir Path services) throws IOException {
    ReplayFile replay =
        ReplayFile.read(Path.of(System.getProperty("crforge.exampleReplay")), tables);
    assumeTrue(
        replay.playable(), "the example replay plays only on the tables of its own data version");

    ReplayPlayer followed = new ReplayPlayer(replay, tables);
    try (URLClassLoader loader = recordingPlugin(services)) {
      PresentationHost host = PresentationHost.discover(null, loader);
      host.follow(followed.getSession());
      while (followed.step()) {
        // to the replay's end
      }
      host.close();
    }
    List<TickFrame> heard = Recording.last.frames;

    ReplayPlayer plain = new ReplayPlayer(replay, tables);
    assertThat(heard).hasSizeGreaterThan(1000);
    assertSameBattle(
        TickFrames.of(BattleAdapter.frame(plain.getSession()), List.of(), List.of()), heard.get(0));
    for (TickFrame frame : heard.subList(1, heard.size())) {
      assertThat(plain.step()).as("the plain battle plays on past %s", frame.tick()).isTrue();
      assertSameBattle(
          TickFrames.of(BattleAdapter.frame(plain.getSession()), List.of(), List.of()), frame);
    }
    assertThat(plain.step()).as("both end together").isFalse();

    assertThat(heard.stream().flatMap(frame -> frame.deaths().stream())).isNotEmpty();
    assertThat(heard.stream().flatMap(frame -> frame.strikes().stream())).isNotEmpty();
    assertThat(heard.stream().flatMap(frame -> frame.entities().stream()))
        .anyMatch(entity -> !entity.statuses().isEmpty());
  }

  /** Two frames of the same tick say the same of the battle, whatever events each carries. */
  private static void assertSameBattle(TickFrame alone, TickFrame followed) {
    assertThat(followed.tick()).isEqualTo(alone.tick());
    assertThat(followed.timeMs()).isEqualTo(alone.timeMs());
    assertThat(followed.ended()).isEqualTo(alone.ended());
    assertThat(followed.winner()).isEqualTo(alone.winner());
    assertThat(followed.entities()).as("tick %s", alone.tick()).isEqualTo(alone.entities());
    assertThat(followed.sides()).as("tick %s", alone.tick()).isEqualTo(alone.sides());
  }

  /** A class loader with {@link Recording}'s service file on it, as a plugin's jar would have. */
  private static URLClassLoader recordingPlugin(Path services) throws IOException {
    Path service = services.resolve("META-INF/services/" + PresentationPlugin.class.getName());
    Files.createDirectories(service.getParent());
    Files.writeString(service, Recording.class.getName() + "\n");
    return new URLClassLoader(
        new URL[] {services.toUri().toURL()}, TickEventLogTest.class.getClassLoader());
  }

  /** A plugin that keeps every frame, found by the host through its service file. */
  public static final class Recording implements PresentationPlugin {

    static Recording last;

    final List<TickFrame> frames = new ArrayList<>();

    public Recording() {
      last = this;
    }

    @Override
    public String id() {
      return "recording";
    }

    @Override
    public void battleStarted(BattleStart battle) {}

    @Override
    public void tick(TickFrame frame) {
      frames.add(frame);
    }

    @Override
    public void present(PresentContext context) {}

    @Override
    public void close() {}
  }
}
