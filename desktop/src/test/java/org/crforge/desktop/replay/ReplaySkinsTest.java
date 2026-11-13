/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.replay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.List;
import org.crforge.desktop.replay.ReplaySkins.PlayerSkins;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A replay's raw tower skin data, read as it stands for a presentation plugin. */
class ReplaySkinsTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  private static JsonNode json(String text) throws Exception {
    return JSON.readTree(text.replace('\'', '"'));
  }

  @Test
  @DisplayName("each player's sk is passed on unchanged: order, repeats and the seed kept")
  void unchanged() throws Exception {
    ReplaySkins skins =
        ReplaySkins.of(
            json(
                "{'rndSeed': 1131, 'battle': {'hbd': ["
                    + "{'kt': 16, 'sk': {'os': [87000001, 87000007, 94000033],"
                    + " 'ss': [94000033, 87000001, 94000033], 'ats': 94000033, 'rnd': true}},"
                    + "{'kt': 16, 'sk': {'os': [87000001], 'ss': [], 'ats': 0, 'rnd': false}}"
                    + "]}}"));
    assertThat(skins.rndSeed()).isEqualTo(1131);
    assertThat(skins.players())
        .containsExactly(
            new PlayerSkins(
                0,
                List.of(87000001, 87000007, 94000033),
                List.of(94000033, 87000001, 94000033),
                94000033,
                true),
            new PlayerSkins(1, List.of(87000001), List.of(), 0, false));
  }

  @Test
  @DisplayName("a field the sk leaves out reads as empty, 0 or false; a player with no sk as none")
  void missingFields() throws Exception {
    ReplaySkins skins =
        ReplaySkins.of(
            json(
                "{'rndSeed': 7, 'battle': {'hbd': [{'kt': 11}, {'kt': 11, 'sk': {'ats': 87000001}}]}}"));
    assertThat(skins.players())
        .as("player 0 gives no sk")
        .containsExactly(new PlayerSkins(1, List.of(), List.of(), 87000001, false));
  }

  @Test
  @DisplayName("an sk of other types than the client reads is passed over, never altered")
  void malformed() throws Exception {
    ReplaySkins skins =
        ReplaySkins.of(
            json(
                "{'rndSeed': 'x', 'battle': {'hbd': ["
                    + "{'sk': {'os': ['87000001'], 'ats': 87000001}},"
                    + "{'sk': {'os': [87000001], 'ats': 87000001.5}},"
                    + "{'sk': {'os': 87000001}},"
                    + "{'sk': {'rnd': 1}},"
                    + "{'sk': [87000001]},"
                    + "{'sk': {'os': [87000001], 'ats': 87000001}}"
                    + "]}}"));
    assertThat(skins.rndSeed()).as("a seed that is no int").isZero();
    assertThat(skins.players())
        .containsExactly(new PlayerSkins(5, List.of(87000001), List.of(), 87000001, false));
  }

  @Test
  @DisplayName("a replay with no hbd and no seed reads as none")
  void none() throws Exception {
    assertThat(ReplaySkins.of(json("{'battle': {}}"))).isEqualTo(ReplaySkins.NONE);
    assertThat(ReplaySkins.of(json("{}"))).isEqualTo(ReplaySkins.NONE);
  }

  @Test
  @DisplayName("an hbd that is not a list gives no player, and the seed is still read")
  void hbdNotAList() throws Exception {
    ReplaySkins object =
        ReplaySkins.of(json("{'rndSeed': 9, 'battle': {'hbd': {'a': {'sk': {'os': [1]}}}}}"));
    assertThat(object.rndSeed()).isEqualTo(9);
    assertThat(object.players()).isEmpty();
    ReplaySkins text = ReplaySkins.of(json("{'rndSeed': 9, 'battle': {'hbd': 'x'}}"));
    assertThat(text.rndSeed()).isEqualTo(9);
    assertThat(text.players()).isEmpty();
  }

  @Test
  @DisplayName("a replay as the client writes it carries both players' sets and its seed")
  void everyField() {
    ObjectNode document = Replays.archerQueenWithEveryField();
    ReplayFile replay =
        ReplayFile.parse(Path.of("archer_queen_every_field.json"), document, Replays.tables());
    assertThat(replay.skins().rndSeed()).isEqualTo(document.path("rndSeed").asInt());
    assertThat(replay.skins().players())
        .containsExactly(
            new PlayerSkins(0, List.of(87000001), List.of(87000001), 87000001, false),
            new PlayerSkins(1, List.of(87000001), List.of(87000001), 87000001, false));
  }
}
