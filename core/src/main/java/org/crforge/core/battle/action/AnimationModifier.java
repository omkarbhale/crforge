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
 * An animation modifier's set (ActionSetAnimationModifier): its perform only hands the owner's
 * animator an int parameter's value, such as PropState, which a view's state machine selects its
 * sub-states by, so it changes nothing the simulation reads. Its start tells the owner ({@link
 * ActionOwner#animationModifier}), which tells the battle's observers, so that a view can hold the
 * value; it does not last, as the inert row it was before did not.
 */
@Fidelity(
    status = FidelityStatus.TRACED,
    note =
        "Changes nothing the simulation reads, as the inert row it was before: it is scheduled"
            + " and started as any row and keeps no run. Its Parameter and Value are read as the"
            + " row gives them, 0 for an empty Value, and handed to the observers unchanged; the"
            + " animator that holds the value and the states that select on it are the view's, not"
            + " modelled here.")
public final class AnimationModifier extends RowAction {

  /** The row's Parameter: the name of the animator's int parameter it sets. */
  @Getter private final String parameter;

  /** The row's Value: the value it sets the parameter to. */
  @Getter private final int value;

  /**
   * An animation modifier's set.
   *
   * @param row the row's shared columns
   * @param parameter the row's Parameter
   * @param value the row's Value
   */
  public AnimationModifier(ActionRow row, String parameter, int value) {
    super(row);
    this.parameter = parameter;
    this.value = value;
  }

  @Override
  public ActionInstance start(ActionHolder holder) {
    if (holder.getOwner() != null) {
      holder.getOwner().animationModifier(this);
    }
    return null;
  }
}
