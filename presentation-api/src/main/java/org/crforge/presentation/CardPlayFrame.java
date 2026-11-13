/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;

/**
 * A card play that ran in the tick just run and put its card down, told once, whoever made it: the
 * viewer through the host, a plugin's {@link CardCommand.Play}, a model playing a side or a replay.
 * A play the battle refused put nothing down and is not told. Positions are in game units.
 *
 * @param side the side that played, 0 or 1
 * @param card the row name of the card played from the hand, as its {@link CardFrame#name} showed
 *     it: for a Mirror, the Mirror; for a variant card, the card, not the option picked
 * @param form the form the play put its card down in: for a Mirror, the form of the card it
 *     repeated
 * @param x the placed point along the width, where the battle put the play down: the requested
 *     point moved by the placement's search, and where a spell is cast
 * @param y the placed point along the length
 * @param unitIds the {@link EntityFrame#id}s of the units the play made, in the order it made them,
 *     which is their place in the card's formation: the first at index 0. Empty for a play that
 *     makes no unit, a spell's. A unit that waits its turn to deploy is listed as the others are
 */
public record CardPlayFrame(
    int side, String card, CardForm form, int x, int y, List<Integer> unitIds) {

  public CardPlayFrame {
    unitIds = List.copyOf(unitIds);
  }

  /** How many units the play made: the size of {@link #unitIds}. */
  public int unitCount() {
    return unitIds.size();
  }
}
