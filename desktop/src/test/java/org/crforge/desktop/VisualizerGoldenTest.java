/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.crforge.core.battle.data.GameTables;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the visualizer draws is what it drew when the committed traces were written ({@link
 * VisualizerTrace}), with every control used. The traces are of one data version; on the tables of
 * another version the checks are skipped.
 */
class VisualizerGoldenTest {

  private static GameTables tables;

  private static Path folder;

  private static Path example;

  @BeforeAll
  static void read() {
    tables = GameTables.loadConfigured();
    assumeTrue(
        VisualizerGoldenTest.class
                .getClassLoader()
                .getResource(VisualizerTrace.resource("debug", tables.version()))
            != null,
        "no committed trace for data version " + tables.version());
    folder = Path.of(System.getProperty("crforge.gameTables"));
    example = Path.of(System.getProperty("crforge.exampleReplay"));
  }

  @Test
  @DisplayName("the Ladder screen draws its committed trace, control by control")
  void debugScreen() throws IOException {
    assertMatches("debug", VisualizerTrace.debug(tables, folder, example));
  }

  @Test
  @DisplayName("the replay viewer draws its committed trace, control by control")
  void replayViewer() throws IOException {
    assertMatches("replay", VisualizerTrace.replay(tables, folder, example));
  }

  @Test
  @DisplayName("two runs of a scenario in one process draw the same trace")
  void deterministic() throws IOException {
    assertThat(VisualizerTrace.replay(tables, folder, example))
        .isEqualTo(VisualizerTrace.replay(tables, folder, example));
  }

  private static InputStream golden(String scenario) {
    return VisualizerGoldenTest.class
        .getClassLoader()
        .getResourceAsStream(VisualizerTrace.resource(scenario, tables.version()));
  }

  private static void assertMatches(String scenario, List<String> actual) throws IOException {
    String resource = VisualizerTrace.resource(scenario, tables.version());
    try (InputStream golden = golden(scenario)) {
      List<String> expected =
          new String(golden.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
      for (int line = 0; line < Math.min(expected.size(), actual.size()); line++) {
        assertThat(actual.get(line))
            .as(
                "line %d of %s (./gradlew :desktop:visualizerGolden writes it anew, for a change"
                    + " that means to alter what is drawn)",
                line + 1, resource)
            .isEqualTo(expected.get(line));
      }
      assertThat(actual).as("the frames of %s", resource).hasSameSizeAs(expected);
    }
  }
}
