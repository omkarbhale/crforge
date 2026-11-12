/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * Where a character's or crown tower's attack timing stands at a tick, as the battle's own
 * targeting holds it after the tick. All times are milliseconds.
 *
 * <p>The battle times a hit by its attack timer alone: a hit is due on the tick the timer crosses a
 * multiple of the hit speed. The timer steps once per attacking tick, by one tick's 50 ms or by
 * half the hit speed multiplier of the active sequence step, scaled by the buffs that change attack
 * speed; so it already runs at the pace a speed buff gives. Whether a retarget keeps the timer and
 * when it is cleared are the battle's, read here as they are. Whether a due hit is actually dealt
 * is the battle's decision, told by the tick's strikes or projectiles.
 *
 * @param timerMs the attack timer: the attack time elapsed, 0 before an attack starts and once the
 *     battle clears it. An attack that starts from 0 is first credited the part of the wind-up
 *     already run down ({@code loadTimeMs} less {@code loadTimerMs})
 * @param loadTimerMs the load countdown: how much of the wind-up is still to run; every hit reloads
 *     it with the load time
 * @param startDelayMs how long the attack timer is still held at 0 after an attack started on a
 *     sequence step with a start delay; it runs down 50 per attacking tick
 * @param hitSpeedMs the hit speed the battle times this entity's hits by
 * @param loadTimeMs the wind-up the battle holds before a hit
 * @param sequenceIndex the index of the active step of the row's attack sequence, counted from 0 in
 *     the sequence's order, or {@link #NO_SEQUENCE_STEP}; a row without a sequence stays at 0
 */
public record AttackFrame(
    int timerMs,
    int loadTimerMs,
    int startDelayMs,
    int hitSpeedMs,
    int loadTimeMs,
    int sequenceIndex) {

  /** The sequence index of no active step. */
  public static final int NO_SEQUENCE_STEP = -1;

  /**
   * No attack timing: that of a projectile or area effect, and of a frame built without one by the
   * constructors from before it.
   */
  public static final AttackFrame NONE = new AttackFrame(0, 0, 0, 0, 0, NO_SEQUENCE_STEP);
}
