/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.Objects;

/**
 * An int parameter of an entity's animator as the battle last set it, through a row of {@code
 * ActionSetAnimationModifier} that ran on the entity: a view's state machine selects sub-states by
 * it, as a {@code Selector} on {@code PropState} does.
 *
 * @param name the row's {@code Parameter}, the parameter's name, such as {@code PropState}
 * @param value the row's {@code Value}, the value it was set to
 */
public record AnimationParameterFrame(String name, int value) {

  public AnimationParameterFrame {
    Objects.requireNonNull(name, "name");
  }
}
