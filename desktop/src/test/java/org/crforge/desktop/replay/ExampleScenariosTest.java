/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.replay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.EntityView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every example scenario ({@code examples/scenarios}) opens as the viewer opens a replay, and plays
 * to its end tick with every play placed; a scenario with problems is refused with each problem
 * listed.
 */
class ExampleScenariosTest {

  static Stream<Path> files() throws IOException {
    Path folder = Paths.get(System.getProperty("crforge.exampleScenarios"));
    try (Stream<Path> listed = Files.list(folder)) {
      List<Path> files = listed.filter(f -> f.toString().endsWith(".json")).sorted().toList();
      assertThat(files).isNotEmpty();
      return files.stream();
    }
  }

  /** The most ticks a check plays an example: past every example's last play. */
  private static final int TICKS = 600;

  @ParameterizedTest
  @MethodSource("files")
  void theViewerPlaysEachExampleInAScenarioMatchWithEveryPlayPlaced(Path file) throws IOException {
    ReplayFile scenario = ReplayFile.read(file, Replays.tables());
    assertThat(scenario.refusals()).isEmpty();
    assertThat(scenario.isScenario()).isTrue();
    ReplayPlayer player = new ReplayPlayer(scenario, Replays.tables());

    while (player.getSession().tick() < TICKS && player.step()) {
      // past every play
    }

    assertThat(player.getSession().getHalted()).isNull();
    assertThat(player.getSession().match()).isNotNull();
    List<Standard1v1Battle.Play> plays = player.getSession().getBattle().getPlays();
    assertThat(plays).hasSize(scenario.script().plays().size());
    assertThat(plays)
        .allSatisfy(
            play -> {
              assertThat(play.matchCode()).as(play.name()).isZero();
              assertThat(play.result().placed()).as(play.name()).isTrue();
            });
  }

  @Test
  @DisplayName("all 40 cards of the 20 + 20 example deploy on one tick and both sides' units stand")
  void skeletonsAndMotherWitchesAllDeployTogether() throws IOException {
    Path file =
        Paths.get(System.getProperty("crforge.exampleScenarios"))
            .resolve("01-skeletons-evo-vs-mother-witch.json");
    ReplayPlayer player =
        new ReplayPlayer(ReplayFile.read(file, Replays.tables()), Replays.tables());

    while (player.getSession().tick() < 25) {
      assertThat(player.step()).isTrue();
    }

    List<Standard1v1Battle.Play> plays = player.getSession().getBattle().getPlays();
    assertThat(plays).hasSize(40).allMatch(play -> play.tick() == 20 && play.result().placed());
    assertThat(plays.stream().filter(play -> play.side() == 0)).hasSize(20);
    List<EntityView> units =
        BattleAdapter.frame(player.getSession()).entities().stream()
            .filter(e -> e.kind() == EntityView.Kind.TROOP)
            .toList();
    assertThat(units.stream().filter(e -> e.side() == 0 && e.name().contains("Skeleton")))
        .hasSizeGreaterThanOrEqualTo(20);
    assertThat(units.stream().filter(e -> e.side() == 1 && e.name().contains("WitchMother")))
        .hasSize(20);
  }

  @Test
  void aScenarioWithProblemsIsRefusedWithEachListed() throws IOException {
    var document =
        new ObjectMapper()
            .readTree(
                "{\"crforgeScenario\": 1, \"plays\": [{\"side\": 0, \"card\": \"MotherWitch\","
                    + " \"at\": [9, 10]}, {\"side\": 3, \"card\": \"Knight\", \"at\": [9, 10]}]}");

    ReplayFile scenario = ReplayFile.parse(Path.of("bad.json"), document, Replays.tables());

    assertThat(scenario.playable()).isFalse();
    assertThat(scenario.isScenario()).isTrue();
    assertThat(scenario.refusals())
        .hasSize(2)
        .anyMatch(r -> r.startsWith("plays[0].card:") && r.contains("WitchMother"))
        .anyMatch(r -> r.startsWith("plays[1].side:"));
    assertThat(scenario.describe()).anyMatch(line -> line.startsWith("scenario: "));
    assertThat(new ReplayPlayer(scenario, Replays.tables()).getSession()).isNull();
  }
}
