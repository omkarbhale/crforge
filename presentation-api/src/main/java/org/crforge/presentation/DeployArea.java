/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;

/**
 * Tiles of the arena a side may put a card down on, one flag per 1000-unit tile: a grid as wide and
 * as long as the arena, in the battle's own columns and rows. A tile's column counts along the
 * width from {@code x} 0 and its row along the length from side 0's end ({@code y} 0), as {@link
 * EntityFrame} positions are counted: the tile of column {@code c} and row {@code r} spans {@code
 * x} from {@code 1000 c} to {@code 1000 (c + 1)} and {@code y} from {@code 1000 r} to {@code 1000
 * (r + 1)}.
 *
 * <p>What makes a tile open depends on where the area is given: a side's deploy area ({@link
 * SideFrame#deployArea}) or the tiles the selected card lands on ({@link
 * PresentContext#selectedArea}).
 *
 * @param side the side that puts the card down, 0 or 1; -1 for {@link #NONE}
 * @param widthTiles the tiles across the arena's width
 * @param heightTiles the tiles along the arena's length
 * @param tiles one flag per tile, row after row from row 0, each row from column 0: true for open
 */
public record DeployArea(int side, int widthTiles, int heightTiles, List<Boolean> tiles) {

  /** No area told: no tiles at all. */
  public static final DeployArea NONE = new DeployArea(-1, 0, 0, List.of());

  /** Refuses a grid whose flags do not fill it, row after row. */
  public DeployArea {
    if (widthTiles < 0 || heightTiles < 0) {
      throw new IllegalArgumentException(
          "a deploy area's size is not negative, not " + widthTiles + " by " + heightTiles);
    }
    tiles = List.copyOf(tiles);
    if (tiles.size() != widthTiles * heightTiles) {
      throw new IllegalArgumentException(
          "a deploy area of "
              + widthTiles
              + " by "
              + heightTiles
              + " tiles has as many flags, not "
              + tiles.size());
    }
  }

  /**
   * Whether a tile is open; false for a tile off the grid.
   *
   * @param column the tile's column, from {@code x} 0
   * @param row the tile's row, from side 0's end
   */
  public boolean open(int column, int row) {
    return column >= 0
        && row >= 0
        && column < widthTiles
        && row < heightTiles
        && tiles.get(row * widthTiles + column);
  }

  /**
   * Whether the tile holding a battle position is open; false off the grid.
   *
   * @param x the position along the width, in game units
   * @param y the position along the length, in game units
   */
  public boolean openAt(float x, float y) {
    return Float.isFinite(x)
        && Float.isFinite(y)
        && open((int) Math.floor(x / 1000f), (int) Math.floor(y / 1000f));
  }
}
