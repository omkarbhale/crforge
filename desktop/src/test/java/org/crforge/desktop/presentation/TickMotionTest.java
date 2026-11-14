/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.crforge.core.battle.BattleEntity;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.battle.unit.CharacterEntity;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.crforge.core.battle.unit.WorldObserver;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleFrame;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.battle.EntityView;
import org.crforge.presentation.DashFrame;
import org.crforge.presentation.DashMoment;
import org.crforge.presentation.EntityFrame;
import org.crforge.presentation.EntityState;
import org.crforge.presentation.ForcedAnimationFrame;
import org.crforge.presentation.PresentationPlugin;
import org.crforge.presentation.TickFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The dash moments, charges and forced animations a plugin's frames tell, from battles. */
class TickMotionTest {

  private static final int LEVEL = 11;

  private static GameTables tables;

  @BeforeAll
  static void read() {
    tables = GameTables.loadConfigured();
  }

  /** What the battle itself told its observers, to hold the frames to. */
  private static final class Heard implements WorldObserver {

    final List<int[]> windups = new ArrayList<>();

    final List<int[]> forcedRuns = new ArrayList<>();

    @Override
    public void dashWindupStarted(int tick, CharacterEntity unit, int windupMs) {
      windups.add(new int[] {tick, unit.getId(), windupMs});
    }

    @Override
    public void forcedAnimationRun(
        int tick,
        BattleEntity entity,
        String action,
        int customState,
        int playbackDurationMs,
        int forcedDurationMs) {
      forcedRuns.add(new int[] {tick, entity.getId()});
    }
  }

  /** A battle read as the host reads it: a frame after each step, with the log's events. */
  private static final class Played {

    final Standard1v1Battle battle = new Standard1v1Battle(tables, LEVEL, false);

    final BattleSession session = BattleSession.of(battle);

    final TickEventLog log = new TickEventLog();

    final Heard heard = new Heard();

    final List<TickFrame> frames = new ArrayList<>();

    Played() {
      battle.getWorld().addObserver(log);
      battle.getWorld().addObserver(heard);
      session.setStepListener(
          () -> {
            BattleFrame frame = BattleAdapter.frame(session);
            Set<Integer> present = new HashSet<>();
            for (EntityView view : frame.entities()) {
              present.add(view.id());
            }
            frames.add(
                TickFrames.of(
                    frame,
                    log.drainDeaths(),
                    log.drainStrikes(),
                    List.of(),
                    List.of(),
                    log.drainDashes(),
                    log.forcedAnimations(frame.tick(), present)));
          });
    }

    Played play(int tick, String card, int side, int x, int y) {
      battle.play(
          tick,
          battle.getWorld().getRecords().card(card),
          LEVEL,
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

    int idOf(String row, int side) {
      return frames.stream()
          .flatMap(frame -> frame.entities().stream())
          .filter(entity -> entity.name().equals(row) && entity.side() == side)
          .findFirst()
          .orElseThrow()
          .id();
    }

    /** The moments of one character's dashes, in the order told. */
    List<DashMoment> dashesOf(int id) {
      return frames.stream()
          .flatMap(frame -> frame.dashes().stream())
          .filter(dash -> dash.id() == id)
          .map(DashFrame::moment)
          .toList();
    }

    /** Each frame's view of one entity, null where it does not stand. */
    List<EntityFrame> framesOf(int id) {
      List<EntityFrame> of = new ArrayList<>();
      for (TickFrame frame : frames) {
        of.add(
            frame.entities().stream().filter(entity -> entity.id() == id).findFirst().orElse(null));
      }
      return of;
    }

    /** The index of the frame that tells a moment of a character's, the nth of that moment. */
    int frameOf(int id, DashMoment moment, int nth) {
      int seen = 0;
      for (int i = 0; i < frames.size(); i++) {
        for (DashFrame dash : frames.get(i).dashes()) {
          if (dash.id() == id && dash.moment() == moment && seen++ == nth) {
            return i;
          }
        }
      }
      throw new AssertionError("not told: " + moment + " #" + nth + " of " + id);
    }
  }

  @Test
  @DisplayName(
      "a Bandit's dash is told as its wind-up starts, as it starts and as it lands, in that order,"
          + " each in the frame after the tick the battle told it in")
  void banditDash() {
    Played played =
        new Played().play(1, "Assassin", 0, 4500, 11000).play(1, "Golem", 1, 4500, 15500).run(200);
    int bandit = played.idOf("Assassin", 0);

    List<DashMoment> moments = played.dashesOf(bandit);
    assertThat(moments)
        .as("the first dash")
        .startsWith(DashMoment.WINDUP_STARTED, DashMoment.STARTED, DashMoment.LANDED);

    List<int[]> windups = played.heard.windups.stream().filter(w -> w[1] == bandit).toList();
    assertThat(moments.stream().filter(m -> m == DashMoment.WINDUP_STARTED).count())
        .as("each wind-up the battle told, once")
        .isEqualTo(windups.size());
    int first = played.frameOf(bandit, DashMoment.WINDUP_STARTED, 0);
    assertThat(played.frames.get(first).tick())
        .as("the frame after the tick it was told in")
        .isEqualTo(windups.get(0)[0] + 1);
    assertThat(windups.get(0)[2]).as("the wind-up loaded").isPositive();

    int started = played.frameOf(bandit, DashMoment.STARTED, 0);
    int landed = played.frameOf(bandit, DashMoment.LANDED, 0);
    assertThat(started).isGreaterThan(first);
    assertThat(landed).isGreaterThan(started);
    List<EntityFrame> views = played.framesOf(bandit);
    assertThat(views.get(started).state())
        .as("dashing as it starts")
        .isEqualTo(EntityState.DASHING);
    assertThat(views.get(first).state())
        .as("not yet dashing as its wind-up starts")
        .isNotEqualTo(EntityState.DASHING);
  }

  @Test
  @DisplayName(
      "a Mega Knight's dash lands before it leaves DASHING: the landing hold comes after the"
          + " landing")
  void megaKnightLandsBeforeItsHold() {
    Played played =
        new Played()
            .play(1, "MegaKnight", 0, 4500, 9000)
            .play(60, "Golem", 1, 4500, 18000)
            .run(400);
    int knight = played.idOf("MegaKnight", 0);
    int landed = played.frameOf(knight, DashMoment.LANDED, 0);
    assertThat(played.framesOf(knight).get(landed).state())
        .as("still dashing in its landing hold")
        .isEqualTo(EntityState.DASHING);
  }

  @Test
  @DisplayName(
      "a Prince's charge rises from 0 toward 10000 as it walks, and the CHARGING bit shows with"
          + " the charge complete; a Knight tells no charge")
  void princeCharge() {
    Played played =
        new Played()
            .play(1, "Prince", 0, 4500, 9000)
            .play(1, "Knight", 1, 13_500, 22_000)
            .play(100, "Golem", 1, 4500, 15000)
            .run(300);
    List<EntityFrame> prince =
        played.framesOf(played.idOf("Prince", 0)).stream().filter(f -> f != null).toList();
    assertThat(prince)
        .as("a charge on its way")
        .anyMatch(f -> f.chargeProgress() > 0 && f.chargeProgress() < EntityFrame.CHARGE_COMPLETE);
    assertThat(prince)
        .as("a complete charge")
        .anyMatch(f -> f.chargeProgress() >= EntityFrame.CHARGE_COMPLETE && f.charging());
    // The bit is folded in from the step's pending flags, so it trails the progress by a tick: it
    // shows the step after the charge completes and lingers the step after it resets.
    for (int k = 1; k < prince.size(); k++) {
      if (prince.get(k).charging()) {
        assertThat(prince.get(k - 1).chargeProgress())
            .as("charging a step after a complete charge")
            .isGreaterThanOrEqualTo(EntityFrame.CHARGE_COMPLETE);
      }
    }
    int firstComplete = 0;
    while (prince.get(firstComplete).chargeProgress() < EntityFrame.CHARGE_COMPLETE) {
      firstComplete++;
    }
    assertThat(prince.subList(0, firstComplete))
        .as("rising, never falling, until complete")
        .extracting(EntityFrame::chargeProgress)
        .isSorted();

    List<EntityFrame> knight =
        played.framesOf(played.idOf("Knight", 1)).stream().filter(f -> f != null).toList();
    assertThat(knight).allMatch(f -> f.chargeProgress() == EntityFrame.NO_CHARGE && !f.charging());
  }

  @Test
  @DisplayName(
      "the Giant Buffer's forced animation is told on the entity it ran on, with its row's"
          + " columns, from the frame after its run on")
  void giantBufferForcedAnimation() {
    Played played =
        new Played()
            .play(1, "GiantBuffer", 0, 4500, 9000)
            .play(20, "Knight", 0, 4500, 11000)
            .play(60, "Golem", 1, 4500, 18000)
            .run(400);
    assertThat(played.heard.forcedRuns).as("the battle ran one").isNotEmpty();
    int[] run = played.heard.forcedRuns.get(0);
    TickFrame telling =
        played.frames.stream().filter(f -> f.tick() == run[0] + 1).findFirst().orElseThrow();
    EntityFrame shown =
        telling.entities().stream().filter(e -> e.id() == run[1]).findFirst().orElseThrow();
    ForcedAnimationFrame forced = shown.forcedAnimation();
    assertThat(forced.action()).isEqualTo("giantbuffer_play_buff_animation");
    assertThat(forced.customState()).isEqualTo(1);
    assertThat(forced.playbackDurationMs()).isEqualTo(1600);
    assertThat(forced.forcedDurationMs()).isZero();
    assertThat(forced.startTick()).isEqualTo(telling.tick());

    TickFrame before =
        played.frames.stream().filter(f -> f.tick() == run[0]).findFirst().orElseThrow();
    assertThat(before.entities())
        .as("none before its run")
        .filteredOn(e -> e.id() == run[1])
        .allMatch(e -> e.forcedAnimation().equals(ForcedAnimationFrame.NONE));
  }

  @Test
  @DisplayName(
      "a forced animation is held from its run, stamped with the frame that first tells it,"
          + " replaced by a later run, let go at its stop and when its entity leaves the frame")
  void forcedAnimationLifetime() {
    Played played = new Played().play(1, "Knight", 0, 4500, 9000).run(25);
    CharacterEntity knight = played.battle.getPlays().get(0).units().get(0);
    int id = knight.getId();
    TickEventLog log = new TickEventLog();

    log.forcedAnimationRun(30, knight, "first", 1, 500, 0);
    assertThat(log.forcedAnimations(31, Set.of(id)))
        .containsExactly(Map.entry(id, new ForcedAnimationFrame("first", 1, 31, 500, 0)));
    assertThat(log.forcedAnimations(32, Set.of(id)).get(id).startTick())
        .as("its start kept")
        .isEqualTo(31);

    log.forcedAnimationRun(33, knight, "second", 2, -1, 450);
    assertThat(log.forcedAnimations(34, Set.of(id)).get(id))
        .isEqualTo(new ForcedAnimationFrame("second", 2, 34, -1, 450));

    log.forcedAnimationStopped(35, knight, "stop");
    assertThat(log.forcedAnimations(36, Set.of(id))).as("stopped").isEmpty();

    log.forcedAnimationRun(37, knight, "third", 1, 0, 0);
    assertThat(log.forcedAnimations(38, Set.of())).as("it left").isEmpty();
    assertThat(log.forcedAnimations(39, Set.of(id))).as("not back").isEmpty();

    log.forcedAnimationRun(40, knight, "fourth", 1, 0, 0);
    log.stop();
    log.forcedAnimationRun(41, knight, "fifth", 1, 0, 0);
    assertThat(log.forcedAnimations(42, Set.of(id))).as("a stopped log holds none").isEmpty();
  }

  /**
   * Through the real host, as a plugin jar is found: a Bandit's dash moments, a Prince's charge and
   * the Giant Buffer's forced animation reach a plugin's frames, none on the first frame.
   */
  @Test
  @DisplayName("the host tells a plugin dash moments, charges and forced animations")
  void throughTheHost(@TempDir Path services) throws IOException {
    Played played =
        new Played()
            .play(1, "Assassin", 0, 4500, 11000)
            .play(1, "Prince", 0, 13_500, 9000)
            .play(1, "GiantBuffer", 0, 9000, 6000)
            .play(20, "Knight", 0, 9000, 8000)
            .play(1, "Golem", 1, 4500, 15500)
            .play(1, "Golem", 1, 13_500, 18_000);
    Path service = services.resolve("META-INF/services/" + PresentationPlugin.class.getName());
    Files.createDirectories(service.getParent());
    Files.writeString(service, TickPlaysTest.Recording.class.getName() + "\n");
    try (URLClassLoader loader =
        new URLClassLoader(
            new URL[] {services.toUri().toURL()}, TickMotionTest.class.getClassLoader())) {
      PresentationHost host = PresentationHost.discover(null, loader);
      host.follow(played.session);
      played.run(400);
      host.close();
    }
    List<TickFrame> heard = TickPlaysTest.Recording.last.frames;
    assertThat(heard.get(0).dashes()).as("the first frame tells none").isEmpty();
    List<DashFrame> dashes = heard.stream().flatMap(f -> f.dashes().stream()).toList();
    // The host's step listener takes the place of this battle's own frames; the battle's own
    // tells are still heard.
    assertThat(dashes.stream().filter(d -> d.moment() == DashMoment.WINDUP_STARTED).count())
        .as("each wind-up the battle told")
        .isEqualTo(played.heard.windups.size());
    assertThat(dashes)
        .extracting(DashFrame::moment)
        .contains(DashMoment.WINDUP_STARTED, DashMoment.STARTED, DashMoment.LANDED);
    assertThat(heard.stream().flatMap(f -> f.entities().stream()))
        .anyMatch(e -> e.charging() && e.chargeProgress() >= EntityFrame.CHARGE_COMPLETE)
        .anyMatch(e -> e.forcedAnimation().action().equals("giantbuffer_play_buff_animation"));
  }
}
