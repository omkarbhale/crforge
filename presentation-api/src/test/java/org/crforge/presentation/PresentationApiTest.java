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
        .contains(BattleStart.class, TickFrame.class, EntityFrame.class, PresentContext.class);
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
    TickFrame frame = new TickFrame(7, 350, false, -1, entities);

    entities.add(entity(2));

    assertThat(frame.entities()).extracting(EntityFrame::id).containsExactly(1);
    assertThatThrownBy(() -> frame.entities().add(entity(3)))
        .isInstanceOf(UnsupportedOperationException.class);
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
        0);
  }
}
