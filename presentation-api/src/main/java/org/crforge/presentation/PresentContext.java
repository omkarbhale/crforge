/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * Where a display refresh draws. The host has set the display up for the arena's rectangle before
 * the call.
 *
 * @param viewportX the rectangle's left edge in window pixels
 * @param viewportY the rectangle's bottom edge in window pixels, counted from the window's bottom
 * @param viewportWidth the rectangle's width in window pixels
 * @param viewportHeight the rectangle's height in window pixels
 * @param bottomSide the side the view has at the bottom, 0 or 1: the two sides' views are half a
 *     turn apart
 * @param selectedSide the side whose card is selected to be played, or {@link #NO_SELECTION}
 * @param selectedSlot the hand slot of the card selected to be played, counted from 0, or {@link
 *     #NO_SELECTION}: selected by the host's hand buttons and keys or by a {@link
 *     CardCommand.Select}
 * @param selectedArea the tiles the selected card lands on without being moved now, for its side
 *     and on the field that side sees: a play sent from any other tile is moved by the battle's
 *     placement search to the nearest legal one, or finds none. {@link DeployArea#NONE} with no
 *     card selected, while the host inspects units, and for a card the host places by no search of
 *     its own
 * @param placement where a play of the selected card from the tile under the pointer would be
 *     placed now ({@link PlacementPreview}); {@link PlacementPreview#NONE} with no card selected,
 *     while the host inspects units, with the pointer off the arena, and for a card the host places
 *     by no placement of its own
 */
public record PresentContext(
    int viewportX,
    int viewportY,
    int viewportWidth,
    int viewportHeight,
    int bottomSide,
    int selectedSide,
    int selectedSlot,
    DeployArea selectedArea,
    PlacementPreview placement) {

  /** The selected side and slot while no card is selected. */
  public static final int NO_SELECTION = -1;

  public PresentContext {
    if (selectedArea == null) {
      selectedArea = DeployArea.NONE;
    }
    if (placement == null) {
      placement = PlacementPreview.NONE;
    }
  }

  /**
   * Where a refresh draws, with the selection and no deploy tiles or placement told. Kept on
   * purpose: crforge-render builds against this module live, and its tests and harness construct
   * contexts, so removing it breaks them.
   */
  public PresentContext(
      int viewportX,
      int viewportY,
      int viewportWidth,
      int viewportHeight,
      int bottomSide,
      int selectedSide,
      int selectedSlot) {
    this(
        viewportX,
        viewportY,
        viewportWidth,
        viewportHeight,
        bottomSide,
        selectedSide,
        selectedSlot,
        DeployArea.NONE,
        PlacementPreview.NONE);
  }

  /** Where a refresh draws, with no card selected. */
  public PresentContext(
      int viewportX, int viewportY, int viewportWidth, int viewportHeight, int bottomSide) {
    this(
        viewportX,
        viewportY,
        viewportWidth,
        viewportHeight,
        bottomSide,
        NO_SELECTION,
        NO_SELECTION);
  }
}
