/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.battle;

import java.util.List;
import org.crforge.core.battle.unit.ChampionController;

/**
 * What the visualizer draws of a battle core session at one moment, read by {@link BattleAdapter}
 * between steps.
 *
 * @param tick the battle's count of completed steps
 * @param timeMs the battle time: the match clock in a match, else the battle's own clock
 * @param overtime whether the match is in overtime
 * @param elixirRate the elixir rate as a multiple of the opening rate (1, 2 or 3), 0 outside a
 *     match
 * @param entities every entity of the holder's live list in its own order, mapped to its kind;
 *     objects that stand nowhere on the arena are left out
 * @param sides side 0 then side 1 in a match; empty outside one
 * @param ended whether the match has ended
 * @param winner the winning side, or -1 for a draw, once the match has ended
 * @param over whether the battle has stopped stepping by its own rule
 * @param halted why the session stopped stepping on a refusal, or null
 * @param messages the session's latest messages, oldest first
 * @param section the match timeline's current section, counted from 0; 0 outside a match
 * @param sectionLeftMs the time left in that section in milliseconds, 0 for a section without a
 *     length and outside a match
 * @param frozen whether the match's end has frozen its timeline: the section and {@code
 *     sectionLeftMs} stop, while {@code timeMs} keeps following the ticks
 * @param endTimerMs the match's end timer: 0 until it ends, then rising
 * @param tiebreakMs the tiebreaker's time: 0 until it begins, then rising
 * @param nextCardCooldownMs the length a hand's refill countdown restarts at after filling a slot,
 *     in milliseconds
 */
public record BattleFrame(
    int tick,
    int timeMs,
    boolean overtime,
    int elixirRate,
    List<EntityView> entities,
    List<SideView> sides,
    boolean ended,
    int winner,
    boolean over,
    String halted,
    List<String> messages,
    int section,
    int sectionLeftMs,
    boolean frozen,
    int endTimerMs,
    int tiebreakMs,
    int nextCardCooldownMs) {

  /**
   * One side of a match.
   *
   * @param side 0 or 1
   * @param elixir the elixir in ten-thousandths, as the match holds it
   * @param wholeElixir the whole elixir a play is checked against
   * @param crowns the crowns the side has taken
   * @param hand the four hand slots, null for an empty slot
   * @param next the card that refills the hand next, or null
   * @param abilities the king's two champion slots, null for a slot that follows no champion
   * @param nextCardCooldownLeftMs what is left of the hand's refill countdown, in milliseconds: an
   *     empty slot refills when it runs out. It also runs with a full hand, and a slot emptied with
   *     it at 0 refills on its play's own step
   */
  public record SideView(
      int side,
      int elixir,
      int wholeElixir,
      int crowns,
      List<CardView> hand,
      CardView next,
      List<AbilityView> abilities,
      int nextCardCooldownLeftMs) {}

  /**
   * One card of a hand.
   *
   * @param name the card row name
   * @param cost its own row's cost in whole elixir, which need not be what a play of it spends: a
   *     Mirror's play adds the card it repeats, and an evolved, hero or variant play costs its own
   *     row or option
   * @param level the level its side plays it at, counted from 1 across all rarities
   * @param unavailableReason why selection is disabled, or null if available
   * @param pending whether it was played through the session's commands, from the screen or by a
   *     model playing its side, and waits for its play to run; a replay's queued plays never show
   *     it
   * @param evolutionCount the king's count for an evolution slot's card, 0 for any other: its plays
   *     since its last evolved or hero play, up to its need
   * @param evolutionNeed the count its evolved row asks for, 0 for a card that does not evolve: one
   *     outside an evolution slot, also in a hero slot, or without an evolved row
   * @param evolved whether its next play is evolved
   */
  public record CardView(
      String name,
      int cost,
      int level,
      boolean pending,
      String unavailableReason,
      int evolutionCount,
      int evolutionNeed,
      boolean evolved) {}

  /**
   * One champion slot of a king: the champion or hero it follows and its ability's button.
   *
   * @param champion the name of the deck card the slot's champion was found in, or the champion's
   *     row name when it was found in none
   * @param cost the ability's cost in whole elixir
   * @param state the slot's button state as the game works it out, one of {@link
   *     ChampionController}'s states
   * @param cooldownMs the slot's cooldown left, in milliseconds
   * @param charges the slot's charges left, or -1 when its uses are not counted
   * @param pending whether an ability command given from the screen for the slot waits to run
   * @param unavailableReason why the button cannot be tapped now, or null when it can
   */
  public record AbilityView(
      String champion,
      int cost,
      int state,
      int cooldownMs,
      int charges,
      boolean pending,
      String unavailableReason) {}
}
