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

  /** What {@link #entityAt} answers when it names no entity: the host then picks on its own. */
  int NO_ENTITY = -1;

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

  /**
   * Where the last {@link #present} drew the arena, in the rectangle it was given. The host asks
   * after each {@link #present} and maps the mouse back through it, so that hovering and clicking
   * point at the battle position drawn under the mouse.
   *
   * <p>It is the last refresh's projection, so the mouse follows a change of camera one refresh
   * late: right after the view turns, the first refresh's hover is still mapped as drawn before the
   * turn.
   *
   * <p>By default, null: the host then takes the whole rectangle as the arena, drawn as the
   * built-in visualizer draws it.
   *
   * @return the projection the last {@link #present} drew with, or null
   */
  default ArenaProjection arenaProjection() {
    return null;
  }

  /**
   * The entity the last {@link #present} drew at a pixel, for the host's hover and inspect click.
   * The plugin knows the art it drew, which the host does not: a flyer lifted above its ground
   * point, the upper part of a tall sprite. The host asks the plugin first, and picks on its own,
   * from the battle position under the pixel, when the plugin answers {@link #NO_ENTITY}.
   *
   * <p>The pixel is in the rectangle the last {@link #present} was given, as {@link
   * ArenaProjection} counts it: the column from its left edge, the row from its bottom edge. It may
   * lie outside the art drawn, and before the first {@link #present} nothing has been drawn.
   *
   * <p>By default, {@link #NO_ENTITY}: the host picks as it does for the built-in visualizer.
   *
   * @param pixelX the pixel column, from the rectangle's left edge
   * @param pixelY the pixel row, from the rectangle's bottom edge
   * @return the {@link EntityFrame#id} of an entity of the last tick told, drawn at the pixel, or
   *     {@link #NO_ENTITY} to leave the pick to the host
   */
  default int entityAt(float pixelX, float pixelY) {
    return NO_ENTITY;
  }

  /**
   * What a press or a release of the viewer's main button at a pixel asks of the hand, for a plugin
   * that draws the hand and lets the viewer play from it: select a slot's card, play the selected
   * card at a battle position, or let it go. The host carries the command out as it carries out its
   * own hand buttons and arena clicks, under the same rules and checks, and the plugin never
   * reaches the battle itself. A click is a press then a release at the same pixel; a drag from a
   * card onto the arena is a press on the card and a release on the arena. Which of the two names
   * which command is the plugin's choice.
   *
   * <p>The host asks only where a battle's cards can be played, only for a pixel of the arena's
   * rectangle, and only after a {@link #present}. It asks before acting on its own: a command
   * answered takes the press or release, and the host does nothing else with it; null leaves it to
   * the host, which then acts as it does for the built-in visualizer. A play answered while the
   * host inspects units is not taken: the press inspects, as a click does. What the host has
   * selected is told in the next {@link PresentContext}.
   *
   * <p>The pixel is in the rectangle the last {@link #present} was given, as {@link #entityAt}
   * counts it: the column from its left edge, the row from its bottom edge.
   *
   * <p>By default, null: the host plays from its own hand buttons and arena clicks only.
   *
   * @param pixelX the pixel column, from the rectangle's left edge
   * @param pixelY the pixel row, from the rectangle's bottom edge
   * @param released false for a press of the button, true for its release
   * @return the command, or null to leave the press or release to the host
   */
  default CardCommand cardCommandAt(float pixelX, float pixelY, boolean released) {
    return null;
  }

  /**
   * The viewer's pointer moved, for a plugin that draws the hand and follows a drag from it: where
   * the pointer is now, and whether the main button is held from a press on the arena's rectangle.
   * Between a press {@link #cardCommandAt} is asked for and its release, each move of the pointer
   * comes with {@code held} true, wherever the pointer goes, in or out of the rectangle. The
   * release itself comes with {@code held} false, after {@link #cardCommandAt} is asked for it when
   * it lands in the rectangle, and wherever it lands: it ends the drag. Any other move comes with
   * {@code held} false: with no button held, or with the button held from a press off the
   * rectangle.
   *
   * <p>The host tells it only where a battle's cards can be played, and only after a {@link
   * #present}. It is told as it happens, between refreshes, and asks nothing: what the plugin draws
   * of it shows at the next {@link #present}. The host's own hover and placement preview follow the
   * same pointer ({@link PresentContext#placement}).
   *
   * <p>The pixel is in the rectangle the last {@link #present} was given, as {@link #entityAt}
   * counts it: the column from its left edge, the row from its bottom edge. It may lie outside the
   * rectangle, below 0 or past its width or height.
   *
   * <p>By default, nothing: the plugin follows no pointer.
   *
   * @param pixelX the pixel column, from the rectangle's left edge
   * @param pixelY the pixel row, from the rectangle's bottom edge
   * @param held true while the main button is held from a press on the rectangle; false for the
   *     release that ends it, and for any other move
   */
  default void pointerMoved(float pixelX, float pixelY, boolean held) {}

  /** The plugin will not be called again: its resources can go. */
  default void close() {}
}
