/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * Draws a battle from what the host tells it. An implementation has a public no-argument
 * constructor, is named in {@code META-INF/services/org.crforge.presentation.PresentationPlugin}
 * and is on the classpath; see {@link java.util.ServiceLoader}.
 *
 * <p>The host calls a plugin on one thread, the one that owns the display: {@link #battleStarted}
 * before anything else of a battle, then {@link #tick} and {@link #present} as the battle runs and
 * the display refreshes, and {@link #close} last. An exception out of any call never reaches the
 * battle: the host logs it, closes the plugin and goes back to the built-in visualizer.
 */
public interface PresentationPlugin {

  /** A short name for the plugin, for the log. The lowest wins when several are found. */
  String id();

  /**
   * A battle starts on screen, replacing the last one: the first battle, a restart, a step back, a
   * data version switch or another replay opened. Everything kept from an earlier battle is stale,
   * since ids start again. The ticks the host ran to reach {@link BattleStart#tick} are not told.
   *
   * @param battle the new battle
   */
  void battleStarted(BattleStart battle);

  /**
   * The state of the battle at a tick: first the tick of {@link #battleStarted}, then each tick the
   * battle runs, in order and without gaps until the next {@link #battleStarted}. No call comes
   * while the battle is paused or stopped.
   *
   * @param frame the battle's state; it stays as it is, so it may be kept
   */
  void tick(TickFrame frame);

  /**
   * The display refreshes: show what the ticks told so far. One tick may be presented many times,
   * or none. The time between refreshes is not given, so that what is shown follows the ticks.
   *
   * @param context where to draw, and how the arena is turned
   */
  void present(PresentContext context);

  /** The plugin will not be called again: its resources can go. */
  default void close() {}
}
