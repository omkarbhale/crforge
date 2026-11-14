/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * The last forced animation the battle ran on an entity (its ActionRunForcedAnimationOnce row), as
 * the row gives it. The battle changes nothing by it and does not time it: when the state it forces
 * shows and ends, by its durations, is the view's rule. It is held from the run until a forced
 * animation's stop (ActionStopForcedAnimation) runs on the entity, a later run replaces it, or the
 * entity leaves.
 *
 * @param action the row's name; empty for {@link #NONE}
 * @param customState the row's CustomStateNumber: the custom state it forces, 0 when the row gives
 *     none
 * @param startTick the {@link TickFrame#tick} of the first frame that told the run: the run started
 *     in the tick just before it; -1 for {@link #NONE}
 * @param playbackDurationMs the row's PlaybackDuration in milliseconds, as the row gives it (some
 *     rows give -1); 0 when it gives none
 * @param forcedDurationMs the row's ForcedDuration in milliseconds, as the row gives it; 0 when it
 *     gives none
 */
public record ForcedAnimationFrame(
    String action, int customState, int startTick, int playbackDurationMs, int forcedDurationMs) {

  /** No forced animation: none ran on the entity, or its stop ran since. */
  public static final ForcedAnimationFrame NONE = new ForcedAnimationFrame("", 0, -1, 0, 0);

  public ForcedAnimationFrame {
    java.util.Objects.requireNonNull(action, "action; empty stands for none");
  }
}
