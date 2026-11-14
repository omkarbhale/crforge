/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.crforge.core.battle.unit.ChampionController;
import org.crforge.core.pathfinding.GridEntityState;
import org.crforge.desktop.battle.BattleFrame;
import org.crforge.desktop.battle.BattleFrame.AbilityView;
import org.crforge.desktop.battle.BattleFrame.CardView;
import org.crforge.desktop.battle.BattleFrame.SideView;
import org.crforge.desktop.battle.EntityView;
import org.crforge.desktop.battle.UnitStatus;
import org.crforge.presentation.AbilityFrame;
import org.crforge.presentation.AbilityState;
import org.crforge.presentation.AttackFrame;
import org.crforge.presentation.CardFrame;
import org.crforge.presentation.CardPlayFrame;
import org.crforge.presentation.DashFrame;
import org.crforge.presentation.DeathFrame;
import org.crforge.presentation.DeployArea;
import org.crforge.presentation.EntityFrame;
import org.crforge.presentation.EntityKind;
import org.crforge.presentation.EntityState;
import org.crforge.presentation.ForcedAnimationFrame;
import org.crforge.presentation.SideFrame;
import org.crforge.presentation.StatusFrame;
import org.crforge.presentation.StrikeFrame;
import org.crforge.presentation.TickFrame;

/**
 * A plugin's frame from the visualizer's: every value copied across. {@link EntityView#grid} stays
 * behind, the view's one live handle into the battle.
 */
final class TickFrames {

  private TickFrames() {
    // Utility class
  }

  /**
   * A plugin's frame with the tick's deaths and strikes, and no card plays.
   *
   * @param deaths what died in the tick just run; none on a battle's first frame
   * @param strikes the attacks' hits that landed in it; none on a battle's first frame
   */
  static TickFrame of(BattleFrame frame, List<DeathFrame> deaths, List<StrikeFrame> strikes) {
    return of(frame, deaths, strikes, List.of());
  }

  /**
   * A plugin's frame with the tick's events.
   *
   * @param deaths what died in the tick just run; none on a battle's first frame
   * @param strikes the attacks' hits that landed in it; none on a battle's first frame
   * @param plays the card plays that ran in it and put their card down; none on a battle's first
   *     frame
   */
  static TickFrame of(
      BattleFrame frame,
      List<DeathFrame> deaths,
      List<StrikeFrame> strikes,
      List<CardPlayFrame> plays) {
    return of(frame, deaths, strikes, plays, List.of());
  }

  /**
   * A plugin's frame with the tick's events and each side's deploy area.
   *
   * @param deaths what died in the tick just run; none on a battle's first frame
   * @param strikes the attacks' hits that landed in it; none on a battle's first frame
   * @param plays the card plays that ran in it and put their card down; none on a battle's first
   *     frame
   * @param areas each side's deploy area by side; a side with none is told {@link DeployArea#NONE}
   */
  static TickFrame of(
      BattleFrame frame,
      List<DeathFrame> deaths,
      List<StrikeFrame> strikes,
      List<CardPlayFrame> plays,
      List<DeployArea> areas) {
    return of(frame, deaths, strikes, plays, areas, List.of(), Map.of());
  }

  /**
   * A plugin's frame with the tick's events, each side's deploy area and the forced animations.
   *
   * @param deaths what died in the tick just run; none on a battle's first frame
   * @param strikes the attacks' hits that landed in it; none on a battle's first frame
   * @param plays the card plays that ran in it and put their card down; none on a battle's first
   *     frame
   * @param areas each side's deploy area by side; a side with none is told {@link DeployArea#NONE}
   * @param dashes the dash moments of the tick just run; none on a battle's first frame
   * @param forced the forced animation each entity shows, by id; an entity absent shows none
   */
  static TickFrame of(
      BattleFrame frame,
      List<DeathFrame> deaths,
      List<StrikeFrame> strikes,
      List<CardPlayFrame> plays,
      List<DeployArea> areas,
      List<DashFrame> dashes,
      Map<Integer, ForcedAnimationFrame> forced) {
    List<EntityFrame> entities = new ArrayList<>(frame.entities().size());
    for (EntityView view : frame.entities()) {
      entities.add(entity(view, forced.getOrDefault(view.id(), ForcedAnimationFrame.NONE)));
    }
    List<SideFrame> sides = new ArrayList<>(frame.sides().size());
    for (SideView view : frame.sides()) {
      int s = view.side();
      sides.add(side(view, s >= 0 && s < areas.size() ? areas.get(s) : DeployArea.NONE));
    }
    return new TickFrame(
        frame.tick(),
        frame.timeMs(),
        frame.ended(),
        frame.winner(),
        entities,
        frame.overtime(),
        frame.elixirRate(),
        sides,
        frame.section(),
        frame.sectionLeftMs(),
        frame.endTimerMs(),
        frame.tiebreakMs(),
        frame.nextCardCooldownMs(),
        deaths,
        strikes,
        plays,
        dashes);
  }

  private static SideFrame side(SideView view, DeployArea area) {
    List<CardFrame> hand = new ArrayList<>(view.hand().size());
    for (CardView card : view.hand()) {
      hand.add(card(card));
    }
    List<AbilityFrame> abilities = new ArrayList<>(view.abilities().size());
    for (AbilityView ability : view.abilities()) {
      abilities.add(ability(ability));
    }
    return new SideFrame(
        view.side(),
        view.elixir(),
        view.wholeElixir(),
        view.crowns(),
        hand,
        card(view.next()),
        abilities,
        view.nextCardCooldownLeftMs(),
        area);
  }

  private static CardFrame card(CardView card) {
    return card == null
        ? CardFrame.NONE
        : new CardFrame(
            card.name(),
            card.cost(),
            card.level(),
            card.pending(),
            card.unavailableReason() == null,
            card.evolutionCount(),
            card.evolutionNeed(),
            card.evolved());
  }

  private static AbilityFrame ability(AbilityView ability) {
    return ability == null
        ? AbilityFrame.NONE
        : new AbilityFrame(
            ability.champion(),
            ability.cost(),
            abilityState(ability.state()),
            ability.cooldownMs(),
            ability.charges(),
            ability.pending(),
            ability.unavailableReason() == null);
  }

  /**
   * The name of a champion slot's button state.
   *
   * @param state a state number of {@link ChampionController}; any other, which only an action row
   *     naming one of the bounding markers can write, has no button to show
   */
  static AbilityState abilityState(int state) {
    return switch (state) {
      case ChampionController.ABSENT -> AbilityState.ABSENT;
      case ChampionController.READY -> AbilityState.READY;
      case ChampionController.DEPLOYING -> AbilityState.DEPLOYING;
      case ChampionController.LIMITED_AVAILABILITY -> AbilityState.LIMITED_AVAILABILITY;
      case ChampionController.ALL_CHARGES_CONSUMED -> AbilityState.ALL_CHARGES_CONSUMED;
      case ChampionController.PENDING -> AbilityState.PENDING;
      case ChampionController.ON_COOLDOWN -> AbilityState.ON_COOLDOWN;
      case ChampionController.NOT_ENOUGH_ELIXIR -> AbilityState.NOT_ENOUGH_ELIXIR;
      case ChampionController.CASTING -> AbilityState.CASTING;
      case ChampionController.DISABLED -> AbilityState.DISABLED;
      case ChampionController.TEMPORARILY_UNAVAILABLE -> AbilityState.TEMPORARILY_UNAVAILABLE;
      case ChampionController.NO_YET_AVAILABLE -> AbilityState.NOT_YET_AVAILABLE;
      default -> AbilityState.UNKNOWN;
    };
  }

  private static EntityFrame entity(EntityView view, ForcedAnimationFrame forced) {
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
        view.aimY(),
        view.height(),
        statuses(view),
        view.hasStatus(UnitStatus.Kind.CLONE),
        view.level(),
        attack(view.attack()),
        view.charge().progress(),
        view.charge().charging(),
        forced,
        view.carrierId() == EntityView.NO_CARRIER ? EntityFrame.NO_CARRIER : view.carrierId());
  }

  /**
   * A character's attack timing; a projectile's or area effect's {@link EntityView.Attack#NONE}
   * copies across equal to {@link AttackFrame#NONE}.
   */
  private static AttackFrame attack(EntityView.Attack attack) {
    return new AttackFrame(
        attack.timerMs(),
        attack.loadTimerMs(),
        attack.startDelayMs(),
        attack.hitSpeedMs(),
        attack.loadTimeMs(),
        attack.sequenceIndex());
  }

  /** The buffs on an entity; a clone's flag stands in the view's statuses but is no buff row. */
  private static List<StatusFrame> statuses(EntityView view) {
    List<StatusFrame> statuses = new ArrayList<>(view.statuses().size());
    for (UnitStatus status : view.statuses()) {
      if (status.row() != null) {
        statuses.add(
            new StatusFrame(
                status.row(), status.remainingMs(), status.totalMs(), status.appliedSide()));
      }
    }
    return statuses;
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
