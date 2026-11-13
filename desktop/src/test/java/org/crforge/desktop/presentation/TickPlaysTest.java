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
import java.util.List;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.battle.match.EvolutionItem;
import org.crforge.core.battle.match.MatchCard;
import org.crforge.core.battle.unit.CharacterEntity;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.crforge.core.battle.unit.Standard1v1Battle.Play;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.replay.ReplayFile;
import org.crforge.desktop.replay.ReplayPlayer;
import org.crforge.presentation.BattleStart;
import org.crforge.presentation.CardForm;
import org.crforge.presentation.CardPlayFrame;
import org.crforge.presentation.EntityFrame;
import org.crforge.presentation.PresentContext;
import org.crforge.presentation.PresentationPlugin;
import org.crforge.presentation.TickFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The card plays a plugin's frames tell, from battles on the tables. */
class TickPlaysTest {

  private static final int LEVEL = 11;

  private static GameTables tables;

  @BeforeAll
  static void read() {
    tables = GameTables.loadConfigured();
  }

  /** A battle read as the host reads it: a frame after each step, with the plays it ran. */
  private static final class Played {

    final Standard1v1Battle battle = new Standard1v1Battle(tables, LEVEL, false);

    final BattleSession session = BattleSession.of(battle);

    final TickPlays plays = new TickPlays(battle);

    final List<TickFrame> frames = new ArrayList<>();

    Played() {
      session.setStepListener(
          () ->
              frames.add(
                  TickFrames.of(
                      BattleAdapter.frame(session), List.of(), List.of(), plays.drain())));
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
      return this;
    }

    List<CardPlayFrame> told() {
      return frames.stream().flatMap(frame -> frame.plays().stream()).toList();
    }
  }

  @Test
  @DisplayName(
      "each side's play is told once, in its tick, at the placed point, with its units in order")
  void bothSides() {
    Played played =
        new Played()
            .play(1, "Knight", 0, 4500, 9000)
            .play(1, "SkeletonArmy", 1, 13_500, 22_000)
            .play(3, "Fireball", 0, 9000, 25_000)
            .run(10);

    List<CardPlayFrame> told = played.told();
    assertThat(told)
        .extracting(CardPlayFrame::card)
        .containsExactly("Knight", "SkeletonArmy", "Fireball");
    assertThat(told).extracting(CardPlayFrame::side).containsExactly(0, 1, 0);
    assertThat(told).extracting(CardPlayFrame::form).containsOnly(CardForm.BASIC);

    List<Play> plays = played.battle.getPlays();
    assertThat(plays).hasSize(3);
    for (int i = 0; i < plays.size(); i++) {
      Play play = plays.get(i);
      CardPlayFrame frame = told.get(i);
      assertThat(new int[] {frame.x(), frame.y()})
          .as("the placed point")
          .containsExactly(play.result().x(), play.result().y());
      assertThat(frame.unitIds())
          .as("the units in creation order")
          .containsExactlyElementsOf(play.units().stream().map(CharacterEntity::getId).toList());
      // Told in the frame of the tick it ran, with its units standing in it.
      TickFrame telling =
          played.frames.stream().filter(f -> f.plays().contains(frame)).findFirst().orElseThrow();
      assertThat(telling.tick()).as("the frame after its run").isEqualTo(play.tick() + 1);
      assertThat(telling.entities().stream().map(EntityFrame::id).toList())
          .containsAll(frame.unitIds());
    }
    assertThat(told.get(0).unitCount()).isOne();
    assertThat(told.get(1).unitCount()).as("an army of several units").isGreaterThan(1);
    assertThat(told.get(2).unitIds()).as("a spell makes no unit").isEmpty();
  }

  @Test
  @DisplayName("plays run before the reader began are not told, and each play is told once")
  void startsAtTheBattleAsItStands() {
    Played played = new Played().play(1, "Knight", 0, 4500, 9000).run(5);
    TickPlays late = new TickPlays(played.battle);
    assertThat(late.drain()).as("a play run before it began").isEmpty();
    played.play(8, "Knight", 1, 4500, 22_000).run(5);
    assertThat(late.drain()).extracting(CardPlayFrame::side).containsExactly(1);
    assertThat(late.drain()).as("drained").isEmpty();
  }

  @Test
  @DisplayName("an item's evolution field and a row's form name the same forms")
  void forms() {
    assertThat(TickPlays.form(0)).isEqualTo(CardForm.BASIC);
    assertThat(TickPlays.form(EvolutionItem.EVOLVED)).isEqualTo(CardForm.EVOLVED);
    assertThat(TickPlays.form(EvolutionItem.HERO)).isEqualTo(CardForm.HERO);
    assertThat(TickPlays.form(MatchCard.BASIC_FORM)).isEqualTo(CardForm.BASIC);
    assertThat(TickPlays.form(MatchCard.EVO_FORM)).isEqualTo(CardForm.EVOLVED);
    assertThat(TickPlays.form(MatchCard.HERO_FORM)).isEqualTo(CardForm.HERO);
  }

  /**
   * A replay's plays reach a plugin through the real host: each placed play of both sides told
   * once, as the battle ran it, and none of the plays it refused.
   */
  @Test
  @DisplayName("the host tells a plugin every placed play of a replay, both sides', once")
  void replayThroughTheHost(@TempDir Path services) throws IOException {
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
    assertThat(heard.get(0).plays()).as("the first frame tells none").isEmpty();

    List<Play> placed =
        followed.getSession().getBattle().getPlays().stream()
            .filter(play -> play.result() != null && play.result().placed())
            .toList();
    List<CardPlayFrame> told = heard.stream().flatMap(frame -> frame.plays().stream()).toList();
    assertThat(placed).isNotEmpty();
    assertThat(told).hasSameSizeAs(placed);
    assertThat(told).extracting(CardPlayFrame::side).contains(0, 1);
    for (int i = 0; i < placed.size(); i++) {
      Play play = placed.get(i);
      CardPlayFrame frame = told.get(i);
      assertThat(frame.side()).isEqualTo(play.side());
      assertThat(frame.x()).isEqualTo(play.result().x());
      assertThat(frame.y()).isEqualTo(play.result().y());
      assertThat(frame.form()).isEqualTo(TickPlays.form(play));
      assertThat(frame.card()).isNotEmpty();
      assertThat(frame.unitIds())
          .containsExactlyElementsOf(play.units().stream().map(CharacterEntity::getId).toList());
    }
  }

  /** A class loader with {@link Recording}'s service file on it, as a plugin's jar would have. */
  private static URLClassLoader recordingPlugin(Path services) throws IOException {
    Path service = services.resolve("META-INF/services/" + PresentationPlugin.class.getName());
    Files.createDirectories(service.getParent());
    Files.writeString(service, Recording.class.getName() + "\n");
    return new URLClassLoader(
        new URL[] {services.toUri().toURL()}, TickPlaysTest.class.getClassLoader());
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
  }
}
