/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import java.util.ArrayList;
import java.util.List;
import org.crforge.core.battle.deploy.CardPlacement;
import org.crforge.core.battle.deploy.DeployMask;
import org.crforge.core.battle.deploy.MaskEntity;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.crforge.core.pathfinding.grid.TileMap;
import org.crforge.desktop.battle.LandingTiles;
import org.crforge.presentation.DeployArea;
import org.crforge.presentation.PlacementPreview;

/**
 * The deploy tiles and the placement preview a plugin is told, from the battle core's deploy mask,
 * the deploy overlay's landing tiles and the battle's placement preview.
 *
 * <p>A side's deploy area is the deploy mask's tower boxes alone: the mask built for a card of no
 * row limits, that may not be placed on the other side or on buildings, from the other side's
 * standing towers only ({@link DeployMask#build}). A box a tower keeps free closes its tiles; once
 * the tower falls it closes none, so a fallen princess tower opens its pocket. The area is kept per
 * side until the towers it was built from change.
 */
public final class DeployAreas {

  /** The neutral side, which keeps no tiles from either side. */
  private static final int NEUTRAL = 100;

  private final Standard1v1Battle battle;

  /** Per side: the towers its area was last built from. */
  private final List<List<MaskEntity>> towers = new ArrayList<>(List.of(List.of(), List.of()));

  /** Per side: the area last built, or null for none yet. */
  private final DeployArea[] areas = new DeployArea[2];

  DeployAreas(Standard1v1Battle battle) {
    this.battle = battle;
  }

  /** Each side's deploy area now, side 0 then side 1. */
  List<DeployArea> sides() {
    List<MaskEntity> all = battle.placementEntities(w -> true);
    return List.of(side(0, all), side(1, all));
  }

  private DeployArea side(int side, List<MaskEntity> all) {
    List<MaskEntity> enemyTowers = new ArrayList<>();
    for (MaskEntity e : all) {
      if (DeployMask.reads(e)
          && e.noDeploySizeW() >= 1
          && e.side() != NEUTRAL
          && (e.side() & 1) != (side & 1)) {
        enemyTowers.add(e);
      }
    }
    if (areas[side] != null && enemyTowers.equals(towers.get(side))) {
      return areas[side];
    }
    TileMap map = battle.getWorld().getTileMap();
    int width = map.width() >> 1;
    int height = map.height() >> 1;
    boolean[] open =
        DeployMask.build(
            width, height, enemyTowers, (side & 1) == 0, false, false, false, false, 0, 0);
    towers.set(side, enemyTowers);
    areas[side] = area(side, width, height, open);
    return areas[side];
  }

  /** The deploy overlay's landing tiles of a side's card as a plugin's area; NONE for none. */
  public static DeployArea of(int side, LandingTiles.Tiles tiles) {
    return tiles == null
        ? DeployArea.NONE
        : area(side, tiles.width(), tiles.height(), tiles.flags());
  }

  /**
   * The battle's placement preview of a play sent from a tile as a plugin's; NONE for no preview.
   */
  public static PlacementPreview of(int tileX, int tileY, CardPlacement.Result result) {
    if (result == null) {
      return PlacementPreview.NONE;
    }
    if (!result.placed()) {
      return new PlacementPreview(tileX, tileY, false, 0, 0, List.of());
    }
    List<PlacementPreview.PlacedUnit> units = new ArrayList<>(result.units().size());
    for (CardPlacement.Unit unit : result.units()) {
      units.add(new PlacementPreview.PlacedUnit(unit.unit().name(), unit.x(), unit.y()));
    }
    return new PlacementPreview(tileX, tileY, true, result.x(), result.y(), units);
  }

  private static DeployArea area(int side, int width, int height, boolean[] open) {
    List<Boolean> tiles = new ArrayList<>(open.length);
    for (boolean flag : open) {
      tiles.add(flag);
    }
    return new DeployArea(side, width, height, tiles);
  }
}
