/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * One card of a hand, or the next card, at a tick.
 *
 * @param name the card row name, empty for {@link #NONE}
 * @param cost its cost in whole elixir
 * @param level the level its side plays it at, counted from 1 across all rarities
 * @param pending whether it was played through the session's commands, from the screen or by a
 *     model playing its side, and waits for its play to run; a replay's queued plays never show it
 * @param available whether it can be selected now; never for the next card, which is not in hand
 * @param evolutionCount the king's count for the card: its plays since its last evolved play
 * @param evolutionNeed the count its evolved row asks for, 0 for a card that does not evolve
 * @param evolved whether its next play is evolved
 */
public record CardFrame(
    String name,
    int cost,
    int level,
    boolean pending,
    boolean available,
    int evolutionCount,
    int evolutionNeed,
    boolean evolved) {

  /** No card: an empty hand slot, or no next card. */
  public static final CardFrame NONE = new CardFrame("", 0, 0, false, false, 0, 0, false);
}
