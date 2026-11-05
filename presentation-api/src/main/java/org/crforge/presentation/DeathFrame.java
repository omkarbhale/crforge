/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * A character that died in the tick just run, told once. Positions are in game units.
 *
 * @param id the entity's id in the battle
 * @param name its row name in the game tables
 * @param side its side, 0 or 1
 * @param x its position along the width as it died
 * @param y its position along the length as it died
 * @param cause what ended it
 */
public record DeathFrame(int id, String name, int side, int x, int y, DeathCause cause) {}
