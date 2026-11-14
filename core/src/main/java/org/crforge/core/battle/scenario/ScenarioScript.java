/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.core.battle.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.crforge.core.battle.Battle;
import org.crforge.core.battle.data.BattleRecords;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.battle.deploy.DeployCard;
import org.crforge.core.battle.match.MatchCard;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.crforge.core.pathfinding.grid.TileMap;
import org.crforge.core.util.GameUnits;

/**
 * A hand-written scenario: a battle set up outside any match, in which named card rows are played
 * by either side at named points and ticks. There is no hand, no elixir, no deck and no evolution
 * count: a card is played as the row it names, as often as the file lists it, and an evolved or
 * hero form is named by its own row (or by {@code "form"}). The battle has the standard arena and
 * both sides' towers, and runs until the scenario's end tick, as no match ends it.
 *
 * <p>The file is JSON and is told apart from a replay by its top-level {@code "crforgeScenario"}
 * field, the format's version. The format is documented in {@code examples/scenarios/README.md}.
 * Every problem the reader finds is listed, each naming the field it is in ({@link
 * ScenarioScriptException}).
 *
 * <p>A scenario is not a battle the game could have recorded: it is a tool for watching units
 * against each other.
 */
public record ScenarioScript(
    String name,
    String description,
    int seed,
    List<Standard1v1Battle.Towers> towers,
    boolean towersAttack,
    int endTick,
    List<Play> plays) {

  /** The top-level field that marks a scenario file, holding the format version. */
  public static final String MARKER = "crforgeScenario";

  /** The only format version so far. */
  public static final int VERSION = 1;

  /** The level cards and towers stand at when the file names none. */
  public static final int DEFAULT_LEVEL = Standard1v1Battle.DEFAULT_LEVEL;

  /** The highest card or tower level the file may name. */
  public static final int MAX_LEVEL = 16;

  /** Battle steps a second. */
  public static final int TICKS_PER_SECOND = 1000 / Battle.STEP_MS;

  /** The end tick when the file names none: three minutes. */
  public static final int DEFAULT_END_TICK = 180 * TICKS_PER_SECOND;

  /** Ticks between a play's drop and its deploy, as the game runs every play. */
  public static final int DROP_DELAY = Standard1v1Battle.PLAY_DELAY_TICKS;

  /** The most plays a file may expand to, a guard against a typo such as a count of 100000. */
  public static final int MAX_PLAYS = 2000;

  private static final String FORM_EVOLVED_SUFFIX = "_EV1";
  private static final String FORM_HERO_SUFFIX = "_hero";

  private static final List<String> CARD_TABLES =
      List.of(
          "spells_characters",
          "spells_buildings",
          "spells_other",
          "spells_evolved",
          "spells_hero_form");

  private static final Set<String> TOP_FIELDS =
      Set.of(MARKER, "name", "description", "seed", "towers", "endTick", "endTime", "plays");
  private static final Set<String> TOWER_FIELDS = Set.of("level", "kingLevel", "attack");
  private static final Set<String> SIDE_TOWER_FIELDS = Set.of("level", "kingLevel");
  private static final Set<String> PLAY_FIELDS =
      Set.of(
          "side",
          "card",
          "form",
          "level",
          "tick",
          "time",
          "every",
          "everyTicks",
          "count",
          "at",
          "line",
          "grid",
          "comment");
  private static final Set<String> LINE_FIELDS = Set.of("from", "to");
  private static final Set<String> GRID_FIELDS = Set.of("from", "to", "columns", "rows");

  /**
   * One card play of the scenario, after its layout is expanded.
   *
   * @param name the play's name in the battle, {@code cmd<index>} in the order plays run
   * @param entry the index of the {@code plays} entry it came from
   * @param side 0 or 1; side 0's half of the arena is y 0 to 15 tiles, side 1's y 17 to 32
   * @param card the card row played
   * @param level the level it is played at, counted from 1
   * @param dropTick the tick the card is dropped on; it deploys {@link #DROP_DELAY} ticks later
   * @param x the point in game units across the arena
   * @param y the point in game units along the arena, from side 0's end
   */
  public record Play(
      String name, int entry, int side, String card, int level, int dropTick, int x, int y) {

    /** The tick the play deploys on. */
    public int runTick() {
      return dropTick + DROP_DELAY;
    }
  }

  /** Whether a JSON document is a scenario file rather than a replay. */
  public static boolean isScenario(JsonNode document) {
    return document != null && document.isObject() && document.has(MARKER);
  }

  /**
   * Reads and checks a scenario file against the game tables.
   *
   * @param document the file's JSON
   * @param tables the game tables, which name the cards
   * @return the scenario
   * @throws ScenarioScriptException listing every problem, each with the field it is in
   */
  public static ScenarioScript read(JsonNode document, GameTables tables) {
    return new Reader(tables).read(document);
  }

  /**
   * Builds the scenario's battle before its first step, every play queued: the towers at their
   * levels, the seed, and no match.
   *
   * @param tables the game tables the scenario was read against
   * @return the battle, not yet stepped
   */
  public Standard1v1Battle build(GameTables tables) {
    Standard1v1Battle battle = new Standard1v1Battle(tables, towers, towersAttack);
    battle.getWorld().seed(seed);
    for (Play play : plays) {
      battle.play(
          play.dropTick(),
          play.runTick(),
          battle.getWorld().getRecords().card(play.card()),
          play.level(),
          play.side(),
          play.x(),
          play.y(),
          play.name());
    }
    return battle;
  }

  /** The cards each side plays, each once, in the order they first appear. */
  public List<List<String>> cardsBySide() {
    List<List<String>> sides = List.of(new ArrayList<>(), new ArrayList<>());
    for (Play play : plays) {
      List<String> side = sides.get(play.side());
      if (!side.contains(play.card())) {
        side.add(play.card());
      }
    }
    return List.of(List.copyOf(sides.get(0)), List.copyOf(sides.get(1)));
  }

  /** Reads one document, collecting every problem. */
  private static final class Reader {
    private final GameTables tables;
    private final List<String> errors = new ArrayList<>();
    private BattleRecords records;

    Reader(GameTables tables) {
      this.tables = tables;
    }

    ScenarioScript read(JsonNode root) {
      if (root == null || !root.isObject()) {
        throw new ScenarioScriptException(List.of("the file is not a JSON object"));
      }
      JsonNode marker = root.path(MARKER);
      if (!marker.isInt() || marker.asInt() != VERSION) {
        errors.add(
            MARKER + ": must be " + VERSION + " (the format version), got " + marker.toString());
      }
      unknownFields(root, TOP_FIELDS, "");
      String name = text(root, "name", "");
      String description = text(root, "description", "");
      int seed = 0;
      if (root.has("seed")) {
        if (root.get("seed").isInt()) {
          seed = root.get("seed").asInt();
        } else {
          errors.add("seed: must be a whole number, got " + root.get("seed"));
        }
      }
      List<Standard1v1Battle.Towers> towers = new ArrayList<>();
      boolean attack = towers(root.path("towers"), towers);
      int endTick = endTick(root);
      List<Pending> pending = new ArrayList<>();
      JsonNode plays = root.path("plays");
      if (!plays.isArray()) {
        errors.add(
            "plays: must be a list of plays" + (plays.isMissingNode() ? ", it is missing" : ""));
      } else {
        for (int i = 0; i < plays.size(); i++) {
          play(plays.get(i), i, pending);
        }
      }
      if (pending.size() > MAX_PLAYS) {
        errors.add(
            "plays: expand to "
                + pending.size()
                + " card plays, more than the "
                + MAX_PLAYS
                + " a scenario may hold");
      }
      for (Pending play : pending) {
        if (play.dropTick() + DROP_DELAY > endTick) {
          errors.add(
              "plays["
                  + play.entry()
                  + "]: deploys on tick "
                  + (play.dropTick() + DROP_DELAY)
                  + ", after the scenario's end tick "
                  + endTick);
          break;
        }
      }
      if (!errors.isEmpty()) {
        throw new ScenarioScriptException(errors);
      }
      // Plays run in drop order; within a tick, in the file's order.
      pending.sort(Comparator.comparingInt(Pending::dropTick));
      List<Play> out = new ArrayList<>();
      for (int i = 0; i < pending.size(); i++) {
        Pending p = pending.get(i);
        out.add(
            new Play(
                "cmd" + i, p.entry(), p.side(), p.card(), p.level(), p.dropTick(), p.x(), p.y()));
      }
      return new ScenarioScript(name, description, seed, towers, attack, endTick, out);
    }

    private record Pending(
        int entry, int side, String card, int level, int dropTick, int x, int y) {}

    /**
     * The towers: one object for both sides, or a list of two (side 0, then side 1), each with a
     * level and a king level; {@code attack}, which holds every tower's fire when false, is given
     * once at the top of the object or in neither side of the list but in a single object.
     */
    private boolean towers(JsonNode node, List<Standard1v1Battle.Towers> out) {
      out.add(defaultTowers());
      out.add(defaultTowers());
      if (node.isMissingNode()) {
        return true;
      }
      if (node.isObject()) {
        unknownFields(node, TOWER_FIELDS, "towers.");
        Standard1v1Battle.Towers towers = sideTowers(node, "towers");
        out.set(0, towers);
        out.set(1, towers);
        if (!node.has("attack")) {
          return true;
        }
        if (!node.get("attack").isBoolean()) {
          errors.add("towers.attack: must be true or false" + got(node.get("attack")));
          return true;
        }
        return node.get("attack").asBoolean();
      }
      if (node.isArray() && node.size() == 2) {
        for (int side = 0; side < 2; side++) {
          String path = "towers[" + side + "]";
          JsonNode t = node.get(side);
          if (!t.isObject()) {
            errors.add(path + ": must be an object with level and kingLevel");
            continue;
          }
          unknownFields(t, SIDE_TOWER_FIELDS, path + ".");
          out.set(side, sideTowers(t, path));
        }
        return true;
      }
      errors.add("towers: must be an object, or a list of two objects (side 0, then side 1)");
      return true;
    }

    private Standard1v1Battle.Towers sideTowers(JsonNode t, String path) {
      int level = level(t, "level", path + ".level", DEFAULT_LEVEL);
      int king = level(t, "kingLevel", path + ".kingLevel", level);
      return new Standard1v1Battle.Towers(Standard1v1Battle.PRINCESS_TOWERS, king, level);
    }

    private static Standard1v1Battle.Towers defaultTowers() {
      return new Standard1v1Battle.Towers(
          Standard1v1Battle.PRINCESS_TOWERS, DEFAULT_LEVEL, DEFAULT_LEVEL);
    }

    private int endTick(JsonNode root) {
      if (root.has("endTick") && root.has("endTime")) {
        errors.add("endTick, endTime: give one of them, not both");
        return DEFAULT_END_TICK;
      }
      if (root.has("endTick")) {
        return ticks(root.get("endTick"), "endTick", 1, DEFAULT_END_TICK);
      }
      if (root.has("endTime")) {
        return seconds(root.get("endTime"), "endTime", 1, DEFAULT_END_TICK);
      }
      return DEFAULT_END_TICK;
    }

    private void play(JsonNode node, int i, List<Pending> out) {
      String path = "plays[" + i + "]";
      if (!node.isObject()) {
        errors.add(path + ": must be an object");
        return;
      }
      int before = errors.size();
      unknownFields(node, PLAY_FIELDS, path + ".");
      int side = side(node.path("side"), path + ".side");
      String card = card(node, path);
      int level = level(node, "level", path + ".level", DEFAULT_LEVEL);
      int drop = 0;
      if (node.has("tick") && node.has("time")) {
        errors.add(path + ": give tick or time, not both");
      } else if (node.has("tick")) {
        drop = ticks(node.get("tick"), path + ".tick", 0, 0);
      } else if (node.has("time")) {
        drop = seconds(node.get("time"), path + ".time", 0, 0);
      }
      int every = 0;
      if (node.has("every") && node.has("everyTicks")) {
        errors.add(path + ": give every or everyTicks, not both");
      } else if (node.has("everyTicks")) {
        every = ticks(node.get("everyTicks"), path + ".everyTicks", 0, 0);
      } else if (node.has("every")) {
        every = seconds(node.get("every"), path + ".every", 0, 0);
      }
      List<int[]> points = points(node, path);
      if (errors.size() > before) {
        return;
      }
      for (int k = 0; k < points.size(); k++) {
        int[] p = points.get(k);
        out.add(new Pending(i, side, card, level, drop + k * every, p[0], p[1]));
      }
    }

    private int side(JsonNode node, String path) {
      if (node.isInt() && (node.asInt() == 0 || node.asInt() == 1)) {
        return node.asInt();
      }
      errors.add(
          path + ": must be 0 (the side whose half is y 0 to 15) or 1 (y 17 to 32)" + got(node));
      return 0;
    }

    private String card(JsonNode node, String path) {
      JsonNode card = node.path("card");
      if (!card.isTextual() || card.asText().isEmpty()) {
        errors.add(path + ".card: must name a card row, such as \"Knight\" or \"Skeletons_EV1\"");
        return null;
      }
      String name = card.asText();
      if (node.has("form")) {
        JsonNode form = node.get("form");
        String f = form.isTextual() ? form.asText().toLowerCase(Locale.ROOT) : "";
        switch (f) {
          case "basic" -> {}
          case "evolved", "evo" -> name = name + FORM_EVOLVED_SUFFIX;
          case "hero" -> name = name + FORM_HERO_SUFFIX;
          default -> {
            errors.add(path + ".form: must be \"basic\", \"evolved\" or \"hero\", got " + form);
            return null;
          }
        }
      }
      if (tableOf(name) == null) {
        StringBuilder message =
            new StringBuilder(path + ".card: the game tables have no card row \"" + name + "\"");
        if (node.has("form") && tableOf(card.asText()) != null) {
          message
              .append(": ")
              .append(card.asText())
              .append(" has no ")
              .append(node.get("form").asText())
              .append(" form");
        } else {
          List<String> near = nearest(name);
          if (!near.isEmpty()) {
            message.append("; did you mean ").append(String.join(", ", near)).append('?');
          }
        }
        errors.add(message.toString());
        return null;
      }
      // The battle core reads the card now, so a card it does not model is named here.
      try {
        if (records == null) {
          records = new BattleRecords(tables);
        }
        MatchCard match = records.matchCard(name);
        if (match.mirror()) {
          errors.add(
              path + ".card: " + name + " repeats its side's last card, which needs a match");
          return null;
        }
        if (match.variant() != null) {
          errors.add(
              path
                  + ".card: "
                  + name
                  + " is played as an option picked from the elixir, which needs a match; name"
                  + " the option's own card row instead");
          return null;
        }
        DeployCard deploy = records.card(name);
        if (deploy == null) {
          errors.add(path + ".card: " + name + " cannot be played");
        }
      } catch (RuntimeException e) {
        errors.add(path + ".card: " + name + " cannot be played: " + e.getMessage());
      }
      return name;
    }

    private String tableOf(String name) {
      for (String table : CARD_TABLES) {
        if (tables.tableNames().contains(table) && tables.table(table).has(name)) {
          return table;
        }
      }
      return null;
    }

    /** Up to five card rows nearest a name: containing it either way, else by edit distance. */
    private List<String> nearest(String name) {
      String lower = name.toLowerCase(Locale.ROOT);
      TreeSet<String> all = new TreeSet<>();
      for (String table : CARD_TABLES) {
        if (tables.tableNames().contains(table)) {
          tables.table(table).rows().forEach(row -> all.add(row.name()));
        }
      }
      List<String> containing = new ArrayList<>();
      for (String row : all) {
        String r = row.toLowerCase(Locale.ROOT);
        if (r.contains(lower) || lower.contains(r)) {
          containing.add(row);
        }
      }
      // Words swapped, as MotherWitch for WitchMother, read as the same letters.
      String sorted = sortedLetters(lower);
      // The rows of the same letters first, then the rest shortest first.
      containing.sort(
          Comparator.comparing(
                  (String row) -> !sortedLetters(row.toLowerCase(Locale.ROOT)).equals(sorted))
              .thenComparingInt(String::length));
      for (String row : all) {
        if (sortedLetters(row.toLowerCase(Locale.ROOT)).equals(sorted)
            && !containing.contains(row)) {
          containing.add(0, row);
        }
      }
      if (!containing.isEmpty()) {
        return containing.subList(0, Math.min(5, containing.size()));
      }
      List<String> ranked = new ArrayList<>(all);
      ranked.sort(Comparator.comparingInt(row -> distance(lower, row.toLowerCase(Locale.ROOT))));
      List<String> near = new ArrayList<>();
      for (String row : ranked) {
        if (near.size() == 3 || distance(lower, row.toLowerCase(Locale.ROOT)) > 3) {
          break;
        }
        near.add(row);
      }
      return near;
    }

    private static String sortedLetters(String s) {
      char[] chars = s.toCharArray();
      java.util.Arrays.sort(chars);
      return new String(chars);
    }

    private static int distance(String a, String b) {
      int[] prev = new int[b.length() + 1];
      int[] cur = new int[b.length() + 1];
      for (int j = 0; j <= b.length(); j++) {
        prev[j] = j;
      }
      for (int i = 1; i <= a.length(); i++) {
        cur[0] = i;
        for (int j = 1; j <= b.length(); j++) {
          int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
          cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
        }
        int[] swap = prev;
        prev = cur;
        cur = swap;
      }
      return prev[b.length()];
    }

    /** The play's points in game units, from {@code at}, {@code line} or {@code grid}. */
    private List<int[]> points(JsonNode node, String path) {
      int layouts =
          (node.has("at") ? 1 : 0) + (node.has("line") ? 1 : 0) + (node.has("grid") ? 1 : 0);
      if (layouts != 1) {
        errors.add(
            path
                + ": give exactly one of at (a point), line (count points from one point to"
                + " another) or grid (columns by rows)"
                + (layouts == 0 ? ", none is given" : ", " + layouts + " are given"));
        return List.of();
      }
      Integer count = null;
      if (node.has("count")) {
        JsonNode c = node.get("count");
        if (c.isInt() && c.asInt() >= 1) {
          count = c.asInt();
        } else {
          errors.add(path + ".count: must be a whole number of at least 1, got " + c);
          return List.of();
        }
      }
      List<int[]> points = new ArrayList<>();
      if (node.has("at")) {
        double[] at = point(node.get("at"), path + ".at");
        if (at == null) {
          return List.of();
        }
        for (int k = 0; k < (count == null ? 1 : count); k++) {
          points.add(units(at));
        }
      } else if (node.has("line")) {
        JsonNode line = node.get("line");
        if (!line.isObject()) {
          errors.add(path + ".line: must be an object with from and to");
          return List.of();
        }
        unknownFields(line, LINE_FIELDS, path + ".line.");
        double[] from = point(line.path("from"), path + ".line.from");
        double[] to = point(line.path("to"), path + ".line.to");
        if (count == null) {
          errors.add(path + ".count: a line needs a count, the number of points along it");
          return List.of();
        }
        if (from == null || to == null) {
          return List.of();
        }
        for (int k = 0; k < count; k++) {
          double f = count == 1 ? 0 : (double) k / (count - 1);
          points.add(units(new double[] {lerp(from[0], to[0], f), lerp(from[1], to[1], f)}));
        }
      } else {
        JsonNode grid = node.get("grid");
        if (!grid.isObject()) {
          errors.add(path + ".grid: must be an object with from, to, columns and rows");
          return List.of();
        }
        unknownFields(grid, GRID_FIELDS, path + ".grid.");
        double[] from = point(grid.path("from"), path + ".grid.from");
        double[] to = point(grid.path("to"), path + ".grid.to");
        int columns = positive(grid.path("columns"), path + ".grid.columns");
        int rows = positive(grid.path("rows"), path + ".grid.rows");
        if (from == null || to == null || columns < 1 || rows < 1) {
          return List.of();
        }
        int cells = columns * rows;
        if (count != null && count > cells) {
          errors.add(path + ".count: " + count + " is more than the grid's " + cells + " points");
          return List.of();
        }
        int n = count == null ? cells : count;
        // Row by row from the from corner, each row left to right as from to to.
        for (int k = 0; k < n; k++) {
          int col = k % columns;
          int row = k / columns;
          double fx = columns == 1 ? 0 : (double) col / (columns - 1);
          double fy = rows == 1 ? 0 : (double) row / (rows - 1);
          points.add(units(new double[] {lerp(from[0], to[0], fx), lerp(from[1], to[1], fy)}));
        }
      }
      return points;
    }

    private int positive(JsonNode node, String path) {
      if (node.isInt() && node.asInt() >= 1) {
        return node.asInt();
      }
      errors.add(path + ": must be a whole number of at least 1" + got(node));
      return 0;
    }

    private static double lerp(double a, double b, double f) {
      return a + (b - a) * f;
    }

    private static int[] units(double[] tiles) {
      return new int[] {
        (int) Math.round(tiles[0] * GameUnits.UNITS_PER_TILE),
        (int) Math.round(tiles[1] * GameUnits.UNITS_PER_TILE)
      };
    }

    /** A point [x, y] in tiles, inside the arena. */
    private double[] point(JsonNode node, String path) {
      if (!node.isArray()
          || node.size() != 2
          || !node.get(0).isNumber()
          || !node.get(1).isNumber()) {
        errors.add(path + ": must be a point [x, y] in tiles" + got(node));
        return null;
      }
      double x = node.get(0).asDouble();
      double y = node.get(1).asDouble();
      TileMap map = TileMap.standard1v1();
      double width = (double) map.widthUnits() / GameUnits.UNITS_PER_TILE;
      double height = (double) map.heightUnits() / GameUnits.UNITS_PER_TILE;
      if (x < 0 || x > width || y < 0 || y > height) {
        errors.add(
            path
                + ": "
                + node
                + " is outside the arena, x 0 to "
                + fmt(width)
                + ", y 0 to "
                + fmt(height)
                + " tiles");
        return null;
      }
      return new double[] {x, y};
    }

    private static String fmt(double d) {
      return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private int level(JsonNode parent, String field, String path, int fallback) {
      if (!parent.has(field)) {
        return fallback;
      }
      JsonNode node = parent.get(field);
      if (node.isInt() && node.asInt() >= 1 && node.asInt() <= MAX_LEVEL) {
        return node.asInt();
      }
      errors.add(path + ": must be a whole number from 1 to " + MAX_LEVEL + got(node));
      return fallback;
    }

    private int ticks(JsonNode node, String path, int min, int fallback) {
      if (node.isInt() && node.asInt() >= min) {
        return node.asInt();
      }
      errors.add(path + ": must be a whole number of ticks of at least " + min + got(node));
      return fallback;
    }

    private int seconds(JsonNode node, String path, int minTicks, int fallback) {
      if (node.isNumber()) {
        int ticks = (int) Math.round(node.asDouble() * TICKS_PER_SECOND);
        if (ticks >= minTicks) {
          return ticks;
        }
      }
      errors.add(
          path
              + ": must be a number of seconds of at least "
              + fmt((double) minTicks / TICKS_PER_SECOND)
              + got(node));
      return fallback;
    }

    private String text(JsonNode root, String field, String fallback) {
      if (!root.has(field)) {
        return fallback;
      }
      if (!root.get(field).isTextual()) {
        errors.add(field + ": must be text");
        return fallback;
      }
      return root.get(field).asText();
    }

    private void unknownFields(JsonNode node, Set<String> known, String prefix) {
      node.fieldNames()
          .forEachRemaining(
              field -> {
                if (!known.contains(field)) {
                  errors.add(
                      prefix
                          + field
                          + ": not a field of the scenario format here; known: "
                          + String.join(", ", new TreeSet<>(known)));
                }
              });
    }

    private static String got(JsonNode node) {
      return node.isMissingNode() ? ", it is missing" : ", got " + node;
    }
  }

  /** The scenario's plays by name, each naming its card, for messages. */
  public Map<String, String> playCards() {
    Map<String, String> cards = new java.util.LinkedHashMap<>();
    plays.forEach(play -> cards.put(play.name(), play.card()));
    return cards;
  }
}
