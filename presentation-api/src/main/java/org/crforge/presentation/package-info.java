/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

/**
 * The presentation API: what a plugin that draws a battle is given, and when. It is engine-agnostic
 * and read-only by construction.
 *
 * <p>A plugin ({@link org.crforge.presentation.PresentationPlugin}) is found with {@link
 * java.util.ServiceLoader}. With none on the classpath, the visualizer draws as it always has.
 *
 * <h2>Read-only by construction</h2>
 *
 * <p>Everything a plugin receives is a record of primitives, strings, enums and unmodifiable lists
 * of the same, built for it and detached from the battle. No type of the battle core or of a
 * graphics engine appears here, and this module depends on neither. In particular, a plugin is not
 * given:
 *
 * <ul>
 *   <li>a unit's live grid state ({@code GridUnitState}): writable movement and targeting state
 *   <li>the battle's world ({@code BattleWorld}): writable routing grid, and the battle's one
 *       random source
 *   <li>the world's observer callbacks ({@code WorldObserver}): live entities, mid-tick, with an
 *       exception halting the battle
 * </ul>
 *
 * <h2>Tick-driven</h2>
 *
 * <p>A plugin hears one {@link org.crforge.presentation.TickFrame} for each tick the battle runs,
 * however many run between two display refreshes, and carries no wall clock. {@link
 * org.crforge.presentation.PresentationPlugin#present} asks it to show what it has been told so
 * far.
 *
 * <h2>Deferred</h2>
 *
 * <p>This is a thin first slice, enough for one renderer to draw a unit fight. These are exposable
 * without changing the battle, and are added when a renderer needs them:
 *
 * <ul>
 *   <li>TODO per-tick character fields: attack, load and start delay timers, sequence index, burst
 *       and special load progress, attack count, deploy countdown, height, target id, whether a
 *       king tower is inactive
 *   <li>TODO per-tick projectile fields: start, aim height, facing, target id
 *   <li>TODO match fields: section, section left, frozen, end timer, tiebreak time, next card
 *       cooldown, evolution count, a king's champion slots
 *   <li>TODO statuses, action meters and roles, which the frames the visualizer draws already hold
 *   <li>TODO value events: attack started and fired, died (with a final state for an entity that
 *       leaves during the tick), card played, action started and finished
 *   <li>TODO a static battle header: cosmetics, emotes, profiles
 *   <li>TODO audio output
 * </ul>
 */
package org.crforge.presentation;
