/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * Where a plugin drew the arena: the map from a battle position in game units to a pixel of the
 * rectangle {@link PresentContext} gave it. A plugin reports it through {@link
 * PresentationPlugin#arenaProjection} so that the host maps the mouse back to the battle position
 * drawn under it.
 *
 * <p>Each axis has its own scale and offset: a position {@code (x, y)} is drawn at pixel column
 * {@code originX + scaleX * x} and pixel row {@code originY + scaleY * y}, counted from the
 * rectangle's left and bottom edges. A negative scale draws that axis reversed, as one side's view
 * of the arena does. Positions are as {@link EntityFrame} gives them: {@code x} along the arena's
 * width, {@code y} along its length from side 0 towards side 1.
 *
 * @param originX the pixel column, from the rectangle's left edge, where the battle's {@code x} is
 *     0
 * @param originY the pixel row, from the rectangle's bottom edge, where the battle's {@code y} is 0
 * @param scaleX pixels per game unit along the width; negative when the width is drawn reversed
 * @param scaleY pixels per game unit along the length; negative when the length is drawn reversed
 */
public record ArenaProjection(float originX, float originY, float scaleX, float scaleY) {

  /** Refuses an origin that is not finite and a scale that is zero or not finite. */
  public ArenaProjection {
    if (!Float.isFinite(originX) || !Float.isFinite(originY)) {
      throw new IllegalArgumentException(
          "an arena projection's origin is finite, not " + originX + ", " + originY);
    }
    if (!Float.isFinite(scaleX) || !Float.isFinite(scaleY) || scaleX == 0 || scaleY == 0) {
      throw new IllegalArgumentException(
          "an arena projection's scales are finite and not zero, not " + scaleX + ", " + scaleY);
    }
  }

  /** The pixel column a position along the width is drawn at. */
  public float pixelX(float x) {
    return originX + scaleX * x;
  }

  /** The pixel row a position along the length is drawn at. */
  public float pixelY(float y) {
    return originY + scaleY * y;
  }

  /** The position along the width, in game units, drawn at a pixel column. */
  public float x(float pixelX) {
    return (pixelX - originX) / scaleX;
  }

  /** The position along the length, in game units, drawn at a pixel row. */
  public float y(float pixelY) {
    return (pixelY - originY) / scaleY;
  }
}
