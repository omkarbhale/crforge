/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;

/**
 * A battle starting on screen.
 *
 * @param arenaWidthUnits the arena's width in game units (1000 per tile)
 * @param arenaHeightUnits the arena's length in game units
 * @param tick the tick it starts at: 0, or later for a step back, whose battle is played again to
 *     the tick before
 * @param towerSkins a replay's raw tower skin data, one entry for each of its battle's players
 *     whose {@code battle.hbd} entry gives an {@code sk}, in player order; empty for a battle that
 *     is no replay's, and for a replay that gives none
 * @param rndSeed a replay's {@code rndSeed}, unchanged, which the game's client seeds the random it
 *     rerolls a player's {@code ss} with from; 0 for a battle that is no replay's
 * @param replay whether the battle is a replay's: {@code rndSeed} is its seed only then
 */
public record BattleStart(
    int arenaWidthUnits,
    int arenaHeightUnits,
    int tick,
    List<TowerSkinFrame> towerSkins,
    int rndSeed,
    boolean replay) {

  public BattleStart {
    towerSkins = List.copyOf(towerSkins);
  }

  /**
   * A battle that is no replay's, with no tower skin data. Kept on purpose: crforge-render builds
   * against this module live, and its tests and harness tools construct starts with this signature,
   * so removing it breaks them.
   */
  public BattleStart(int arenaWidthUnits, int arenaHeightUnits, int tick) {
    this(arenaWidthUnits, arenaHeightUnits, tick, List.of(), 0, false);
  }
}
