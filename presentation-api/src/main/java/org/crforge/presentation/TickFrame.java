/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;

/**
 * The state of a battle after a tick, detached from the battle: it never changes.
 *
 * @param tick how many ticks the battle has run. A tick's own events belong to the tick before this
 *     number: the frame reads the battle after its count moved on
 * @param timeMs the battle's clock in milliseconds
 * @param ended whether the match has ended
 * @param winner the side that won, 0 or 1, or -1 for none (yet)
 * @param entities every entity in the battle at this tick; one that left during the tick is absent
 * @param overtime whether the match is in overtime
 * @param elixirRate the elixir rate as a multiple of the opening rate (1, 2 or 3), 0 outside a
 *     match
 * @param sides side 0 then side 1 in a match; empty outside one
 * @param section the match's current section, counted from 0; 0 outside a match
 * @param sectionLeftMs the time left in that section in milliseconds, 0 for a section without a
 *     length and outside a match
 * @param frozen whether the match's end has frozen its timeline: the section and {@code
 *     sectionLeftMs} stop, while {@code timeMs} keeps following the ticks
 * @param endTimerMs the match's end timer: 0 until it ends, then rising
 * @param tiebreakMs the tiebreaker's time: 0 until it begins, then rising
 * @param nextCardCooldownMs the length a hand's refill countdown restarts at after filling a slot,
 *     in milliseconds
 */
public record TickFrame(
    int tick,
    int timeMs,
    boolean ended,
    int winner,
    List<EntityFrame> entities,
    boolean overtime,
    int elixirRate,
    List<SideFrame> sides,
    int section,
    int sectionLeftMs,
    boolean frozen,
    int endTimerMs,
    int tiebreakMs,
    int nextCardCooldownMs) {

  public TickFrame {
    entities = List.copyOf(entities);
    sides = List.copyOf(sides);
  }
}
