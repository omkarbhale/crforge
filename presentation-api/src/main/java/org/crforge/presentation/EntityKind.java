/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/** What an entity is. */
public enum EntityKind {
  /** A character that is not a building. */
  TROOP,
  /** A building a card placed or a spawn made. */
  BUILDING,
  /** One of the six crown towers. */
  TOWER,
  /** A projectile in flight. */
  PROJECTILE,
  /** A spell's zone, a death's area, an ability's field. */
  AREA_EFFECT
}
