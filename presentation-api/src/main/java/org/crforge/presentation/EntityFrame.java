/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;
import java.util.Objects;

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
 * @param statuses the buffs on a character or crown tower now, empty for anything else
 * @param isClone whether a character is a clone, for its whole life; false for anything else
 * @param level the level the battle holds the entity at, counted from 1 across all rarities as the
 *     match counts a card's level: a troop's, building's or tower's own level, which a level change
 *     moves, and a projectile's or area effect's the level it was made at; {@link #NO_LEVEL} when
 *     the frame was built without one
 * @param attack where a character's or crown tower's attack timing stands: its attack timer, load
 *     and start delay timers, the hit speed and load time they are counted against, and the index
 *     of the active attack sequence step; {@link AttackFrame#NONE} for a projectile or area effect
 *     and when the frame was built without one
 * @param chargeProgress a character's charge progress as its movement component holds it after the
 *     tick: {@link #NO_CHARGE} (-1) while no charge is in progress (and for anything else), else 0
 *     rising toward {@link #CHARGE_COMPLETE} (10000) as it walks, the charge complete at 10000 or
 *     more
 * @param charging whether the battle's CHARGING tag bit is set on a character after the tick. A
 *     step that leaves the charge progress at {@link #CHARGE_COMPLETE} or more raises it, folded in
 *     from the step's pending tags, so it trails the progress by a tick: it shows from the tick
 *     after the charge completes and stays the tick after the charge resets. False for anything
 *     else
 * @param forcedAnimation the last forced animation the battle ran on the entity, held until its
 *     stop runs, a later run replaces it or the entity leaves; {@link ForcedAnimationFrame#NONE}
 *     for none and when the frame was built without one
 * @param carrierId the id of the character this one rides on, as a spawner whose row attaches its
 *     children ({@code SpawnAttach}) makes its riders: the Spear Goblins on a Goblin Giant, the
 *     rider on a Ram. Held from the spawn's attach until the carrier leaves, which lets the rider
 *     go. A rider carries no riders of its own in the battle core, so a carrier carries none.
 *     {@link #NO_CARRIER} for a character that rides on nothing, for anything else and when the
 *     frame was built without one
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
    int height,
    List<StatusFrame> statuses,
    boolean isClone,
    int level,
    AttackFrame attack,
    int chargeProgress,
    boolean charging,
    ForcedAnimationFrame forcedAnimation,
    int carrierId) {

  /**
   * The carrier id of an entity that rides on nothing, and of a frame built without one. No entity
   * of a battle has this id.
   */
  public static final int NO_CARRIER = -1;

  /**
   * The charge progress of an entity with no charge in progress, and of a frame built without one.
   */
  public static final int NO_CHARGE = -1;

  /** The charge progress at which a charge is complete. */
  public static final int CHARGE_COMPLETE = 10000;

  /**
   * The level of a frame built without one: by the constructors from before the level. Every entity
   * of a battle stands at a level counted from 1, so this is never a battle's value.
   */
  public static final int NO_LEVEL = 0;

  public EntityFrame {
    statuses = List.copyOf(statuses);
    Objects.requireNonNull(attack, "attack; AttackFrame.NONE stands for none");
    Objects.requireNonNull(
        forcedAnimation, "forcedAnimation; ForcedAnimationFrame.NONE stands for none");
  }

  /**
   * A frame of an entity that rides on nothing ({@link #NO_CARRIER}). Kept on purpose:
   * crforge-render builds against this module live, and its tests and harness tools construct
   * frames with this signature, so removing it breaks them.
   */
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
      int aimY,
      int height,
      List<StatusFrame> statuses,
      boolean isClone,
      int level,
      AttackFrame attack,
      int chargeProgress,
      boolean charging,
      ForcedAnimationFrame forcedAnimation) {
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
        height,
        statuses,
        isClone,
        level,
        attack,
        chargeProgress,
        charging,
        forcedAnimation,
        NO_CARRIER);
  }

  /**
   * A frame with no charge ({@link #NO_CHARGE}, not charging), no forced animation ({@link
   * ForcedAnimationFrame#NONE}) and no carrier ({@link #NO_CARRIER}). Kept on purpose:
   * crforge-render builds against this module live, and its tests and harness tools construct
   * frames with this signature, so removing it breaks them.
   */
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
      int aimY,
      int height,
      List<StatusFrame> statuses,
      boolean isClone,
      int level,
      AttackFrame attack) {
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
        height,
        statuses,
        isClone,
        level,
        attack,
        NO_CHARGE,
        false,
        ForcedAnimationFrame.NONE,
        NO_CARRIER);
  }

  /**
   * A frame with no attack timing ({@link AttackFrame#NONE}). Kept on purpose: crforge-render
   * builds against this module live, and its tests and harness tools construct frames with this
   * signature, so removing it breaks them.
   */
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
      int aimY,
      int height,
      List<StatusFrame> statuses,
      boolean isClone,
      int level) {
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
        height,
        statuses,
        isClone,
        level,
        AttackFrame.NONE,
        NO_CHARGE,
        false,
        ForcedAnimationFrame.NONE,
        NO_CARRIER);
  }

  /**
   * A frame with no level ({@link #NO_LEVEL}) and no attack timing ({@link AttackFrame#NONE}). Kept
   * on purpose: crforge-render builds against this module live, and its tests and harness tools
   * construct frames with this signature, so removing it breaks them.
   */
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
      int aimY,
      int height,
      List<StatusFrame> statuses,
      boolean isClone) {
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
        height,
        statuses,
        isClone,
        NO_LEVEL,
        AttackFrame.NONE,
        NO_CHARGE,
        false,
        ForcedAnimationFrame.NONE,
        NO_CARRIER);
  }

  /**
   * A frame with no statuses, of no clone, with no level ({@link #NO_LEVEL}) and no attack timing
   * ({@link AttackFrame#NONE}). Kept on purpose: crforge-render builds against this module live,
   * and its tests and harness tools construct frames with this signature, so removing it breaks
   * them.
   */
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
      int aimY,
      int height) {
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
        height,
        List.of(),
        false,
        NO_LEVEL,
        AttackFrame.NONE,
        NO_CHARGE,
        false,
        ForcedAnimationFrame.NONE,
        NO_CARRIER);
  }
}
