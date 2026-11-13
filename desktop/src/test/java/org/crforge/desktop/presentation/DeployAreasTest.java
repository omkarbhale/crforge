/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.crforge.core.battle.BattleEntity;
import org.crforge.core.battle.GameData;
import org.crforge.core.battle.deploy.CardPlacement;
import org.crforge.core.battle.unit.TowerEntity;
import org.crforge.core.pathfinding.grid.TileMap;
import org.crforge.core.util.GameUnits;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.battle.LandingTiles;
import org.crforge.presentation.DeployArea;
import org.crforge.presentation.PlacementPreview;
import org.crforge.presentation.SideFrame;
import org.crforge.presentation.TickFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Each side's deploy area a plugin is told, and the selected card's tiles and placement as it is
 * told them, read from the battle core's deploy mask, landing tiles and placement preview.
 */
class DeployAreasTest {

  @Test
  @DisplayName(
      "each side may deploy on its own half at the start, and a fallen princess tower opens its"
          + " pocket to the other side")
  void fallenTowerOpensPocket() {
    BattleSession session = only("Knight");
    TileMap map = session.getBattle().getWorld().getTileMap();
    int cols = map.widthUnits() / GameUnits.UNITS_PER_TILE;
    int rows = map.heightUnits() / GameUnits.UNITS_PER_TILE;
    DeployAreas areas = new DeployAreas(session.getBattle());
    List<DeployArea> before = areas.sides();
    assertThat(before).hasSize(2);
    for (int side = 0; side < 2; side++) {
      DeployArea area = before.get(side);
      assertThat(area.side()).isEqualTo(side);
      assertThat(area.widthTiles()).isEqualTo(cols);
      assertThat(area.heightTiles()).isEqualTo(rows);
      assertThat(open(area, ownHalf(side, rows))).as("side %d's own half", side).isPositive();
      assertThat(open(area, ownHalf(1 - side, rows))).as("side %d, the other half", side).isZero();
    }
    // The two sides' areas are each other's, half a turn apart.
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        assertThat(before.get(0).open(col, row))
            .as("tile (%d, %d)", col, row)
            .isEqualTo(before.get(1).open(cols - 1 - col, rows - 1 - row));
      }
    }
    assertThat(session.step()).isTrue();
    List<DeployArea> kept = areas.sides();
    assertThat(kept.get(0)).as("kept while the towers stand").isSameAs(before.get(0));
    assertThat(kept.get(1)).isSameAs(before.get(1));

    for (BattleEntity entity : session.getBattle().getBattle().getHolder().entities()) {
      if (entity instanceof TowerEntity tower
          && tower.side() == 1
          && tower.getData().summonerTower()) {
        tower.getHitPoints().setHitPoints(0);
      }
    }
    for (int i = 0; i < 5; i++) {
      assertThat(session.step()).isTrue();
    }

    List<DeployArea> after = areas.sides();
    assertThat(open(after.get(0), ownHalf(1, rows))).as("side 0's pockets").isPositive();
    assertThat(after.get(1)).as("side 1's towers did not fall to side 1").isEqualTo(before.get(1));
    // Each tile the Knight lands on unmoved, before and after, lies in its side's area.
    LandingTiles.Tiles knight = session.landingTiles(0, 0);
    int pocket = 0;
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        if (knight.open(col, row)) {
          assertThat(after.get(0).open(col, row)).as("tile (%d, %d)", col, row).isTrue();
          pocket += row >= rows / 2 ? 1 : 0;
        }
      }
    }
    assertThat(pocket).as("the Knight lands in a pocket").isPositive();
  }

  @Test
  @DisplayName("a plugin's tick frame tells each side's deploy area by side")
  void frameTellsAreas() {
    BattleSession session = only("Knight");
    List<DeployArea> areas = new DeployAreas(session.getBattle()).sides();
    TickFrame frame =
        TickFrames.of(BattleAdapter.frame(session), List.of(), List.of(), List.of(), areas);
    assertThat(frame.sides()).extracting(SideFrame::deployArea).containsExactlyElementsOf(areas);
    TickFrame without = TickFrames.of(BattleAdapter.frame(session), List.of(), List.of());
    assertThat(without.sides()).extracting(SideFrame::deployArea).containsOnly(DeployArea.NONE);
  }

  @Test
  @DisplayName(
      "the selected card's tiles and placement are the landing tiles and the battle's preview")
  void selectedTilesAndPlacement() {
    BattleSession session = only("Knight");
    LandingTiles.Tiles tiles = session.landingTiles(0, 0);
    DeployArea area = DeployAreas.of(0, tiles);
    assertThat(area.side()).isZero();
    for (int row = 0; row < tiles.height(); row++) {
      for (int col = 0; col < tiles.width(); col++) {
        assertThat(area.open(col, row)).isEqualTo(tiles.open(col, row));
      }
    }
    assertThat(DeployAreas.of(0, null)).isSameAs(DeployArea.NONE);

    int x = GameUnits.tileCenter(4);
    int y = GameUnits.tileCenter(10);
    CardPlacement.Result result = session.preview(0, 0, x, y);
    PlacementPreview preview = DeployAreas.of(4, 10, result);
    assertThat(preview.placed()).isTrue();
    assertThat(preview.tileX()).isEqualTo(4);
    assertThat(preview.tileY()).isEqualTo(10);
    assertThat(preview.x()).isEqualTo(result.x());
    assertThat(preview.y()).isEqualTo(result.y());
    assertThat(preview.units()).hasSameSizeAs(result.units()).isNotEmpty();
    assertThat(preview.units().get(0).name()).isEqualTo("Knight");
    assertThat(preview.units().get(0).x()).isEqualTo(result.units().get(0).x());
    assertThat(DeployAreas.of(4, 10, null)).isSameAs(PlacementPreview.NONE);
    PlacementPreview refused =
        DeployAreas.of(
            4, 10, new CardPlacement.Result(CardPlacement.NO_POSITION, 0, 0, null, 0, List.of()));
    assertThat(refused.placed()).isFalse();
    assertThat(refused.tileX()).isEqualTo(4);
    assertThat(refused.units()).isEmpty();
  }

  /** The rows of a side's half: side 0's from row 0. */
  private static int[] ownHalf(int side, int rows) {
    return side == 0 ? new int[] {0, rows / 2} : new int[] {rows / 2, rows};
  }

  /** How many tiles of a run of rows are open. */
  private static int open(DeployArea area, int[] rows) {
    int count = 0;
    for (int row = rows[0]; row < rows[1]; row++) {
      for (int col = 0; col < area.widthTiles(); col++) {
        count += area.open(col, row) ? 1 : 0;
      }
    }
    return count;
  }

  private static BattleSession only(String card) {
    List<String> deck = Collections.nCopies(8, card);
    BattleSession session = BattleSession.ladder(GameData.tables(), deck, deck);
    while (session.tick() < session.match().getStartCooldownTicks()) {
      assertThat(session.step()).isTrue();
    }
    return session;
  }
}
