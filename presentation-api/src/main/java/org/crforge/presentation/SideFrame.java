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
 * @param abilities the king's champion slots in order, {@link AbilityFrame#NONE} for a slot that
 *     follows no champion
 * @param nextCardCooldownLeftMs what is left of the hand's refill countdown, in milliseconds: an
 *     empty slot refills when it runs out. It also runs with a full hand, and a slot emptied with
 *     it at 0 refills on its play's own step
 * @param deployArea the tiles the side may deploy a troop on now, as far as the field decides it:
 *     every tile but those the other side's standing towers keep free of the side's plays. At the
 *     start that is the side's own half; a princess tower that falls opens the pocket in front of
 *     it. A card's own rules close more, or open more for one that may go anywhere: its row limits,
 *     the buildings standing, the water and the map's blocked cells, which {@link
 *     PresentContext#selectedArea} reads for the selected card. {@link DeployArea#NONE} when not
 *     told
 */
public record SideFrame(
    int side,
    int elixir,
    int wholeElixir,
    int crowns,
    List<CardFrame> hand,
    CardFrame next,
    List<AbilityFrame> abilities,
    int nextCardCooldownLeftMs,
    DeployArea deployArea) {

  public SideFrame {
    hand = List.copyOf(hand);
    abilities = List.copyOf(abilities);
    if (deployArea == null) {
      deployArea = DeployArea.NONE;
    }
  }

  /**
   * A side with no deploy area told. Kept on purpose: crforge-render builds against this module
   * live, and its tests construct sides with this signature, so removing it breaks them.
   */
  public SideFrame(
      int side,
      int elixir,
      int wholeElixir,
      int crowns,
      List<CardFrame> hand,
      CardFrame next,
      List<AbilityFrame> abilities,
      int nextCardCooldownLeftMs) {
    this(
        side,
        elixir,
        wholeElixir,
        crowns,
        hand,
        next,
        abilities,
        nextCardCooldownLeftMs,
        DeployArea.NONE);
  }
}
