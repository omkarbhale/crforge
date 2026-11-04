/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/** What a character is doing at a tick: the battle's own state, by name. */
public enum EntityState {
  /** Not a character. */
  NONE,
  /** Holding position, and may attack from there. */
  STANDING,
  /** Walking along a route. */
  MOVING,
  /** Attacking a target, not moving. */
  ATTACKING,
  /** Running a dash. */
  DASHING,
  /** Counting down the deploy time after being placed. */
  DEPLOYING,
  /** Following a jump arc. */
  JUMPING,
  /** Routing to a spawn destination. */
  SPAWN_PATHFIND,
  /** Routing to a mid-match destination. */
  INGAME_PATHFIND,
  /** Being set up as a clone. */
  CLONE_SETUP,
  /** Morphing into another character. */
  MORPHING,
  /** Casting an ability. */
  CASTING,
  /** Staggered before deployment starts. */
  WAITING_TO_DEPLOY,
  /** Removed from play while following another entity's position. */
  FOLLOWING_REMOVED,
  /** The building form of {@link #FOLLOWING_REMOVED}. */
  FOLLOWING_REMOVED_BUILDING,
  /** Components disabled. */
  COMPONENTS_DISABLED,
  /** A second route-following state. */
  ROUTE_FOLLOWING_ALTERNATE,
  /** Counting down the follow-up of an ability. */
  ABILITY_FOLLOW_UP
}
