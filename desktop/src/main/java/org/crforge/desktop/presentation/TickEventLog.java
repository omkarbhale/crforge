/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.crforge.core.battle.BattleEntity;
import org.crforge.core.battle.unit.CharacterEntity;
import org.crforge.core.battle.unit.WorldEntity;
import org.crforge.core.battle.unit.WorldObserver;
import org.crforge.core.pathfinding.target.TargetView;
import org.crforge.presentation.DashFrame;
import org.crforge.presentation.DashMoment;
import org.crforge.presentation.DeathCause;
import org.crforge.presentation.DeathFrame;
import org.crforge.presentation.ForcedAnimationFrame;
import org.crforge.presentation.StrikeFrame;

/**
 * Collects a tick's deaths, strikes and dash moments for a plugin, and the forced animation each
 * entity was last told to show and the animation parameters it was last told to hold, as values
 * copied while the battle tells them. It only reads what it is handed and only listens; the host
 * drains it after each step. An entity is reported dead once, under the first cause told. A morph,
 * a plain removal and a spawner leaving at its limit tell none of the hooks here, so they are no
 * deaths. A forced animation run or an animation parameter set before the log was attached was not
 * heard, so it is not known.
 */
final class TickEventLog implements WorldObserver {

  private final List<DeathFrame> deaths = new ArrayList<>();

  private final List<StrikeFrame> strikes = new ArrayList<>();

  private final List<DashFrame> dashes = new ArrayList<>();

  /**
   * The forced animation each entity was last told to run, by id, in the order first told; one not
   * yet drained holds a start tick of -1.
   */
  private final Map<Integer, ForcedAnimationFrame> forced = new LinkedHashMap<>();

  /**
   * The animation parameters each entity was last told to hold, by id: each parameter's name and
   * value, in the order first set.
   */
  private final Map<Integer, Map<String, Integer>> parameters = new HashMap<>();

  /** Who has been reported dead. */
  private final Set<Integer> dead = new HashSet<>();

  private boolean stopped;

  /** Stops collecting: an observer cannot be detached from a battle. */
  void stop() {
    stopped = true;
    deaths.clear();
    strikes.clear();
    dashes.clear();
    forced.clear();
    parameters.clear();
  }

  /** The dash moments told since the last drain. */
  List<DashFrame> drainDashes() {
    List<DashFrame> drained = List.copyOf(dashes);
    dashes.clear();
    return drained;
  }

  /**
   * The forced animation each entity of the frame shows, by id: a run told since the last drain
   * takes the frame's tick as its start, and an entity no longer in the frame is let go.
   *
   * @param tick the tick of the frame they are told in
   * @param present the ids of the entities in that frame
   */
  Map<Integer, ForcedAnimationFrame> forcedAnimations(int tick, Set<Integer> present) {
    forced.keySet().retainAll(present);
    forced.replaceAll(
        (id, run) ->
            run.startTick() >= 0
                ? run
                : new ForcedAnimationFrame(
                    run.action(),
                    run.customState(),
                    tick,
                    run.playbackDurationMs(),
                    run.forcedDurationMs()));
    return Map.copyOf(forced);
  }

  /**
   * The animation parameters each entity of the frame holds, by id: the last value set for each
   * parameter, held until the entity leaves the frame, which lets them go. An entity with none set
   * is absent.
   *
   * @param present the ids of the entities in the frame they are told in
   */
  Map<Integer, Map<String, Integer>> animationParameters(Set<Integer> present) {
    parameters.keySet().retainAll(present);
    Map<Integer, Map<String, Integer>> held = new HashMap<>();
    parameters.forEach(
        (id, values) -> held.put(id, Collections.unmodifiableMap(new LinkedHashMap<>(values))));
    return Collections.unmodifiableMap(held);
  }

  /** The deaths told since the last drain. */
  List<DeathFrame> drainDeaths() {
    List<DeathFrame> drained = List.copyOf(deaths);
    deaths.clear();
    return drained;
  }

  /** The strikes told since the last drain. */
  List<StrikeFrame> drainStrikes() {
    List<StrikeFrame> drained = List.copyOf(strikes);
    strikes.clear();
    return drained;
  }

  @Override
  public void entityDied(int tick, WorldEntity dying, BattleEntity attacker) {
    died(dying, attacker == dying ? DeathCause.KAMIKAZE : DeathCause.KILLED);
  }

  @Override
  public void decayDied(int tick, WorldEntity entity, int hitPointsBefore) {
    died(entity, DeathCause.DECAY);
  }

  @Override
  public void diedAtRemoval(int tick, WorldEntity entity) {
    died(entity, DeathCause.EXPIRED);
  }

  @Override
  public void parentLeft(int tick, CharacterEntity rider, CharacterEntity parent) {
    died(rider, DeathCause.PARENT_LEFT);
  }

  @Override
  public void attackHitLanded(int tick, WorldEntity attacker, WorldEntity victim, boolean special) {
    if (!stopped) {
      strikes.add(new StrikeFrame(attacker.getId(), victim.getId(), special));
    }
  }

  @Override
  public void dashWindupStarted(int tick, CharacterEntity unit, int windupMs) {
    dash(unit, DashMoment.WINDUP_STARTED);
  }

  @Override
  public void dashStarted(
      int tick,
      CharacterEntity unit,
      TargetView reference,
      int fromX,
      int fromY,
      int aimX,
      int aimY) {
    dash(unit, DashMoment.STARTED);
  }

  @Override
  public void dashLanded(
      int tick, CharacterEntity unit, WorldEntity hit, int damage, boolean area) {
    dash(unit, DashMoment.LANDED);
  }

  @Override
  public void forcedAnimationRun(
      int tick,
      BattleEntity entity,
      String action,
      int customState,
      int playbackDurationMs,
      int forcedDurationMs) {
    if (!stopped) {
      // Removed first, so a later run is listed in the order it was told.
      forced.remove(entity.getId());
      forced.put(
          entity.getId(),
          new ForcedAnimationFrame(action, customState, -1, playbackDurationMs, forcedDurationMs));
    }
  }

  @Override
  public void forcedAnimationStopped(int tick, BattleEntity entity, String action) {
    if (!stopped) {
      forced.remove(entity.getId());
    }
  }

  @Override
  public void animationModifierSet(
      int tick, BattleEntity entity, String action, String parameter, int value) {
    if (!stopped) {
      parameters.computeIfAbsent(entity.getId(), id -> new LinkedHashMap<>()).put(parameter, value);
    }
  }

  private void dash(CharacterEntity unit, DashMoment moment) {
    if (!stopped) {
      dashes.add(new DashFrame(unit.getId(), moment));
    }
  }

  private void died(WorldEntity entity, DeathCause cause) {
    if (stopped || !dead.add(entity.getId())) {
      return;
    }
    deaths.add(
        new DeathFrame(
            entity.getId(),
            entity.getData().name(),
            entity.side(),
            entity.getView().getX(),
            entity.getView().getY(),
            cause));
  }
}
