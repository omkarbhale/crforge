/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import java.util.ArrayList;
import java.util.List;
import org.crforge.core.pathfinding.GridEntityState;
import org.crforge.desktop.battle.BattleFrame;
import org.crforge.desktop.battle.EntityView;
import org.crforge.presentation.EntityFrame;
import org.crforge.presentation.EntityKind;
import org.crforge.presentation.EntityState;
import org.crforge.presentation.TickFrame;

/**
 * A plugin's frame from the visualizer's: every value copied across. {@link EntityView#grid} stays
 * behind, the view's one live handle into the battle.
 */
final class TickFrames {

  private TickFrames() {
    // Utility class
  }

  static TickFrame of(BattleFrame frame) {
    List<EntityFrame> entities = new ArrayList<>(frame.entities().size());
    for (EntityView view : frame.entities()) {
      entities.add(entity(view));
    }
    return new TickFrame(frame.tick(), frame.timeMs(), frame.ended(), frame.winner(), entities);
  }

  private static EntityFrame entity(EntityView view) {
    return new EntityFrame(
        view.id(),
        kind(view.kind()),
        view.side(),
        view.name(),
        view.x(),
        view.y(),
        view.radius(),
        view.hitPoints(),
        view.maxHitPoints(),
        view.shield(),
        view.maxShield(),
        view.air(),
        view.king(),
        state(view.state()),
        view.deploying(),
        view.hidden(),
        view.hasTarget(),
        view.targetX(),
        view.targetY(),
        view.headingX(),
        view.headingY(),
        view.aimX(),
        view.aimY());
  }

  private static EntityKind kind(EntityView.Kind kind) {
    return switch (kind) {
      case TROOP -> EntityKind.TROOP;
      case BUILDING -> EntityKind.BUILDING;
      case TOWER -> EntityKind.TOWER;
      case PROJECTILE -> EntityKind.PROJECTILE;
      case AREA_EFFECT -> EntityKind.AREA_EFFECT;
    };
  }

  /**
   * The name of a battle's state number.
   *
   * @param state a state number of {@link GridEntityState}, or -1 for anything but a character
   */
  static EntityState state(int state) {
    return switch (state) {
      case -1 -> EntityState.NONE;
      case GridEntityState.STANDING -> EntityState.STANDING;
      case GridEntityState.MOVING -> EntityState.MOVING;
      case GridEntityState.ATTACKING -> EntityState.ATTACKING;
      case GridEntityState.DASHING -> EntityState.DASHING;
      case GridEntityState.DEPLOYING -> EntityState.DEPLOYING;
      case GridEntityState.JUMPING -> EntityState.JUMPING;
      case GridEntityState.SPAWN_PATHFIND -> EntityState.SPAWN_PATHFIND;
      case GridEntityState.INGAME_PATHFIND -> EntityState.INGAME_PATHFIND;
      case GridEntityState.CLONE_SETUP -> EntityState.CLONE_SETUP;
      case GridEntityState.MORPHING -> EntityState.MORPHING;
      case GridEntityState.CASTING -> EntityState.CASTING;
      case GridEntityState.WAITING_TO_DEPLOY -> EntityState.WAITING_TO_DEPLOY;
      case GridEntityState.FOLLOWING_REMOVED -> EntityState.FOLLOWING_REMOVED;
      case GridEntityState.FOLLOWING_REMOVED_BUILDING -> EntityState.FOLLOWING_REMOVED_BUILDING;
      case GridEntityState.COMPONENTS_DISABLED -> EntityState.COMPONENTS_DISABLED;
      case GridEntityState.ROUTE_FOLLOWING_ALTERNATE -> EntityState.ROUTE_FOLLOWING_ALTERNATE;
      case GridEntityState.ABILITY_FOLLOW_UP -> EntityState.ABILITY_FOLLOW_UP;
      default -> throw new IllegalArgumentException("no entity state " + state);
    };
  }
}
