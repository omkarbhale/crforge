/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.core.battle.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.crforge.core.battle.BattleEntity;
import org.crforge.core.battle.GameData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The animation modifier sets (ActionSetAnimationModifier) the forms that select their states on
 * PropState run from their starting actions: each set that starts on a unit is told to the
 * observers with its row's Parameter and Value.
 */
class BattleAnimationModifierTest {

  private static final int LEVEL = Standard1v1Battle.DEFAULT_LEVEL;

  private record Told(int tick, int id, String action, String parameter, int value) {}

  /** A battle with the row deployed on side 0 and a Knight on side 1, and every set told in it. */
  private static List<Told> played(String row, int ticks, List<CharacterEntity> unit) {
    Standard1v1Battle match = new Standard1v1Battle(GameData.tables(), LEVEL, true);
    List<Told> told = new ArrayList<>();
    match
        .getWorld()
        .addObserver(
            new WorldObserver() {
              @Override
              public void animationModifierSet(
                  int tick, BattleEntity entity, String action, String parameter, int value) {
                told.add(new Told(tick, entity.getId(), action, parameter, value));
              }
            });
    unit.add(match.deploy(0, GameData.unit(row), LEVEL, 0, 3500, 18000, row));
    match.deploy(0, GameData.unit("Knight"), LEVEL, 1, 3500, 24000, "k");
    for (int t = 0; t < ticks; t++) {
      match.getBattle().step();
    }
    return told;
  }

  @Test
  @DisplayName(
      "the evolved Angry Barbarian holds PropState 1 while it has its projectile and 0 once it has"
          + " thrown it, set every 50 ms on itself")
  void angryBarbarian() {
    List<CharacterEntity> unit = new ArrayList<>();
    List<Told> told = played("AngryBarbarian_EV1", 200, unit);
    int id = unit.get(0).getId();
    assertThat(told).isNotEmpty().allMatch(t -> t.id() == id && t.parameter().equals("PropState"));
    assertThat(told.get(0).value()).as("with its projectile").isEqualTo(1);
    assertThat(told.get(0).action()).isEqualTo("AngryBarbarian_EV1_set_prop_state");
    assertThat(told.get(told.size() - 1).value()).as("thrown").isZero();
    assertThat(told.get(told.size() - 1).action()).isEqualTo("AngryBarbarian_EV1_set_normal_state");
    // Every tick: the interval is 50 ms.
    assertThat(told.stream().map(Told::tick).distinct().count()).isEqualTo(told.size());
    for (int k = 1; k < told.size(); k++) {
      assertThat(told.get(k).tick() - told.get(k - 1).tick()).isEqualTo(1);
    }
  }

  @Test
  @DisplayName(
      "the hero Electro Wizard holds PropState 0 while its custom tag is not set, and the hero"
          + " Barbarian Barrel's barbarian 1 while its ability has not been played")
  void heroes() {
    List<CharacterEntity> unit = new ArrayList<>();
    List<Told> wizard = played("ElectroWizardHero", 100, unit);
    assertThat(wizard)
        .isNotEmpty()
        .allMatch(t -> t.action().equals("ElectroWizardHero_Enable_Default_Animations"))
        .allMatch(t -> t.value() == 0);
    List<Told> barbarian = played("BarbLogBarbarianHero", 100, unit);
    assertThat(barbarian)
        .isNotEmpty()
        .allMatch(t -> t.action().equals("BarbLog_hero_set_pro_modifier"))
        .allMatch(t -> t.value() == 1);
  }
}
