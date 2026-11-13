/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import java.util.ArrayList;
import java.util.List;
import org.crforge.core.battle.match.EvolutionItem;
import org.crforge.core.battle.match.MatchCard;
import org.crforge.core.battle.unit.CharacterEntity;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.crforge.core.battle.unit.Standard1v1Battle.Command;
import org.crforge.core.battle.unit.Standard1v1Battle.Play;
import org.crforge.presentation.CardForm;
import org.crforge.presentation.CardPlayFrame;

/**
 * Tells a plugin the card plays a battle ran since the last drain, as values copied from the
 * battle's own list of plays ({@link Standard1v1Battle#getPlays}), which every play appends to as
 * it runs, whoever gave it. It only reads that list and the battle's commands, and attaches nothing
 * to the battle.
 */
final class TickPlays {

  private final Standard1v1Battle battle;

  /** How many of the battle's plays have been drained or passed over. */
  private int read;

  /**
   * Starts at the battle as it stands: the plays already run are not told, as a battle's first
   * frame tells none of the ticks run to reach it.
   */
  TickPlays(Standard1v1Battle battle) {
    this.battle = battle;
    this.read = battle.getPlays().size();
  }

  /** The plays that ran since the last drain and put their card down, in the order they ran. */
  List<CardPlayFrame> drain() {
    List<Play> plays = battle.getPlays();
    if (read == plays.size()) {
      return List.of();
    }
    List<CardPlayFrame> drained = new ArrayList<>();
    for (Play play : plays.subList(read, plays.size())) {
      if (play.result() != null && play.result().placed()) {
        drained.add(frame(play));
      }
    }
    read = plays.size();
    return drained;
  }

  private CardPlayFrame frame(Play play) {
    List<Integer> unitIds = new ArrayList<>(play.units().size());
    for (CharacterEntity unit : play.units()) {
      unitIds.add(unit.getId());
    }
    return new CardPlayFrame(
        play.side(), card(play), form(play), play.result().x(), play.result().y(), unitIds);
  }

  /** The card the play's command named: the hand card the player played. */
  private String card(Play play) {
    List<Command> commands = battle.getCommands();
    for (int i = commands.size() - 1; i >= 0; i--) {
      Command command = commands.get(i);
      if (command.name().equals(play.name()) && command.card() != null) {
        return command.card();
      }
    }
    // Every play is queued with its command; a play's own name is the next best thing.
    return play.name();
  }

  /** The form the play put its card down in, from the item it carried; basic outside a match. */
  static CardForm form(Play play) {
    if (play.evolution() != null) {
      return form(play.evolution().field());
    }
    if (play.mirror() != null && play.mirror().repeats() != null) {
      return form(play.mirror().repeats().form());
    }
    return CardForm.BASIC;
  }

  /**
   * An item's evolution field or a row's form as a form: the two count alike ({@link
   * EvolutionItem#EVOLVED} is {@link MatchCard#EVO_FORM}, {@link EvolutionItem#HERO} is {@link
   * MatchCard#HERO_FORM}).
   */
  static CardForm form(int field) {
    return switch (field) {
      case EvolutionItem.EVOLVED -> CardForm.EVOLVED;
      case EvolutionItem.HERO -> CardForm.HERO;
      default -> CardForm.BASIC;
    };
  }
}
