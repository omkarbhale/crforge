/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * What a viewer's press or release on a plugin's art asks of the hand, for the host to carry out
 * ({@link PresentationPlugin#cardCommandAt}). It is a request, not a play: the host checks it and
 * issues the same command its own hand buttons and arena clicks issue, under the same rules, or
 * refuses it as it refuses theirs. Nothing of it reaches the battle but through the host.
 */
public sealed interface CardCommand
    permits CardCommand.Select, CardCommand.Play, CardCommand.Deselect {

  /**
   * Select a hand slot's card, as its hand button does. The host refuses an empty slot, a card the
   * side cannot play now, and a side or slot that does not exist, and keeps what was selected.
   *
   * @param side the side whose hand it is, 0 or 1 (the {@link SideFrame#side})
   * @param slot the hand slot, counted from 0 (an index of {@link SideFrame#hand})
   */
  record Select(int side, int slot) implements CardCommand {}

  /**
   * Play the selected card at a battle position, as a click on the arena there does: at the centre
   * of the tile holding the position. The host refuses it with no card selected or a position off
   * the arena, and refuses a play the battle refuses; a play given deselects the card.
   *
   * @param x the position along the width, in game units (an {@link ArenaProjection#x})
   * @param y the position along the length, in game units (an {@link ArenaProjection#y})
   */
  record Play(float x, float y) implements CardCommand {}

  /** Let go of the selected card, as a right click does. */
  record Deselect() implements CardCommand {}
}
