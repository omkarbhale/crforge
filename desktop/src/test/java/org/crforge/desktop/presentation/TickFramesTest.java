/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.crforge.core.battle.BattleEntity;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.battle.match.EvolutionItem;
import org.crforge.core.battle.match.Hand;
import org.crforge.core.battle.match.LadderMatch;
import org.crforge.core.battle.match.MatchCard;
import org.crforge.core.battle.match.MatchSide;
import org.crforge.core.battle.match.Timeline;
import org.crforge.core.battle.projectile.ProjectileEntity;
import org.crforge.core.battle.unit.AreaEffectEntity;
import org.crforge.core.battle.unit.ChampionController;
import org.crforge.core.battle.unit.WorldEntity;
import org.crforge.core.pathfinding.GridEntityState;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleFrame;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.replay.ReplayFile;
import org.crforge.desktop.replay.ReplayPlayer;
import org.crforge.presentation.AbilityFrame;
import org.crforge.presentation.AbilityState;
import org.crforge.presentation.CardFrame;
import org.crforge.presentation.EntityKind;
import org.crforge.presentation.EntityState;
import org.crforge.presentation.SideFrame;
import org.crforge.presentation.TickFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TickFramesTest {

  @Test
  @DisplayName("every state of the battle has its name, and none other is taken for one")
  void everyState() {
    List<EntityState> named = new ArrayList<>();
    for (int state = 0; state <= GridEntityState.MAX_STATE; state++) {
      named.add(TickFrames.state(state));
    }
    assertThat(named).doesNotContain(EntityState.NONE).doesNotHaveDuplicates();
    assertThat(named).hasSize(EntityState.values().length - 1);
    assertThat(TickFrames.state(GridEntityState.ATTACKING)).isEqualTo(EntityState.ATTACKING);
    assertThat(TickFrames.state(-1)).isEqualTo(EntityState.NONE);
    assertThatThrownBy(() -> TickFrames.state(GridEntityState.MAX_STATE + 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("every button state of a champion slot has its name; a marker shows none")
  void everyAbilityState() {
    List<AbilityState> named = new ArrayList<>();
    for (int state = ChampionController.UNKNOWN;
        state <= ChampionController.NO_YET_AVAILABLE;
        state++) {
      named.add(TickFrames.abilityState(state));
    }
    assertThat(named).containsAll(List.of(AbilityState.values()));
    assertThat(TickFrames.abilityState(ChampionController.ON_COOLDOWN))
        .isEqualTo(AbilityState.ON_COOLDOWN);
    assertThat(TickFrames.abilityState(ChampionController.NO_YET_AVAILABLE))
        .isEqualTo(AbilityState.NOT_YET_AVAILABLE);
    assertThat(TickFrames.abilityState(5)).as("ERR_START").isEqualTo(AbilityState.UNKNOWN);
    assertThat(TickFrames.abilityState(14)).as("ERR_MAX").isEqualTo(AbilityState.UNKNOWN);
  }

  @Test
  @DisplayName("a match's clock and sides are copied, with no card or champion for a null")
  void matchFields() {
    BattleFrame.CardView knight =
        new BattleFrame.CardView("Knight", 3, 11, false, null, 1, 2, false);
    BattleFrame.CardView arrows =
        new BattleFrame.CardView("Arrows", 3, 9, true, "too expensive", 2, 2, true);
    BattleFrame.CardView next =
        new BattleFrame.CardView("Giant", 5, 11, false, "not in hand", 0, 0, false);
    BattleFrame.AbilityView ability =
        new BattleFrame.AbilityView(
            "Monk", 1, ChampionController.ON_COOLDOWN, 4_000, -1, true, "on cooldown");
    BattleFrame.SideView full =
        new BattleFrame.SideView(
            0,
            52_500,
            5,
            2,
            Arrays.asList(knight, null, arrows, null),
            next,
            Arrays.asList(ability, null),
            1_500);
    BattleFrame.SideView none =
        new BattleFrame.SideView(
            1, 0, 0, 0, Arrays.asList(null, null), null, Arrays.asList(null, null), 0);

    TickFrame frame =
        TickFrames.of(
            new BattleFrame(
                9,
                450,
                true,
                3,
                List.of(),
                List.of(full, none),
                false,
                -1,
                false,
                null,
                List.of(),
                2,
                61_000,
                51,
                100,
                2_000),
            List.of(),
            List.of());

    assertThat(frame.overtime()).isTrue();
    assertThat(frame.elixirRate()).isEqualTo(3);
    assertThat(frame.section()).isEqualTo(2);
    assertThat(frame.sectionLeftMs()).isEqualTo(61_000);
    assertThat(frame.endTimerMs()).isEqualTo(51);
    assertThat(frame.tiebreakMs()).isEqualTo(100);
    assertThat(frame.nextCardCooldownMs()).isEqualTo(2_000);
    assertThat(frame.sides())
        .containsExactly(
            new SideFrame(
                0,
                52_500,
                5,
                2,
                List.of(
                    new CardFrame("Knight", 3, 11, false, true, 1, 2, false),
                    CardFrame.NONE,
                    new CardFrame("Arrows", 3, 9, true, false, 2, 2, true),
                    CardFrame.NONE),
                new CardFrame("Giant", 5, 11, false, false, 0, 0, false),
                List.of(
                    new AbilityFrame("Monk", 1, AbilityState.ON_COOLDOWN, 4_000, -1, true, false),
                    AbilityFrame.NONE),
                1_500),
            new SideFrame(
                1,
                0,
                0,
                0,
                List.of(CardFrame.NONE, CardFrame.NONE),
                CardFrame.NONE,
                List.of(AbilityFrame.NONE, AbilityFrame.NONE),
                0));
  }

  @Test
  @DisplayName(
      "a card asks for an evolution count only where it can evolve: not outside an evolution slot,"
          + " nor in a hero slot too")
  void evolutionNeedFollowsTheSlot() {
    GameTables tables = GameTables.loadConfigured();
    assertThat(skeletonArmyNeed(tables, MatchSide.EVOLUTION_SLOT)).isPositive();
    assertThat(skeletonArmyNeed(tables, 0)).isZero();
    assertThat(skeletonArmyNeed(tables, MatchSide.EVOLUTION_SLOT | MatchSide.HERO_SLOT)).isZero();
  }

  /**
   * What a frame says SkeletonArmy asks for in the given slots. The shuffle holds the Mirrors back
   * while it draws the first four, so the card opens in hand.
   */
  private static int skeletonArmyNeed(GameTables tables, int slotFlags) {
    List<String> deck =
        List.of(
            "SkeletonArmy", "Archer", "Mirror", "Mirror", "Mirror", "Mirror", "Mirror", "Mirror");
    int[] slots = new int[deck.size()];
    slots[0] = slotFlags;
    BattleSession session = BattleSession.ladder(tables, deck, slots, deck, new int[deck.size()]);
    return frameOf(session).sides().get(0).hand().stream()
        .filter(card -> card.name().equals("SkeletonArmy"))
        .findFirst()
        .orElseThrow()
        .evolutionNeed();
  }

  @Test
  @DisplayName("reading a frame after every tick leaves the battle as it plays without them")
  void readingChangesNothing() throws IOException {
    GameTables tables = GameTables.loadConfigured();
    ReplayFile replay =
        ReplayFile.read(Path.of(System.getProperty("crforge.exampleReplay")), tables);
    assumeTrue(
        replay.playable(), "the example replay plays only on the tables of its own data version");

    ReplayPlayer read = new ReplayPlayer(replay, tables);
    List<TickFrame> heard = new ArrayList<>();
    List<Integer> readStates = new ArrayList<>();
    read.getSession()
        .setStepListener(
            () -> {
              TickFrame frame = frameOf(read.getSession());
              assertFollowsTheMatch(frame, read.getSession());
              heard.add(frame);
              readStates.add(state(read.getSession()));
            });
    while (read.step()) {
      // to the replay's end
    }

    ReplayPlayer unread = new ReplayPlayer(replay, tables);
    List<Integer> unreadStates = new ArrayList<>();
    unread.getSession().setStepListener(() -> unreadStates.add(state(unread.getSession())));
    while (unread.step()) {
      // to the replay's end
    }

    assertThat(heard).isNotEmpty();
    assertThat(unreadStates).as("the battle's state at every tick").isEqualTo(readStates);
    assertThat(unread.getSession().tick()).isEqualTo(read.getSession().tick());
    assertThat(frameOf(unread.getSession())).isEqualTo(heard.get(heard.size() - 1));

    assertThat(heard).allSatisfy(frame -> assertThat(frame.sides()).hasSize(2));
    assertThat(heard).allSatisfy(frame -> assertThat(frame.elixirRate()).isPositive());
    assertThat(heard.get(0).sides().get(0).hand()).hasSize(4);
    assertThat(heard.stream().flatMap(frame -> frame.entities().stream()))
        .as("a projectile in flight is off the ground")
        .anyMatch(entity -> entity.kind() == EntityKind.PROJECTILE && entity.height() > 0);
  }

  /** A session's frame as it stands, with no events told. */
  private static TickFrame frameOf(BattleSession session) {
    return TickFrames.of(BattleAdapter.frame(session), List.of(), List.of());
  }

  /** A frame says what the match holds, read straight from the core and the session. */
  private static void assertFollowsTheMatch(TickFrame frame, BattleSession session) {
    LadderMatch match = session.match();
    Timeline timeline = match.getTimeline();
    assertThat(frame.section()).isEqualTo(timeline.getSection());
    assertThat(frame.sectionLeftMs()).isEqualTo(timeline.sectionLeftMs());
    assertThat(frame.ended()).as("the end freezes the timeline").isEqualTo(timeline.isFrozen());
    assertThat(frame.endTimerMs()).isEqualTo(match.getEndTimerMs());
    assertThat(frame.tiebreakMs()).isEqualTo(match.getTiebreakMs());
    assertThat(frame.nextCardCooldownMs()).isEqualTo(timeline.getNextCardCooldownMs());
    for (int side = 0; side < match.playerCount(); side++) {
      MatchSide matchSide = match.side(side);
      SideFrame sideFrame = frame.sides().get(side);
      assertThat(sideFrame.nextCardCooldownLeftMs()).isEqualTo(matchSide.getHand().getCooldownMs());
      for (int slot = 0; slot < Hand.SLOTS; slot++) {
        int index = matchSide.getHand().slots()[slot];
        CardFrame card = sideFrame.hand().get(slot);
        if (index == Hand.EMPTY) {
          assertThat(card).isEqualTo(CardFrame.NONE);
        } else {
          assertThat(card.name()).isEqualTo(matchSide.deck().get(index).name());
          assertThat(card.evolutionCount()).isEqualTo(matchSide.evolutionCount(index));
          assertThat(card.evolutionNeed()).isEqualTo(evolutionNeed(matchSide, index));
          assertThat(card.evolved())
              .isEqualTo(matchSide.item(index).field() == EvolutionItem.EVOLVED);
          assertThat(card.pending()).as("a replay's plays are never pending").isFalse();
        }
      }
      assertThat(sideFrame.next().evolutionNeed())
          .isEqualTo(evolutionNeed(matchSide, matchSide.getHand().queue().get(0)));
      for (int slot = 0; slot < BattleSession.ABILITY_SLOTS; slot++) {
        BattleFrame.AbilityView ability = session.abilityView(side, slot);
        AbilityFrame abilityFrame = sideFrame.abilities().get(slot);
        if (ability == null) {
          assertThat(abilityFrame).isEqualTo(AbilityFrame.NONE);
        } else {
          assertThat(abilityFrame.champion()).isEqualTo(ability.champion());
          assertThat(abilityFrame.cooldownMs()).isEqualTo(ability.cooldownMs());
          assertThat(abilityFrame.state()).isEqualTo(TickFrames.abilityState(ability.state()));
        }
      }
    }
  }

  /** The count a deck card evolves at, from its row, 0 unless it alone holds an evolution slot. */
  private static int evolutionNeed(MatchSide side, int index) {
    MatchCard card = side.deck().get(index);
    MatchCard evolved = card.formRow(MatchCard.EVO_FORM);
    return side.slotFlags(index) == MatchSide.EVOLUTION_SLOT && evolved != card
        ? evolved.darkElixirCost()
        : 0;
  }

  /** A digest of the battle read straight from the core, through nothing a plugin's frame uses. */
  private static int state(BattleSession session) {
    int hash = session.tick();
    for (BattleEntity entity : session.getBattle().getBattle().getHolder().entities()) {
      hash = 31 * hash + entity.getId();
      if (entity instanceof WorldEntity world) {
        hash = 31 * hash + world.getView().getX();
        hash = 31 * hash + world.getView().getY();
        hash = 31 * hash + (world.getHitPoints() == null ? 0 : world.getHitPoints().getHitPoints());
      } else if (entity instanceof ProjectileEntity projectile) {
        hash = 31 * hash + projectile.getX();
        hash = 31 * hash + projectile.getY();
        hash = 31 * hash + projectile.getZ();
      } else if (entity instanceof AreaEffectEntity area) {
        hash = 31 * hash + area.getX();
        hash = 31 * hash + area.getY();
      }
    }
    LadderMatch match = session.match();
    for (int side = 0; side < match.playerCount(); side++) {
      hash = 31 * hash + match.side(side).getElixir();
      hash = 31 * hash + match.crowns(side);
    }
    return 31 * hash + match.getTimeline().getTimeMs();
  }
}
