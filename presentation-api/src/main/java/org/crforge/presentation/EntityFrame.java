/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * One entity of a battle at a tick. Positions and distances are in game units (1000 per tile) as
 * the battle holds them: {@code x} along the arena's width, {@code y} along its length from side 0
 * towards side 1.
 *
 * @param id the entity's id in the battle, the same from tick to tick
 * @param kind what it is
 * @param side the battle's side, 0 or 1
 * @param name the row name of the game tables: the unit, building, tower, projectile or area effect
 * @param x position along the width
 * @param y position along the length
 * @param radius a character's collision radius, an area effect's radius, a projectile's area radius
 *     (0 for a projectile without one)
 * @param hitPoints the hit points left, 0 for an entity without hit points
 * @param maxHitPoints the maximum hit points, 0 for an entity without hit points
 * @param shield the shield left, 0 for none
 * @param maxShield the shield's maximum, 0 for an entity that never had one
 * @param air whether it flies
 * @param king whether it is a king tower
 * @param state what a character is doing, {@link EntityState#NONE} for anything else
 * @param deploying whether a character is still deploying or waiting its turn to
 * @param hidden whether a character is hidden or invisible
 * @param hasTarget whether a character holds a target
 * @param targetX the target's position along the width, when it holds one
 * @param targetY the target's position along the length, when it holds one
 * @param headingX the direction a character faces along the width, unscaled; 0 with headingY for
 *     none
 * @param headingY the direction a character faces along the length, unscaled
 * @param aimX a projectile's aim along the width, 0 otherwise
 * @param aimY a projectile's aim along the length, 0 otherwise
 * @param height a projectile's height above the ground, 0 otherwise
 */
public record EntityFrame(
    int id,
    EntityKind kind,
    int side,
    String name,
    int x,
    int y,
    int radius,
    int hitPoints,
    int maxHitPoints,
    int shield,
    int maxShield,
    boolean air,
    boolean king,
    EntityState state,
    boolean deploying,
    boolean hidden,
    boolean hasTarget,
    int targetX,
    int targetY,
    int headingX,
    int headingY,
    int aimX,
    int aimY,
    int height) {

  /** A frame with no height, for code written before {@code height} was added. */
  public EntityFrame(
      int id,
      EntityKind kind,
      int side,
      String name,
      int x,
      int y,
      int radius,
      int hitPoints,
      int maxHitPoints,
      int shield,
      int maxShield,
      boolean air,
      boolean king,
      EntityState state,
      boolean deploying,
      boolean hidden,
      boolean hasTarget,
      int targetX,
      int targetY,
      int headingX,
      int headingY,
      int aimX,
      int aimY) {
    this(
        id,
        kind,
        side,
        name,
        x,
        y,
        radius,
        hitPoints,
        maxHitPoints,
        shield,
        maxShield,
        air,
        king,
        state,
        deploying,
        hidden,
        hasTarget,
        targetX,
        targetY,
        headingX,
        headingY,
        aimX,
        aimY,
        0);
  }
}
