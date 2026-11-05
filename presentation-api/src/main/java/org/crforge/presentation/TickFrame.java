/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import java.util.List;

/**
 * The state of a battle after a tick, detached from the battle: it never changes.
 *
 * @param tick how many ticks the battle has run. A tick's own events belong to the tick before this
 *     number: the frame reads the battle after its count moved on
 * @param timeMs the battle's clock in milliseconds
 * @param ended whether the match has ended. The end freezes the match's timeline: the section and
 *     {@code sectionLeftMs} stop, while {@code timeMs} keeps following the ticks
 * @param winner the side that won, 0 or 1, or -1 for none (yet)
 * @param entities every entity in the battle at this tick; one that left during the tick is absent
 * @param overtime whether the match is in overtime
 * @param elixirRate the elixir rate as a multiple of the opening rate (1, 2 or 3), 0 outside a
 *     match
 * @param sides side 0 then side 1 in a match; empty outside one
 * @param section the match's current section, counted from 0; 0 outside a match
 * @param sectionLeftMs the time left in that section in milliseconds, 0 for a section without a
 *     length and outside a match
 * @param endTimerMs the match's end timer: 0 until it ends, then rising
 * @param tiebreakMs the tiebreaker's time: 0 until it begins, then rising
 * @param nextCardCooldownMs the length a hand's refill countdown restarts at after filling a slot,
 *     in milliseconds
 * @param deaths what died in the tick just run, in the order it died. One that dies may still stand
 *     in {@code entities} until it leaves: this list, not its absence, tells a death. A battle's
 *     first frame carries none, whatever its tick: the ticks run to reach it are not told
 * @param strikes the attacks' own hits that landed in the tick just run, in the order they landed.
 *     A battle's first frame carries none, whatever its tick
 */
public record TickFrame(
    int tick,
    int timeMs,
    boolean ended,
    int winner,
    List<EntityFrame> entities,
    boolean overtime,
    int elixirRate,
    List<SideFrame> sides,
    int section,
    int sectionLeftMs,
    int endTimerMs,
    int tiebreakMs,
    int nextCardCooldownMs,
    List<DeathFrame> deaths,
    List<StrikeFrame> strikes) {

  public TickFrame {
    entities = List.copyOf(entities);
    sides = List.copyOf(sides);
    deaths = List.copyOf(deaths);
    strikes = List.copyOf(strikes);
  }

  /**
   * A frame with no events. Kept on purpose: crforge-render builds against this module live, and
   * its tests and harness tools construct frames with this signature, so removing it breaks them.
   */
  public TickFrame(
      int tick,
      int timeMs,
      boolean ended,
      int winner,
      List<EntityFrame> entities,
      boolean overtime,
      int elixirRate,
      List<SideFrame> sides,
      int section,
      int sectionLeftMs,
      int endTimerMs,
      int tiebreakMs,
      int nextCardCooldownMs) {
    this(
        tick,
        timeMs,
        ended,
        winner,
        entities,
        overtime,
        elixirRate,
        sides,
        section,
        sectionLeftMs,
        endTimerMs,
        tiebreakMs,
        nextCardCooldownMs,
        List.of(),
        List.of());
  }
}
