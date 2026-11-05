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
 */
public record TickFrame(
    int tick,
    int timeMs,
    boolean ended,
    int winner,
    List<EntityFrame> entities,
    boolean overtime,
    int elixirRate,
    List<SideFrame> sides) {

  public TickFrame {
    entities = List.copyOf(entities);
    sides = List.copyOf(sides);
  }

  /** A frame outside a match, for code written before the match's fields were added. */
  public TickFrame(int tick, int timeMs, boolean ended, int winner, List<EntityFrame> entities) {
    this(tick, timeMs, ended, winner, entities, false, 0, List.of());
  }
}
