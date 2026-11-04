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
 */
public record TickFrame(
    int tick, int timeMs, boolean ended, int winner, List<EntityFrame> entities) {

  public TickFrame {
    entities = List.copyOf(entities);
  }
}
