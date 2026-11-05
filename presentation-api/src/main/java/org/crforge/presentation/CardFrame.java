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
 * @param pending whether it has been played and waits for its play to run
 * @param available whether it can be selected now; never for the next card, which is not in hand
 */
public record CardFrame(String name, int cost, int level, boolean pending, boolean available) {

  /** No card: an empty hand slot, or no next card. */
  public static final CardFrame NONE = new CardFrame("", 0, 0, false, false);

  /** Whether this stands for no card. */
  public boolean empty() {
    return name.isEmpty();
  }
}
