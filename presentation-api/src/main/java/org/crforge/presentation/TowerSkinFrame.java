/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;

/**
 * A replay's raw tower skin data for one of its battle's players: the replay's {@code
 * battle.hbd[player].sk}, passed through unchanged. No rule of the game's client is applied: which
 * set the client shows (the reroll of {@code ss} from the replay's seed, the check against {@code
 * os}, the fallback to the default set) is the renderer's to work out. The components are named
 * after the replay's own keys, so that what they hold is not read into them.
 *
 * @param player the battle player's index, its place in {@code battle.hbd}: 0 for side 0, 1 for
 *     side 1
 * @param os the {@code os} list, the tower skin set ids in the replay's order; empty when it gives
 *     none
 * @param ss the {@code ss} list, in the replay's order; empty when it gives none
 * @param ats the {@code ats} id; 0 when it gives none
 * @param rnd the {@code rnd} flag; false when it gives none
 */
public record TowerSkinFrame(int player, List<Integer> os, List<Integer> ss, int ats, boolean rnd) {

  public TowerSkinFrame {
    os = List.copyOf(os);
    ss = List.copyOf(ss);
  }
}
