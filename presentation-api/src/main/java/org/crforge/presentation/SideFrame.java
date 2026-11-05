/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;

/**
 * One side of a match at a tick, detached from the battle.
 *
 * @param side the battle's side, 0 or 1
 * @param elixir the elixir in ten-thousandths, as the match holds it
 * @param wholeElixir the whole elixir a play is checked against
 * @param crowns the crowns the side has taken
 * @param hand the hand's slots in order, {@link CardFrame#NONE} for an empty slot
 * @param next the card that refills the hand next, {@link CardFrame#NONE} for none
 */
public record SideFrame(
    int side, int elixir, int wholeElixir, int crowns, List<CardFrame> hand, CardFrame next) {

  public SideFrame {
    hand = List.copyOf(hand);
  }
}
