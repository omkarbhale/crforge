/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.core.battle.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ScenarioScriptTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static GameTables tables;

  @BeforeAll
  static void load() {
    tables = GameTables.loadConfigured();
  }

  /** A document from JSON written with single quotes. */
  private static JsonNode json(String text) {
    try {
      return MAPPER.readTree(text.replace('\'', '"'));
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static ScenarioScript read(String text) {
    return ScenarioScript.read(json(text), tables);
  }

  private static List<String> problems(String text) {
    try {
      read(text);
    } catch (ScenarioScriptException e) {
      return e.problems();
    }
    throw new AssertionError("the scenario was read: " + text);
  }

  @Test
  void aScenarioIsToldApartFromAReplayByItsMarker() {
    assertThat(ScenarioScript.isScenario(json("{'crforgeScenario': 1, 'plays': []}"))).isTrue();
    assertThat(ScenarioScript.isScenario(json("{'battle': {}, 'cmd': []}"))).isFalse();
  }

  @Test
  void theDefaultsAreLevelElevenTowersThatFightAndNoEndTick() {
    ScenarioScript script =
        read("{'crforgeScenario': 1, 'plays': [{'side': 0, 'card': 'Knight', 'at': [3.5, 10]}]}");

    assertThat(script.endTick()).isEqualTo(ScenarioScript.NO_END_TICK);
    assertThat(script.towersAttack()).isTrue();
    assertThat(script.towers())
        .extracting(Standard1v1Battle.Towers::level, Standard1v1Battle.Towers::kingLevel)
        .containsOnly(org.assertj.core.groups.Tuple.tuple(11, 11));
    assertThat(script.plays())
        .containsExactly(new ScenarioScript.Play("cmd0", 0, 0, "Knight", 11, 0, 3500, 10000));
    assertThat(script.plays().get(0).runTick()).isEqualTo(20);
  }

  @Test
  void aLineSpreadsItsCountEvenlyFromOnePointToTheOther() {
    ScenarioScript script =
        read(
            "{'crforgeScenario': 1, 'plays': [{'side': 1, 'card': 'Knight', 'count': 3,"
                + " 'line': {'from': [1, 20], 'to': [17, 22]}}]}");

    assertThat(script.plays())
        .extracting(ScenarioScript.Play::side, ScenarioScript.Play::x, ScenarioScript.Play::y)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(1, 1000, 20000),
            org.assertj.core.groups.Tuple.tuple(1, 9000, 21000),
            org.assertj.core.groups.Tuple.tuple(1, 17000, 22000));
  }

  @Test
  void aGridFillsRowByRowAndACountTakesItsFirstPoints() {
    ScenarioScript script =
        read(
            "{'crforgeScenario': 1, 'plays': [{'side': 0, 'card': 'Archer', 'count': 5,"
                + " 'grid': {'from': [2, 4], 'to': [8, 8], 'columns': 3, 'rows': 2}}]}");

    assertThat(script.plays())
        .extracting(ScenarioScript.Play::x, ScenarioScript.Play::y)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(2000, 4000),
            org.assertj.core.groups.Tuple.tuple(5000, 4000),
            org.assertj.core.groups.Tuple.tuple(8000, 4000),
            org.assertj.core.groups.Tuple.tuple(2000, 8000),
            org.assertj.core.groups.Tuple.tuple(5000, 8000));
  }

  @Test
  void timesAreSecondsOrTicksAndEverySpacesTheCopiesAndPlaysRunInDropOrder() {
    ScenarioScript script =
        read(
            "{'crforgeScenario': 1, 'endTime': 10, 'plays': ["
                + "{'side': 1, 'card': 'Knight', 'at': [9, 20], 'time': 2},"
                + "{'side': 0, 'card': 'HogRider', 'count': 3, 'at': [9, 10], 'everyTicks': 7,"
                + " 'tick': 5}]}");

    assertThat(script.endTick()).isEqualTo(200);
    assertThat(script.plays())
        .extracting(
            ScenarioScript.Play::name, ScenarioScript.Play::card, ScenarioScript.Play::dropTick)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("cmd0", "HogRider", 5),
            org.assertj.core.groups.Tuple.tuple("cmd1", "HogRider", 12),
            org.assertj.core.groups.Tuple.tuple("cmd2", "HogRider", 19),
            org.assertj.core.groups.Tuple.tuple("cmd3", "Knight", 40));
  }

  @Test
  void aFormNamesTheEvolvedOrHeroRowAndALevelIsKept() {
    ScenarioScript script =
        read(
            "{'crforgeScenario': 1, 'plays': ["
                + "{'side': 0, 'card': 'Skeletons', 'form': 'evolved', 'level': 14, 'at': [9, 10]},"
                + "{'side': 0, 'card': 'Skeletons_EV1', 'at': [9, 10]},"
                + "{'side': 1, 'card': 'Knight', 'form': 'hero', 'at': [9, 22]}]}");

    assertThat(script.plays())
        .extracting(ScenarioScript.Play::card, ScenarioScript.Play::level)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Skeletons_EV1", 14),
            org.assertj.core.groups.Tuple.tuple("Skeletons_EV1", 11),
            org.assertj.core.groups.Tuple.tuple("Knight_hero", 11));
    assertThat(script.cardsBySide())
        .containsExactly(List.of("Skeletons_EV1"), List.of("Knight_hero"));
  }

  @Test
  void towersMayBeGivenForBothSidesOrEachAndMayHoldTheirFire() {
    ScenarioScript both =
        read("{'crforgeScenario': 1, 'towers': {'level': 9, 'attack': false}, 'plays': []}");
    ScenarioScript each =
        read(
            "{'crforgeScenario': 1, 'towers': [{'level': 13}, {'level': 7, 'kingLevel': 12}],"
                + " 'plays': []}");

    assertThat(both.towersAttack()).isFalse();
    assertThat(both.towers()).extracting(Standard1v1Battle.Towers::level).containsExactly(9, 9);
    assertThat(each.towersAttack()).isTrue();
    assertThat(each.towers())
        .extracting(Standard1v1Battle.Towers::level, Standard1v1Battle.Towers::kingLevel)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(13, 13),
            org.assertj.core.groups.Tuple.tuple(7, 12));
  }

  @Test
  void anUnknownCardIsNamedWithTheRowsItMayHaveMeant() {
    assertThat(
            problems(
                "{'crforgeScenario': 1, 'plays': [{'side': 1, 'card': 'MotherWitch', 'at': [9,"
                    + " 22]}]}"))
        .singleElement()
        .asString()
        .startsWith("plays[0].card: the game tables have no card row \"MotherWitch\"")
        .contains("did you mean WitchMother");
    assertThat(
            problems(
                "{'crforgeScenario': 1, 'plays': [{'side': 1, 'card': 'Knigt', 'at': [9, 22]}]}"))
        .singleElement()
        .asString()
        .contains("did you mean Knight");
    assertThat(
            problems(
                "{'crforgeScenario': 1, 'plays': [{'side': 1, 'card': 'SuperWitch', 'form': 'evolved',"
                    + " 'at': [9, 22]}]}"))
        .singleElement()
        .asString()
        .contains("SuperWitch has no evolved form");
  }

  @Test
  void everyProblemIsListedWithTheFieldItIsIn() {
    List<String> problems =
        problems(
            "{'crforgeScenario': 2, 'sede': 4, 'endTick': 0, 'plays': ["
                + "{'side': 'green', 'card': 'Knight', 'at': [30, 10]},"
                + "{'side': 0, 'card': 'Knight', 'level': 99, 'line': {'from': [0, 0], 'to': [1,"
                + " 1]}},"
                + "{'side': 0, 'card': 'Knight', 'at': [1, 1], 'grid': {}},"
                + "{'side': 0, 'card': 'Knight', 'at': [1, 1], 'tick': 3, 'time': 1},"
                + "{'side': 0, 'card': 'Knight', 'at': [1, 1], 'count': 0, 'colour': 'x'}]}");

    assertThat(problems)
        .anyMatch(p -> p.startsWith("crforgeScenario: must be 1"))
        .anyMatch(p -> p.startsWith("sede: not a field"))
        .anyMatch(p -> p.startsWith("endTick: must be a whole number of ticks of at least 1"))
        .anyMatch(p -> p.startsWith("plays[0].side: must be 0 (the side whose half"))
        .anyMatch(p -> p.startsWith("plays[0].at: [30,10] is outside the arena, x 0 to 18"))
        .anyMatch(p -> p.startsWith("plays[1].level: must be a whole number from 1 to 16"))
        .anyMatch(p -> p.startsWith("plays[1].count: a line needs a count"))
        .anyMatch(p -> p.startsWith("plays[2]: give exactly one of at"))
        .anyMatch(p -> p.startsWith("plays[3]: give tick or time, not both"))
        .anyMatch(p -> p.startsWith("plays[4].colour: not a field"))
        .anyMatch(p -> p.startsWith("plays[4].count: must be a whole number of at least 1"));
  }

  @Test
  void theMirrorNeedsAMatchAndIsRefused() {
    assertThat(
            problems(
                "{'crforgeScenario': 1, 'plays': [{'side': 0, 'card': 'Mirror', 'at': [9, 10]}]}"))
        .singleElement()
        .asString()
        .isEqualTo("plays[0].card: Mirror repeats its side's last card, which needs a match");
  }

  @Test
  void aPlayAfterTheEndIsRefused() {
    assertThat(
            problems(
                "{'crforgeScenario': 1, 'endTime': 5, 'plays': [{'side': 0, 'card': 'Knight',"
                    + " 'at': [9, 10], 'time': 5}]}"))
        .singleElement()
        .asString()
        .isEqualTo("plays[0]: deploys on tick 120, after the scenario's end tick 100");
  }

  @Test
  void aScenarioMatchPlaysEveryCopyOfACardOnTheSameTickWithNoElixirOrHand() {
    ScenarioScript script =
        read(
            "{'crforgeScenario': 1, 'endTime': 5, 'plays': ["
                + "{'side': 0, 'card': 'Skeletons_EV1', 'count': 20, 'line': {'from': [0.5,"
                + " 11.5], 'to': [17.5, 11.5]}},"
                + "{'side': 1, 'card': 'WitchMother', 'count': 20, 'line': {'from': [0.5,"
                + " 20.5], 'to': [17.5, 20.5]}}]}");
    Standard1v1Battle battle = script.build(tables);

    assertThat(battle.getMatch()).isNotNull();
    assertThat(battle.isFreePlays()).isTrue();
    for (int tick = 0; tick < 25; tick++) {
      battle.getBattle().step();
    }

    assertThat(battle.getPlays()).hasSize(40);
    assertThat(battle.getPlays())
        .allSatisfy(
            play -> {
              assertThat(play.tick()).isEqualTo(20);
              assertThat(play.result().placed()).as(play.name()).isTrue();
              assertThat(play.units()).as(play.name()).isNotEmpty();
            });
    assertThat(battle.getPlays().stream().filter(p -> p.side() == 0))
        .allSatisfy(
            p -> assertThat(p.units()).allMatch(u -> u.getData().name().contains("Skeleton")));
    assertThat(battle.getPlays().stream().filter(p -> p.side() == 1))
        .allSatisfy(p -> assertThat(p.units()).allMatch(u -> u.getData().name().contains("Witch")));
    // Nothing was spent and no card left the deck's order: the hand and elixir gate nothing.
    assertThat(battle.getMatch().side(0).getDeployCounter()).isEqualTo(20);
    assertThat(battle.getMatch().side(1).getDeployCounter()).isEqualTo(20);
  }

  @Test
  void theMatchEndsWhenAKingFallsAndNothingRunsAfter() {
    ScenarioScript script =
        read(
            "{'crforgeScenario': 1, 'towers': [{'level': 11}, {'level': 1}], 'plays': ["
                + "{'side': 0, 'card': 'Rocket', 'level': 16, 'count': 12, 'at': [9, 29],"
                + " 'everyTicks': 10},"
                + "{'side': 0, 'card': 'Knight', 'at': [9, 10], 'time': 30}]}");
    Standard1v1Battle battle = script.build(tables);

    int tick = 0;
    while (!battle.getBattle().getMode().isOver() && tick < 1200) {
      battle.getBattle().step();
      tick++;
    }

    assertThat(battle.getMatch().isEnded()).isTrue();
    assertThat(battle.getMatch().getWinner()).isZero();
    assertThat(battle.getMatch().crowns(0)).isEqualTo(3);
    assertThat(battle.getBattle().getMode().isOver()).as("the battle stopped").isTrue();
    // The Knight, dropped at 30 s, never ran: the battle had stopped by then.
    assertThat(battle.getBattle().getTick()).isLessThan(620);
    assertThat(battle.getPlays()).hasSize(12).allMatch(play -> play.matchCode() == 0);
  }
}
