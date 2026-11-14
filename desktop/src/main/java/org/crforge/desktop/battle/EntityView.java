/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.battle;

import java.util.List;
import org.crforge.core.pathfinding.GridUnitState;

/**
 * One battle core entity as the renderers draw it, read by {@link BattleAdapter} after a step. All
 * positions and distances are in game units (1000 per tile), as the battle holds them.
 *
 * @param id the entity's id in the battle
 * @param kind what the renderers draw it as
 * @param side the battle's side, 0 or 1; the screen's {@code ViewOrientation} decides which is
 *     drawn blue at the bottom
 * @param name the row name: the unit, building, tower, projectile or area effect row
 * @param level the level it stands at, counted from 1 across all rarities as the match counts a
 *     card's level: a character's own level, which a level change moves, a projectile's or an area
 *     effect's the level it was made at
 * @param x position along the arena's width
 * @param y position along the arena's length
 * @param radius the body's radius: a character's collision radius, an area effect's radius, a
 *     projectile's area radius (0 for a projectile without one)
 * @param hitPoints the hit points left, 0 for an entity without hit points
 * @param maxHitPoints the maximum hit points, 0 for an entity without hit points
 * @param shield the shield left, 0 for none
 * @param maxShield the shield's maximum, 0 for an entity that never had one
 * @param air whether it flies
 * @param king whether it is a king tower
 * @param state the entity state number of a character (see {@code GridEntityState}), -1 otherwise
 * @param deploying whether a character is still deploying or waiting its turn to
 * @param hidden whether a character is hidden or invisible, which most hits pass by
 * @param range a character's attack range, 0 otherwise
 * @param minimumRange a character's minimum range, 0 for none
 * @param sightRange a character's sight range, 0 otherwise
 * @param hasTarget whether a character holds a reference (its target)
 * @param targetX the reference's position along the width, when it holds one
 * @param targetY the reference's position along the length, when it holds one
 * @param headingX a character's direction of travel along the width, unscaled; 0 with headingY for
 *     none
 * @param headingY a character's direction of travel along the length, unscaled
 * @param lifeMs an area effect's life left in milliseconds, 0 otherwise
 * @param lifetimeMs an area effect's whole lifetime in milliseconds, 0 otherwise
 * @param aimX a projectile's aim along the width, 0 otherwise
 * @param aimY a projectile's aim along the length, 0 otherwise
 * @param radiusY the half height along the arena's length of a projectile's area when its row makes
 *     it a box (its RadiusY), the radius then its half width across the arena; 0 for a circle and
 *     for anything but a projectile
 * @param bodyRadius the half width across the arena of a projectile's flying body when its row
 *     makes the body a box (its ProjectileRadius); 0 otherwise
 * @param bodyRadiusY the half height along the arena's length of that box (its ProjectileRadiusY);
 *     0 for a body that is a circle, a projectile without a body and anything but a projectile
 * @param height a projectile's height above the ground, 0 otherwise
 * @param speed the distance a character's movement visit asked for in the last tick
 * @param grid a character's grid state, for the route overlay; null otherwise
 * @param meter the bar an action running on a character shows over it, such as the Royal Chef's
 *     cooking or the Dagger Duchess's charges; null for none
 * @param statuses active buff snapshots and persistent clone identity; empty for non-characters
 * @param role what a troop or building does in a fight, read from its row, for its role icon;
 *     {@link UnitRole#NONE} for towers and non-characters
 * @param bar what a character's bar shows: its health, a timer, or nothing (see {@link Bar})
 * @param attack a character's attack timing as its targeting holds it, copied after the step;
 *     {@link Attack#NONE} otherwise
 * @param charge a character's charge as its movement component and tags hold it, copied after the
 *     step; {@link Charge#NONE} otherwise
 */
public record EntityView(
    int id,
    Kind kind,
    int side,
    String name,
    int level,
    int x,
    int y,
    int radius,
    int hitPoints,
    int maxHitPoints,
    int shield,
    int maxShield,
    boolean air,
    boolean king,
    int state,
    boolean deploying,
    boolean hidden,
    int range,
    int minimumRange,
    int sightRange,
    boolean hasTarget,
    int targetX,
    int targetY,
    int headingX,
    int headingY,
    int lifeMs,
    int lifetimeMs,
    int aimX,
    int aimY,
    int radiusY,
    int bodyRadius,
    int bodyRadiusY,
    int height,
    int speed,
    GridUnitState grid,
    ActionMeter meter,
    List<UnitStatus> statuses,
    UnitRole role,
    Bar bar,
    Attack attack,
    Charge charge) {

  public EntityView {
    statuses = List.copyOf(statuses);
  }

  /**
   * A character's attack timing, copied from its targeting component and columns. All times are
   * milliseconds.
   *
   * @param timerMs the attack timer: the attack time elapsed, 0 while no attack runs
   * @param loadTimerMs the load countdown: how much of the wind-up is still to run
   * @param startDelayMs how long the attack timer is still held at 0 by a sequence step's start
   *     delay
   * @param hitSpeedMs the hit speed the timer is counted against
   * @param loadTimeMs the wind-up the battle holds before a hit
   * @param sequenceIndex the index of the active attack sequence step, or -1 for none
   */
  public record Attack(
      int timerMs,
      int loadTimerMs,
      int startDelayMs,
      int hitSpeedMs,
      int loadTimeMs,
      int sequenceIndex) {

    /** No attack timing: a projectile's or area effect's. */
    public static final Attack NONE = new Attack(0, 0, 0, 0, 0, -1);
  }

  /**
   * A character's charge.
   *
   * @param progress the movement component's charge progress: -1 while no charge is in progress or
   *     its movement component is off, else 0 rising toward 10000, complete at 10000 or more
   * @param charging whether its CHARGING tag bit is set
   */
  public record Charge(int progress, boolean charging) {

    /** No charge: anything but a character, or a character with none in progress. */
    public static final Charge NONE = new Charge(-1, false);
  }

  public boolean hasStatus(UnitStatus.Kind kind) {
    return statuses.stream().anyMatch(status -> status.kind() == kind);
  }

  /** What an entity is drawn as. */
  /** What a character's bar shows. */
  public enum Bar {
    /** Its hit points: the usual health bar, with the numbers when they are shown. */
    HEALTH,
    /**
     * Its time left: a unit that cannot be targeted and has a limited lifetime, whose hit points
     * only run down over that lifetime, such as the unit that holds the Tombstone hero's ability.
     * {@link #lifeMs} and {@link #lifetimeMs} carry the time left and the whole lifetime.
     */
    TIMER,
    /**
     * Nothing: its row hides the default bar and starts no client action that draws its own, such
     * as a decoration standing on another unit.
     */
    NONE
  }

  /** Whether the character's bar is not its health: a timer or nothing. */
  public boolean healthBarHidden() {
    return bar != Bar.HEALTH;
  }

  public enum Kind {
    /** A troop: a character that is not a building. */
    TROOP,
    /** A building a card placed or a spawn made: a character of a building row. */
    BUILDING,
    /** One of the six crown towers. */
    TOWER,
    /** A projectile in flight. */
    PROJECTILE,
    /** An area effect: a spell's zone, a death's area, an ability's field. */
    AREA_EFFECT
  }

  /** Whether the entity has hit points at all. */
  public boolean hasHitPoints() {
    return maxHitPoints > 0;
  }

  /** The share of its hit points left, 0 to 1; 0 for an entity without hit points. */
  public float healthShare() {
    return maxHitPoints > 0 ? (float) hitPoints / maxHitPoints : 0f;
  }

  /** Whether it is a character: a troop, a building or a tower. */
  public boolean isCharacter() {
    return kind == Kind.TROOP || kind == Kind.BUILDING || kind == Kind.TOWER;
  }
}
