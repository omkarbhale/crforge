/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/** The state of a champion slot's ability button. */
public enum AbilityState {
  /** No state to show: before any champion, or no slot. */
  UNKNOWN,
  /** No live copy of the champion. */
  ABSENT,
  /** The ability may be used. */
  READY,
  /** The champion's play is followed and its copies deploy. */
  DEPLOYING,
  /** The ability is available for a limited time. */
  LIMITED_AVAILABILITY,
  /** No charge is left. */
  ALL_CHARGES_CONSUMED,
  /** A live copy holds the ability pending. */
  PENDING,
  /** The cooldown runs. */
  ON_COOLDOWN,
  /** The king's whole elixir is below the ability's cost. */
  NOT_ENOUGH_ELIXIR,
  /** A live copy casts with its effect still to fire. */
  CASTING,
  /** A live copy carries the tag that disables the ability. */
  DISABLED,
  /** The ability is out of reach for a while. */
  TEMPORARILY_UNAVAILABLE,
  /** The ability is not available yet. */
  NOT_YET_AVAILABLE
}
