/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * One champion slot of a king at a tick: the champion it follows and its ability's button.
 *
 * @param champion the name of the deck card the slot's champion was found in, or the champion's row
 *     name when it was found in none; empty for {@link #NONE}
 * @param cost the ability's cost in whole elixir
 * @param state the button's state as the game works it out
 * @param cooldownMs the slot's cooldown left, in milliseconds
 * @param charges the slot's charges left, or -1 when its uses are not counted
 * @param pending whether an ability command given from the screen for the slot waits to run
 * @param available whether the button can be tapped now
 */
public record AbilityFrame(
    String champion,
    int cost,
    AbilityState state,
    int cooldownMs,
    int charges,
    boolean pending,
    boolean available) {

  /** No champion: a slot that follows none. */
  public static final AbilityFrame NONE =
      new AbilityFrame("", 0, AbilityState.UNKNOWN, 0, 0, false, false);
}
