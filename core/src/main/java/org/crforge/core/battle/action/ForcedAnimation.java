/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.core.battle.action;

import lombok.Getter;
import org.crforge.core.fidelity.Fidelity;
import org.crforge.core.fidelity.FidelityStatus;

/**
 * A forced animation's run or stop (ActionRunForcedAnimationOnce, ActionStopForcedAnimation): its
 * perform only tells the owner's view to show a state, so it changes nothing the simulation reads.
 * Its start tells the owner ({@link ActionOwner#forcedAnimation}), which tells the battle's
 * observers, so that a view can show it; it does not last, as an inert row does not.
 */
@Fidelity(
    status = FidelityStatus.TRACED,
    note =
        "Changes nothing the simulation reads, as the inert row it was before: it is scheduled"
            + " and started as any row and keeps no run. Its columns are read as the row gives"
            + " them, 0 for an empty one, and handed to the observers unchanged; how the client's"
            + " view times the state from them is the view's, not modelled here.")
public final class ForcedAnimation extends RowAction {

  /** True for the stop of a forced animation (ActionStopForcedAnimation). */
  @Getter private final boolean stop;

  /** The row's CustomStateNumber: the custom state it forces, 0 for none; 0 for a stop. */
  @Getter private final int customState;

  /** The row's PlaybackDuration in milliseconds, as the row gives it; 0 for a stop. */
  @Getter private final int playbackDurationMs;

  /** The row's ForcedDuration in milliseconds, as the row gives it; 0 for a stop. */
  @Getter private final int forcedDurationMs;

  /**
   * A forced animation's run.
   *
   * @param row the row's shared columns
   * @param customState the row's CustomStateNumber
   * @param playbackDurationMs the row's PlaybackDuration
   * @param forcedDurationMs the row's ForcedDuration
   */
  public ForcedAnimation(
      ActionRow row, int customState, int playbackDurationMs, int forcedDurationMs) {
    this(row, false, customState, playbackDurationMs, forcedDurationMs);
  }

  private ForcedAnimation(
      ActionRow row, boolean stop, int customState, int playbackDurationMs, int forcedDurationMs) {
    super(row);
    this.stop = stop;
    this.customState = customState;
    this.playbackDurationMs = playbackDurationMs;
    this.forcedDurationMs = forcedDurationMs;
  }

  /**
   * A forced animation's stop.
   *
   * @param row the row's shared columns
   */
  public static ForcedAnimation stop(ActionRow row) {
    return new ForcedAnimation(row, true, 0, 0, 0);
  }

  @Override
  public ActionInstance start(ActionHolder holder) {
    if (holder.getOwner() != null) {
      holder.getOwner().forcedAnimation(this);
    }
    return null;
  }
}
