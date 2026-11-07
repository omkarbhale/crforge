/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.crforge.core.battle.BattleEntity;
import org.crforge.core.battle.unit.CharacterEntity;
import org.crforge.core.battle.unit.WorldEntity;
import org.crforge.core.battle.unit.WorldObserver;
import org.crforge.presentation.DeathCause;
import org.crforge.presentation.DeathFrame;
import org.crforge.presentation.StrikeFrame;

/**
 * Collects a tick's deaths and strikes for a plugin, as values copied while the battle tells them.
 * It only reads what it is handed and only listens; the host drains it after each step. An entity
 * is reported dead once, under the first cause told. A morph, a plain removal and a spawner leaving
 * at its limit tell none of the hooks here, so they are no deaths.
 */
final class TickEventLog implements WorldObserver {

  private final List<DeathFrame> deaths = new ArrayList<>();

  private final List<StrikeFrame> strikes = new ArrayList<>();

  /** Who has been reported dead. */
  private final Set<Integer> dead = new HashSet<>();

  private boolean stopped;

  /** Stops collecting: an observer cannot be detached from a battle. */
  void stop() {
    stopped = true;
    deaths.clear();
    strikes.clear();
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
