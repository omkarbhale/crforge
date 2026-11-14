/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * A moment of a character's dash in the tick just run, told once.
 *
 * @param id the {@link EntityFrame#id} of the character
 * @param moment what happened
 */
public record DashFrame(int id, DashMoment moment) {

  public DashFrame {
    java.util.Objects.requireNonNull(moment, "moment");
  }
}
