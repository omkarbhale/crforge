/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/** The form a card was played in. */
public enum CardForm {
  /** The card as it is. */
  BASIC,
  /** Its evolved form: an evolution slot's card played once its count was reached. */
  EVOLVED,
  /** Its hero form: a hero slot's card. */
  HERO
}
