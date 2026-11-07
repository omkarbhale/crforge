/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * One buff on a character or crown tower at a tick.
 *
 * @param name the buff's row name in the game tables
 * @param remainingMs the time left in milliseconds, -1 for a buff that never runs out
 * @param totalMs its whole time in milliseconds, -1 for a buff that never runs out
 * @param side the side it was applied for, which can differ from the entity's; -1 for none
 */
public record StatusFrame(String name, int remainingMs, int totalMs, int side) {}
