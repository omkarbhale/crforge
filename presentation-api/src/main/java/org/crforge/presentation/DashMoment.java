/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/** A moment of a character's dash, as the battle runs it. */
public enum DashMoment {
  /**
   * Its targeting visit, with the reference in dash range, loaded the dash wind-up timer from a
   * timer at or under 0: a wind-up starts. A wind-up that runs from a timer the dash's start
   * reloaded (a row with no dash landing time, whose start reloads the timer to the dash cooldown)
   * is not told.
   */
  WINDUP_STARTED,
  /** The wind-up ran out, or a chain or charge dash began: the dash starts, its state DASHING. */
  STARTED,
  /**
   * The dash ended and landed, its landing hit dealt: on the tick the route follower stops it. A
   * row with a dash landing time stays DASHING through its landing hold after this.
   */
  LANDED
}
