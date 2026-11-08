/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * Where a display refresh draws. The host has set the display up for the arena's rectangle before
 * the call.
 *
 * @param viewportX the rectangle's left edge in window pixels
 * @param viewportY the rectangle's bottom edge in window pixels, counted from the window's bottom
 * @param viewportWidth the rectangle's width in window pixels
 * @param viewportHeight the rectangle's height in window pixels
 * @param bottomSide the side the view has at the bottom, 0 or 1: the two sides' views are half a
 *     turn apart
 */
public record PresentContext(
    int viewportX, int viewportY, int viewportWidth, int viewportHeight, int bottomSide) {}
