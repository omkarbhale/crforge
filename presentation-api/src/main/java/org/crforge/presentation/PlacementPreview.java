/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;

/**
 * Where a play of the selected card from the tile under the pointer would be placed against the
 * battle as it stands now: the host's own placement preview, worked out as the play's placement
 * works it out and changing nothing. The host sends a play from the centre of the tile under the
 * pointer, and the battle's placement search may move it to another tile, or find none.
 *
 * <p>A play given now runs a few ticks later, against the battle as it stands then, so the two can
 * differ when the characters around the point move in between.
 *
 * @param tileX the column of the tile under the pointer, which the play is sent from; -1 for {@link
 *     #NONE}
 * @param tileY the row of that tile, from side 0's end; -1 for {@link #NONE}
 * @param placed whether the battle places the play now: false when its placement finds no legal
 *     point for it
 * @param x where the play is placed, along the width in game units, when placed; else 0
 * @param y where the play is placed, along the length in game units, when placed; else 0
 * @param units where each of the play's units is created, in creation order, when placed; empty for
 *     a spell, which places no unit, and when not placed
 */
public record PlacementPreview(
    int tileX, int tileY, boolean placed, float x, float y, List<PlacedUnit> units) {

  /** No preview: no card selected, or the pointer off the arena. */
  public static final PlacementPreview NONE = new PlacementPreview(-1, -1, false, 0, 0, List.of());

  public PlacementPreview {
    units = List.copyOf(units);
  }

  /**
   * One unit of a placed play.
   *
   * @param name the unit's name, as {@link EntityFrame#name} gives it once it stands
   * @param x where it is created, along the width in game units
   * @param y where it is created, along the length in game units
   */
  public record PlacedUnit(String name, float x, float y) {}
}
