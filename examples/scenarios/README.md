# Scenarios

A scenario is a hand-written battle: you name the cards each side plays, where, and when, and the replay viewer plays it.

It is a Ladder match, and every rule of the match holds: the 3-minute clock, overtime, crowns, the king tower waking when a princess tower falls, and the end when a king tower falls or the time runs out. Placement follows the game's rules too. Only what limits *which card may be played when* is lifted: the hand, the elixir, the start cooldown and the evolution count gate no play. A card is played as the card row you name, as often as you list it, all on one tick if you like.

This is a tool for watching units against each other. It is not a battle the game could have recorded, and nothing checks it against one.

Scenario mode lives on the `scenario-mode` branch of the fork only.

## Running one

A scenario file opens like a replay, with `--replay` (or by dropping it on the viewer's window). The path must be absolute.

On Linux or WSL, from the repository root:

```bash
./gradlew :desktop:run -Pcrforge.assetSource=cdn --args="--replay $PWD/examples/scenarios/skeletons-evo-vs-mother-witch.json"
```

With the crforge-render plugin drawing it, add `--plugin <jar>` inside the same quotes:

```bash
./gradlew :desktop:run -Pcrforge.assetSource=cdn --args="--plugin /path/to/crforge-render-plugin-0.1.0.jar --replay $PWD/examples/scenarios/skeletons-evo-vs-mother-witch.json"
```

### On Windows

You need Git for Windows and a Java 21 JDK (for example `winget install Microsoft.OpenJDK.21`), with `JAVA_HOME` set to it. This follows crforge-render's README, "Run with a downloaded plugin".

1. Download `crforge-render-plugin-0.1.0.jar` and its `.sha256` from the crforge-render release (v0.1.0) into a folder whose path has no spaces, such as `C:\crforge-plugins`. To check it, in Command Prompt: `certutil -hashfile C:\crforge-plugins\crforge-render-plugin-0.1.0.jar SHA256`, which prints the hash in the `.sha256` file.
2. Clone the fork on this branch and launch the scenario, in Command Prompt:

```bat
cd C:\
git clone --branch scenario-mode https://github.com/omkarbhale/crforge.git
cd crforge
gradlew.bat :desktop:run -Pcrforge.assetSource=cdn --args="--plugin C:\crforge-plugins\crforge-render-plugin-0.1.0.jar --replay C:\crforge\examples\scenarios\skeletons-evo-vs-mother-witch.json"
```

If you already have a clone, run `git fetch origin` and then `git checkout scenario-mode` instead of cloning.

In PowerShell, run it as `.\gradlew.bat` and quote the property: `"-Pcrforge.assetSource=cdn"`.

The first run downloads Gradle and the game's files, and builds the game tables in `%USERPROFILE%\.crforge`. That takes a while. Later runs start at once. To open another scenario, drop its file on the window, or change the path after `--replay`.

### In the viewer

These are the replay viewer's own keys: SPACE pauses, `.` and `,` step one tick forward or back, `+` and `-` change the speed, R restarts from tick 0, and F flips the view.

The viewer opens with **side 1 at the bottom, drawn blue, and side 0 at the top, drawn red**, as it shows a replay. F flips it. The hand panels show each side's cards and the elixir bars fill as in a match, but neither limits the plays. The scenario plays until the match ends, or until its `endTime` if it gives one.

The recent events list each play the battle placed. A play the battle refuses is listed with the reason: one that could not be placed (a troop on a tower or the river, for example), or one that came after the match was decided or after its side's king fell. Once a king has fallen and the end screen's delay has passed, the battle stops and no later play runs.

If a file has problems, the viewer shows `REPLAY NOT PLAYED` with every problem, and the launch prints them too. Each problem starts with the field it is in, for example:

```
plays[1].card: the game tables have no card row "MotherWitch"; did you mean WitchMother, Witch?
plays[3].side: must be 0 (the side whose half is y 0 to 15) or 1 (y 17 to 32), got 2
plays[0].colour: not a field of the scenario format here; known: at, card, comment, count, ...
```

## The format

A scenario is a JSON object. Its `crforgeScenario` field marks it as a scenario rather than a replay.

```json
{
  "crforgeScenario": 1,
  "name": "Knight duel",
  "endTime": 30,
  "towers": { "attack": false },
  "plays": [
    { "side": 0, "card": "Knight", "at": [3.5, 13.5] },
    { "side": 1, "card": "Knight", "at": [3.5, 18.5] }
  ]
}
```

### Top-level fields

| Field | Meaning | Default |
|---|---|---|
| `crforgeScenario` | The format version. Must be `1`. | required |
| `name`, `description` | Text the viewer prints. | none |
| `seed` | The battle's random seed, a whole number. | `0` |
| `endTime` or `endTick` | Stops the scenario early, in seconds or in ticks (20 ticks a second). Give one or neither. | none: the match ends it |
| `towers` | The towers. See below. | level 11, fighting |
| `plays` | The list of plays. See below. | required |

A field the format does not know is a problem, so a misspelled field is reported rather than ignored.

### Towers

Both sides have their two princess towers and their king tower, on the standard arena.

- `"towers": { "level": 9, "kingLevel": 11, "attack": false }` sets both sides. `kingLevel` defaults to `level`. `"attack": false` makes every tower hold its fire for the whole battle.
- `"towers": [ { "level": 13 }, { "level": 7, "kingLevel": 12 } ]` sets side 0, then side 1. In this form there is no `attack`: every tower fights.

Levels run from 1 to 16 and are counted from 1 across all rarities, as crforge counts card levels. The default is 11.

### Plays

Each entry of `plays` is one card, played once or many times:

| Field | Meaning | Default |
|---|---|---|
| `side` | `0` or `1`. Side 0's half of the arena is y 0 to 15, side 1's is y 17 to 32. | required |
| `card` | The card row's name in the game tables, such as `Knight`, `WitchMother` (Mother Witch), `Skeletons_EV1` (evolved Skeletons) or `Knight_hero`. An unknown name is a problem that names the nearest rows. | required |
| `form` | `basic`, `evolved` or `hero`. It picks that form's row of `card`: `"card": "Skeletons", "form": "evolved"` is `Skeletons_EV1`, and `hero` adds `_hero`. | `basic` |
| `level` | The card's level, 1 to 16. | 11 |
| `time` or `tick` | When the card is dropped, in seconds or ticks from the start. It deploys 20 ticks (1 s) later, as every play does in the game. | 0 |
| `at`, `line` or `grid` | Where. Give exactly one. See below. | required |
| `count` | How many copies. | 1 (`at`), all points (`grid`) |
| `every` or `everyTicks` | The gap between successive copies, in seconds or ticks. | 0: all copies on one tick |
| `comment` | Free text, ignored. | none |

Positions are in tiles, as `[x, y]`. x runs 0 to 18 across the arena and y runs 0 to 32 along it, from side 0's end. Fractions are allowed. A tile's centre is at `.5`, so `[3.5, 10.5]` is the centre of the tile 3 across and 10 along. The battle works out each play's placement as the game does: it snaps the point to the placement grid and moves or refuses a troop placed where it may not go, such as on the other side's half or on a tower. Spells may be cast anywhere. A play the battle refuses is listed in the recent events and creates nothing.

The three layouts:

- `"at": [x, y]` places every copy at one point. With `count` greater than 1, the copies are stacked there, and `every` spaces them in time.
- `"line": { "from": [x, y], "to": [x, y] }` with `count` places `count` copies evenly from `from` to `to`, both ends included. A horizontal line makes a row of columns, and a vertical one makes a file.
- `"grid": { "from": [x, y], "to": [x, y], "columns": c, "rows": r }` places a copy on each point of a `c` by `r` grid with corners `from` and `to`. It fills row by row from `from`. A `count` below `c × r` takes only the first points.

Plays run in the order they are dropped. Plays dropped on the same tick run in the file's order. A scenario expands to at most 2000 plays. When it gives an end, every play must deploy by then.

## The examples

| File | What it shows |
|---|---|
| [skeletons-evo-vs-mother-witch.json](skeletons-evo-vs-mother-witch.json) | 20 evolved Skeletons against 20 Mother Witches, all 40 cards on one tick, each in its own column (`line` and `count`, `form`). |
| [knight-duel.json](knight-duel.json) | One Knight a side, with the towers holding their fire (`at`, `towers.attack`). |
| [hog-train.json](hog-train.json) | Five Hog Riders half a second apart, against weaker towers on the other side, and a later answer (`every`, per-side `towers`, `level`, `time`). |
| [archer-grid-vs-pekka.json](archer-grid-vs-pekka.json) | A grid of Archers against a P.E.K.K.A and an evolved Knight, then a Fireball (`grid`, a spell). |

`ExampleScenariosTest` (desktop) opens each file here, as the viewer does, and plays its first 30 seconds, checking that every play is placed. A new file in this folder is tested the same way.

## Limits

- Only the standard arena and its towers. No other game mode, arena, or tower troop.
- No ability taps. The Mirror (which repeats the last card) and a card played as an option picked from the elixir are refused when the file is read. Name the option's own card row instead.
- A side may play at most two champion cards, as a Ladder deck may hold.
- The battle core still refuses what it does not model. A card it cannot play is reported when the file is read. A unit behaviour it does not model halts the battle in the viewer, with the reason.
- The rows and their art are those of the data version the build makes (the fork's lock). The crforge-render plugin's art is for the version its release names.

The code: `org.crforge.core.battle.scenario.ScenarioScript` reads and builds a scenario. The viewer opens it through `ReplayFile` and plays it with `ReplayPlayer`.
