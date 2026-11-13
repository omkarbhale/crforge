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
            CardPlayFrame.class,
            TowerSkinFrame.class,
            PresentContext.class,
            ArenaProjection.class,
            CardCommand.Select.class,
            CardCommand.Play.class,
            CardCommand.Deselect.class,
            DeployArea.class,
            PlacementPreview.class,
            PlacementPreview.PlacedUnit.class);
  }

  @Test
  @DisplayName("a plugin reports no projection, names no pick and asks nothing unless it says so")
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
    assertThat(plugin.cardCommandAt(0, 0, false))
        .as("a press leaves the hand to the host")
        .isNull();
    assertThat(plugin.cardCommandAt(0, 0, true)).as("a release leaves it too").isNull();
    // A pointer the plugin does not follow is a no-op.
    plugin.pointerMoved(10, 20, true);
    plugin.pointerMoved(-5, 2000, false);
  }

  /** Those signatures are held for crforge-render, whose tests and harness construct contexts. */
  @Test
  @DisplayName(
      "the context constructors from before the deploy tiles tell no tiles and no placement")
  void contextWithoutDeployTiles() {
    PresentContext selected = new PresentContext(10, 20, 300, 400, 0, 1, 2);
    assertThat(selected.selectedArea()).isSameAs(DeployArea.NONE);
    assertThat(selected.placement()).isSameAs(PlacementPreview.NONE);
    assertThat(new PresentContext(10, 20, 300, 400, 1).selectedArea()).isSameAs(DeployArea.NONE);
    assertThat(new PresentContext(10, 20, 300, 400, 0, 1, 2, null, null)).isEqualTo(selected);
    DeployArea area = new DeployArea(1, 2, 1, List.of(true, false));
    PlacementPreview placement = new PlacementPreview(0, 0, true, 500, 500, List.of());
    PresentContext told = new PresentContext(10, 20, 300, 400, 0, 1, 2, area, placement);
    assertThat(told.selectedArea()).isEqualTo(area);
    assertThat(told.placement()).isEqualTo(placement);
    SideFrame side = new SideFrame(0, 0, 0, 0, List.of(), CardFrame.NONE, List.of(), 0);
    assertThat(side.deployArea()).isSameAs(DeployArea.NONE);
    assertThat(
            new SideFrame(0, 0, 0, 0, List.of(), CardFrame.NONE, List.of(), 0, area).deployArea())
        .isEqualTo(area);
  }

  @Test
  @DisplayName(
      "a deploy area counts its tiles row after row from side 0's end, and refuses a grid its"
          + " flags do not fill")
  void deployArea() {
    // Three columns by two rows: row 0 open but its middle, row 1 closed but its middle.
    List<Boolean> flags = new ArrayList<>(List.of(true, false, true, false, true, false));
    DeployArea area = new DeployArea(0, 3, 2, flags);
    flags.clear();
    assertThat(area.tiles()).hasSize(6);
    assertThatThrownBy(() -> area.tiles().set(0, false))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(area.open(0, 0)).isTrue();
    assertThat(area.open(1, 0)).isFalse();
    assertThat(area.open(1, 1)).isTrue();
    assertThat(area.open(2, 1)).isFalse();
    assertThat(area.open(-1, 0)).as("off the grid").isFalse();
    assertThat(area.open(3, 0)).isFalse();
    assertThat(area.open(0, 2)).isFalse();
    assertThat(area.openAt(1500, 1999)).as("the tile holding the position").isTrue();
    assertThat(area.openAt(1500, 999)).isFalse();
    assertThat(area.openAt(-1, 500)).isFalse();
    assertThat(area.openAt(Float.NaN, 500)).isFalse();
    assertThat(DeployArea.NONE.open(0, 0)).isFalse();
    assertThatThrownBy(() -> new DeployArea(0, 3, 2, List.of(true)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DeployArea(0, -1, 0, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("a placement preview keeps its own unmodifiable list of units")
  void placementPreviewIsDetached() {
    List<PlacementPreview.PlacedUnit> units =
        new ArrayList<>(List.of(new PlacementPreview.PlacedUnit("Knight", 4500, 10500)));
    PlacementPreview preview = new PlacementPreview(4, 10, true, 4500, 10500, units);
    units.clear();
    assertThat(preview.units())
        .containsExactly(new PlacementPreview.PlacedUnit("Knight", 4500, 10500));
    assertThatThrownBy(() -> preview.units().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(PlacementPreview.NONE.placed()).isFalse();
    assertThat(PlacementPreview.NONE.tileX()).isNegative();
  }

  /** That signature is held for crforge-render, whose tests construct contexts. */
  @Test
  @DisplayName("the constructor from before the selection says no card is selected")
  void contextWithoutSelection() {
    PresentContext context = new PresentContext(10, 20, 300, 400, 1);
    assertThat(context.selectedSide()).isEqualTo(PresentContext.NO_SELECTION).isNegative();
    assertThat(context.selectedSlot()).isEqualTo(PresentContext.NO_SELECTION);
    assertThat(context).isEqualTo(new PresentContext(10, 20, 300, 400, 1, -1, -1));
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
    assertThat(frame.plays()).isEmpty();
  }

  /** That signature is held for crforge-render, whose tests construct frames with events. */
  @Test
  @DisplayName("the constructor from before card plays tells its events and no play")
  void eventsWithoutPlays() {
    List<DeathFrame> deaths = List.of(new DeathFrame(1, "Knight", 0, 10, 20, DeathCause.KILLED));
    List<StrikeFrame> strikes = List.of(new StrikeFrame(1, 2, true));
    TickFrame frame =
        new TickFrame(
            7, 350, false, -1, List.of(), false, 1, List.of(), 0, 0, 0, 0, 0, deaths, strikes);
    assertThat(frame.deaths()).isEqualTo(deaths);
    assertThat(frame.strikes()).isEqualTo(strikes);
    assertThat(frame.plays()).isEmpty();
    assertThat(frame)
        .isEqualTo(
            new TickFrame(
                7, 350, false, -1, List.of(), false, 1, List.of(), 0, 0, 0, 0, 0, deaths, strikes,
                List.of()));
  }

  /** That signature is held for crforge-render, whose tests and harness construct starts. */
  @Test
  @DisplayName("the start from before tower skins is no replay's and tells no skin")
  void startWithoutSkins() {
    BattleStart start = new BattleStart(18_000, 32_000, 5);
    assertThat(start.towerSkins()).isEmpty();
    assertThat(start.rndSeed()).isZero();
    assertThat(start.replay()).isFalse();
    assertThat(start).isEqualTo(new BattleStart(18_000, 32_000, 5, List.of(), 0, false));
  }

  @Test
  @DisplayName("a start keeps its own unmodifiable tower skins, and each its own lists")
  void towerSkinsAreDetached() {
    List<Integer> os = new ArrayList<>(List.of(87000001, 94000033));
    List<Integer> ss = new ArrayList<>(List.of(94000033, 94000033));
    TowerSkinFrame skin = new TowerSkinFrame(1, os, ss, 94000033, true);
    List<TowerSkinFrame> skins = new ArrayList<>(List.of(skin));
    BattleStart start = new BattleStart(18_000, 32_000, 0, skins, 1131, true);
    os.clear();
    ss.clear();
    skins.clear();

    assertThat(start.towerSkins()).containsExactly(skin);
    assertThat(skin.os()).containsExactly(87000001, 94000033);
    assertThat(skin.ss()).as("repeats kept").containsExactly(94000033, 94000033);
    assertThatThrownBy(() -> start.towerSkins().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> skin.os().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("a card play keeps its own unmodifiable list of units, and counts them")
  void cardPlayIsDetached() {
    List<Integer> units = new ArrayList<>(List.of(1_000_007, 1_000_008, 1_000_009));
    CardPlayFrame play = new CardPlayFrame(1, "SkeletonArmy", CardForm.EVOLVED, 9000, 22500, units);
    TickFrame frame =
        new TickFrame(
            7,
            350,
            false,
            -1,
            List.of(),
            false,
            1,
            List.of(),
            0,
            0,
            0,
            0,
            0,
            List.of(),
            List.of(),
            new ArrayList<>(List.of(play)));
    units.clear();

    assertThat(play.unitIds()).containsExactly(1_000_007, 1_000_008, 1_000_009);
    assertThat(play.unitCount()).isEqualTo(3);
    assertThat(frame.plays()).containsExactly(play);
    assertThatThrownBy(() -> play.unitIds().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> frame.plays().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    CardPlayFrame spell = new CardPlayFrame(0, "Fireball", CardForm.BASIC, 9000, 25000, List.of());
    assertThat(spell.unitCount()).as("a spell makes no unit").isZero();
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
    // A boxed int or boolean is as much a value as the primitive: a list of ids, or of a deploy
    // area's tile flags, holds them.
    if (value.isPrimitive()
        || value == String.class
        || value == Integer.class
        || value == Boolean.class) {
      return;
    }
    assertThat(value.getPackageName()).as("%s", value).isEqualTo("org.crforge.presentation");
    if (value.isEnum()) {
      return;
    }
    if (value.isSealed()) {
      // A choice of values, such as a command: each of its kinds is a record of them.
      for (Class<?> kind : value.getPermittedSubclasses()) {
        walk(kind, records);
      }
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
