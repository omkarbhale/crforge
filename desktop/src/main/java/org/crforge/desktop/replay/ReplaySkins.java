/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.replay;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A replay's tower skin data, read as it stands: its {@code rndSeed} and each battle player's
 * {@code battle.hbd[i].sk}. Nothing of it is consumed by the battle core, which carries {@code sk}
 * without reading it; it is read here only to be passed on to a presentation plugin, with no rule
 * of the game's client applied.
 *
 * @param rndSeed the replay's {@code rndSeed}, or 0 when it gives none as an int
 * @param players one entry for each {@code battle.hbd} entry that gives an {@code sk} as the client
 *     reads it, in player order
 */
public record ReplaySkins(int rndSeed, List<PlayerSkins> players) {

  /** None: what a replay with no {@code rndSeed} and no {@code sk} reads as. */
  public static final ReplaySkins NONE = new ReplaySkins(0, List.of());

  public ReplaySkins {
    players = List.copyOf(players);
  }

  /**
   * One battle player's {@code sk}, unchanged.
   *
   * @param player its place in {@code battle.hbd}
   * @param os its {@code os} list, empty when it gives none
   * @param ss its {@code ss} list, empty when it gives none
   * @param ats its {@code ats}, 0 when it gives none
   * @param rnd its {@code rnd}, false when it gives none
   */
  public record PlayerSkins(int player, List<Integer> os, List<Integer> ss, int ats, boolean rnd) {

    public PlayerSkins {
      os = List.copyOf(os);
      ss = List.copyOf(ss);
    }
  }

  /**
   * Reads a replay document's tower skin data. An {@code sk} that is not an object, or whose fields
   * are not of the types the client reads (lists of ints, an int, a boolean), is passed over: what
   * it holds cannot be passed on unchanged.
   *
   * @param document the replay document
   * @return its data; {@link #NONE}'s seed and no player when it gives none
   */
  public static ReplaySkins of(JsonNode document) {
    JsonNode seed = document.path("rndSeed");
    List<PlayerSkins> players = new ArrayList<>();
    JsonNode entries = document.path("battle").path("hbd");
    if (entries.isArray()) {
      for (int i = 0; i < entries.size(); i++) {
        player(i, entries.get(i).path("sk")).ifPresent(players::add);
      }
    }
    return new ReplaySkins(seed.isInt() ? seed.asInt() : 0, players);
  }

  private static Optional<PlayerSkins> player(int index, JsonNode sk) {
    if (!sk.isObject()) {
      return Optional.empty();
    }
    Optional<List<Integer>> os = ids(sk.path("os"));
    Optional<List<Integer>> ss = ids(sk.path("ss"));
    JsonNode ats = sk.path("ats");
    JsonNode rnd = sk.path("rnd");
    if (os.isEmpty()
        || ss.isEmpty()
        || !(ats.isMissingNode() || ats.isInt())
        || !(rnd.isMissingNode() || rnd.isBoolean())) {
      return Optional.empty();
    }
    return Optional.of(
        new PlayerSkins(index, os.get(), ss.get(), ats.asInt(0), rnd.asBoolean(false)));
  }

  /** A list of ints as it stands, empty when it is missing, or none when it is not one. */
  private static Optional<List<Integer>> ids(JsonNode list) {
    if (list.isMissingNode()) {
      return Optional.of(List.of());
    }
    if (!list.isArray()) {
      return Optional.empty();
    }
    List<Integer> ids = new ArrayList<>(list.size());
    for (JsonNode id : list) {
      if (!id.isInt()) {
        return Optional.empty();
      }
      ids.add(id.asInt());
    }
    return Optional.of(ids);
  }
}
