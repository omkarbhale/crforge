/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Everything a plugin can reach through the API is a value, with nothing of an engine in it. */
class PresentationApiTest {

  @Test
  @DisplayName(
      "every type a plugin can reach is a primitive, a string, an enum or a record of them")
  void readOnlyByConstruction() {
    Set<Class<?>> records = new HashSet<>();
    for (Method method : PresentationPlugin.class.getDeclaredMethods()) {
      for (Type type : method.getGenericParameterTypes()) {
        walk(type, records);
      }
      walk(method.getGenericReturnType(), records);
    }
    assertThat(records)
        .contains(
            BattleStart.class,
            TickFrame.class,
            EntityFrame.class,
            SideFrame.class,
            CardFrame.class,
            AbilityFrame.class,
            StatusFrame.class,
            DeathFrame.class,
            StrikeFrame.class,
            PresentContext.class,
            ArenaProjection.class);
  }

  @Test
  @DisplayName("a plugin reports no projection and names no pick unless it says so")
  void noProjectionByDefault() {
    PresentationPlugin plugin =
        new PresentationPlugin() {
          @Override
          public String id() {
            return "none";
          }

          @Override
          public void battleStarted(BattleStart battle) {}

          @Override
          public void tick(TickFrame frame) {}

          @Override
          public void present(PresentContext context) {}
        };
    assertThat(plugin.arenaProjection()).isNull();
    assertThat(plugin.entityAt(0, 0))
        .as("a plugin names no entity unless it picks on its own")
        .isEqualTo(PresentationPlugin.NO_ENTITY)
        .isNegative();
  }

  @Test
  @DisplayName("a projection maps a pixel back to the position it draws there, on each axis")
  void projectionRoundTrip() {
    // Reversed across the arena and scaled differently along it, off the rectangle's corner.
    ArenaProjection projection = new ArenaProjection(612.5f, -41f, -0.0242f, 0.0299f);
    assertThat(projection.pixelX(9000)).isCloseTo(612.5f - 217.8f, within(1e-3f));
    assertThat(projection.pixelY(32000)).isCloseTo(-41f + 956.8f, within(1e-3f));
    for (float x : new float[] {0, 500, 9000, 17_500, 18_000}) {
      assertThat(projection.x(projection.pixelX(x))).isCloseTo(x, within(0.05f));
    }
    for (float y : new float[] {0, 500, 16_000, 31_500, 32_000}) {
      assertThat(projection.y(projection.pixelY(y))).isCloseTo(y, within(0.05f));
    }
  }

  @Test
  @DisplayName("a projection no point maps back through is refused")
  void projectionRefused() {
    assertThatThrownBy(() -> new ArenaProjection(0, 0, 0, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ArenaProjection(0, 0, 1, Float.NaN))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ArenaProjection(Float.POSITIVE_INFINITY, 0, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Those signatures are held for crforge-render, whose tests and tools construct frames. */
  @Test
  @DisplayName(
      "the constructors from before statuses, events, the level and attack timing default them to"
          + " empty, no clone, no level, no attack timing")
  void olderConstructors() {
    assertThat(entity(1).statuses()).isEmpty();
    assertThat(entity(1).isClone()).isFalse();
    assertThat(entity(1).level()).isEqualTo(EntityFrame.NO_LEVEL);
    assertThat(entity(1).attack()).isEqualTo(AttackFrame.NONE);
    assertThat(withLevel(entity(1), 11).level()).isEqualTo(11);
    assertThat(withLevel(entity(1), 11).attack()).isEqualTo(AttackFrame.NONE);
    AttackFrame attack = new AttackFrame(650, 0, 0, 1200, 700, 2);
    EntityFrame attacking = withAttack(withLevel(entity(1), 11), attack);
    assertThat(attacking.attack()).isEqualTo(attack);
    assertThat(attacking.level()).isEqualTo(11);
    assertThat(AttackFrame.NONE.sequenceIndex()).isEqualTo(AttackFrame.NO_SEQUENCE_STEP);
    assertThatThrownBy(() -> withAttack(entity(1), null)).isInstanceOf(NullPointerException.class);
    TickFrame frame = frame(List.of(entity(1)), List.of());
    assertThat(frame.deaths()).isEmpty();
    assertThat(frame.strikes()).isEmpty();
  }

  @Test
  @DisplayName("a frame's statuses and events are copied and cannot be changed")
  void statusesAndEventsAreCopied() {
    List<StatusFrame> statuses = new ArrayList<>(List.of(new StatusFrame("Freeze", 500, 4000, 1)));
    List<DeathFrame> deaths =
        new ArrayList<>(List.of(new DeathFrame(1, "Knight", 0, 10, 20, DeathCause.KILLED)));
    List<StrikeFrame> strikes = new ArrayList<>(List.of(new StrikeFrame(1, 2, true)));
    EntityFrame old = entity(1);
    EntityFrame entity =
        new EntityFrame(
            old.id(),
            old.kind(),
            old.side(),
            old.name(),
            old.x(),
            old.y(),
            old.radius(),
            old.hitPoints(),
            old.maxHitPoints(),
            old.shield(),
            old.maxShield(),
            old.air(),
            old.king(),
            old.state(),
            old.deploying(),
            old.hidden(),
            old.hasTarget(),
            old.targetX(),
            old.targetY(),
            old.headingX(),
            old.headingY(),
            old.aimX(),
            old.aimY(),
            old.height(),
            statuses,
            false);
    assertThat(entity.level()).as("the constructor from before the level").isZero();
    TickFrame frame =
        new TickFrame(
            7, 350, false, -1, List.of(), false, 1, List.of(), 0, 0, 0, 0, 0, deaths, strikes);
    statuses.clear();
    deaths.clear();
    strikes.clear();
    assertThat(entity.statuses()).hasSize(1);
    assertThat(frame.deaths()).hasSize(1);
    assertThat(frame.strikes()).hasSize(1);
    assertThatThrownBy(() -> frame.deaths().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  /** Follows a type, failing on any that is not a value of this package or the language. */
  private static void walk(Type type, Set<Class<?>> records) {
    if (type instanceof ParameterizedType generic) {
      assertThat(generic.getRawType()).as("the only generic type").isEqualTo(List.class);
      walk(generic.getActualTypeArguments()[0], records);
      return;
    }
    Class<?> value = (Class<?>) type;
    if (value.isPrimitive() || value == String.class) {
      return;
    }
    assertThat(value.getPackageName()).as("%s", value).isEqualTo("org.crforge.presentation");
    if (value.isEnum()) {
      return;
    }
    assertThat(value.isRecord()).as("%s is a record", value).isTrue();
    if (records.add(value)) {
      for (RecordComponent component : value.getRecordComponents()) {
        walk(component.getGenericType(), records);
      }
    }
  }

  @Test
  @DisplayName("a frame keeps its own unmodifiable list of entities")
  void frameIsDetached() {
    List<EntityFrame> entities = new ArrayList<>();
    entities.add(entity(1));
    TickFrame frame = frame(entities, List.of());

    entities.add(entity(2));

    assertThat(frame.entities()).extracting(EntityFrame::id).containsExactly(1);
    assertThatThrownBy(() -> frame.entities().add(entity(3)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("a frame keeps its own unmodifiable lists of sides, and a side its hand and slots")
  void sidesAreDetached() {
    List<CardFrame> hand = new ArrayList<>();
    hand.add(CardFrame.NONE);
    List<AbilityFrame> abilities = new ArrayList<>();
    abilities.add(AbilityFrame.NONE);
    SideFrame side = new SideFrame(0, 50_000, 5, 1, hand, CardFrame.NONE, abilities, 0);
    List<SideFrame> sides = new ArrayList<>();
    sides.add(side);
    TickFrame frame = frame(List.of(), sides);

    hand.add(CardFrame.NONE);
    abilities.add(AbilityFrame.NONE);
    sides.clear();

    assertThat(frame.sides()).containsExactly(side);
    assertThat(side.hand()).hasSize(1);
    assertThat(side.abilities()).hasSize(1);
    assertThatThrownBy(() -> frame.sides().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> side.hand().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> side.abilities().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  /** The entity at the given level, built with the constructor from before attack timing. */
  private static EntityFrame withLevel(EntityFrame old, int level) {
    return new EntityFrame(
        old.id(),
        old.kind(),
        old.side(),
        old.name(),
        old.x(),
        old.y(),
        old.radius(),
        old.hitPoints(),
        old.maxHitPoints(),
        old.shield(),
        old.maxShield(),
        old.air(),
        old.king(),
        old.state(),
        old.deploying(),
        old.hidden(),
        old.hasTarget(),
        old.targetX(),
        old.targetY(),
        old.headingX(),
        old.headingY(),
        old.aimX(),
        old.aimY(),
        old.height(),
        old.statuses(),
        old.isClone(),
        level);
  }

  /** The entity with the given attack timing, built with the record's own constructor. */
  private static EntityFrame withAttack(EntityFrame old, AttackFrame attack) {
    return new EntityFrame(
        old.id(),
        old.kind(),
        old.side(),
        old.name(),
        old.x(),
        old.y(),
        old.radius(),
        old.hitPoints(),
        old.maxHitPoints(),
        old.shield(),
        old.maxShield(),
        old.air(),
        old.king(),
        old.state(),
        old.deploying(),
        old.hidden(),
        old.hasTarget(),
        old.targetX(),
        old.targetY(),
        old.headingX(),
        old.headingY(),
        old.aimX(),
        old.aimY(),
        old.height(),
        old.statuses(),
        old.isClone(),
        old.level(),
        attack);
  }

  private static TickFrame frame(List<EntityFrame> entities, List<SideFrame> sides) {
    return new TickFrame(7, 350, false, -1, entities, false, 1, sides, 0, 0, 0, 0, 0);
  }

  private static EntityFrame entity(int id) {
    return new EntityFrame(
        id,
        EntityKind.TROOP,
        0,
        "Knight",
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        false,
        false,
        EntityState.STANDING,
        false,
        false,
        false,
        0,
        0,
        0,
        0,
        0,
        0,
        0);
  }
}
