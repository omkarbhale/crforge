/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * A battle starting on screen.
 *
 * @param arenaWidthUnits the arena's width in game units (1000 per tile)
 * @param arenaHeightUnits the arena's length in game units
 * @param tick the tick it starts at: 0, or later for a step back, whose battle is played again to
 *     the tick before
 */
public record BattleStart(int arenaWidthUnits, int arenaHeightUnits, int tick) {}
