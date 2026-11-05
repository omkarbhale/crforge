/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            PresentContext.class);
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

  private static TickFrame frame(List<EntityFrame> entities, List<SideFrame> sides) {
    return new TickFrame(7, 350, false, -1, entities, false, 1, sides, 0, 0, false, 0, 0, 0);
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
