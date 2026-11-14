/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.core.battle.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.crforge.core.battle.GameData;
import org.crforge.core.battle.Shipped;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The evolved Giant's row (Giant_EV1). Its attack sequence's last entry sets
 * CustomOnKillDeathEffectOverride, an effect, so the row's columns are all modelled and the row is
 * made. Its actions are not modelled: the first the battle refuses as it reads them is the spawn of
 * its first rage area (giant_evo_spawn_tower_rage_1), an area effect spawned at a point with an
 * action to run on it. Beyond that, its starting and rage actions call expression functions the
 * battle's function table does not name (is_enough_to_kill_target, direction_offset_x and _y,
 * is_contained_by_circle).
 */
class BattleGiantEvoTest {

  @Test
  @DisplayName("the evolved Giant's row sets no column the battle does not model")
  void itsColumnsAreModelled() {
    assertThat(
            Shipped.column(Shipped.unitRow("Giant_EV1"), "AttackSequenceList")
                .get(3)
                .has("CustomOnKillDeathEffectOverride"))
        .as("the entry the refusal was over")
        .isTrue();
    assertThat(GameData.unit("Giant_EV1").unmodelledColumns()).isEmpty();
  }

  @Test
  @DisplayName(
      "an evolved Giant is made, and refused as its starting actions are read, which the battle does"
          + " not model; not at its columns")
  void itsActionsAreRefused() {
    Standard1v1Battle match =
        new Standard1v1Battle(GameData.tables(), Standard1v1Battle.DEFAULT_LEVEL, false);
    match.deploy(
        0, GameData.unit("Giant_EV1"), Standard1v1Battle.DEFAULT_LEVEL, 0, 3500, 18000, "Giant");
    assertThatThrownBy(
            () -> {
              for (int tick = 0; tick < 100; tick++) {
                match.getBattle().step();
              }
            })
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("giant_evo_spawn_tower_rage_1")
        .hasMessageContaining("ActionToRunOnSpawned")
        .hasMessageNotContaining("sets columns the battle does not model");
  }
}
