/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/** What ended a character. A morph and a plain removal end none: they are no deaths. */
public enum DeathCause {
  /** A hit, a spell, a buff or a rule of the match took its hit points. */
  KILLED,
  /** It killed itself, as a unit whose hit ends it does. */
  KAMIKAZE,
  /** Its lifetime ran out and took its last hit point. */
  DECAY,
  /** An object without hit points ended by its own course, as a bomb does as its deploy ends. */
  EXPIRED,
  /** The parent it rode on left the battle. */
  PARENT_LEFT,
  /** A limited spawner spent its spawns and its row destroys it at that limit. */
  LIMIT
}
