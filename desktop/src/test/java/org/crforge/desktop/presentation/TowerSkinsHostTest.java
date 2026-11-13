/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.battle.unit.Standard1v1Battle;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.replay.ReplaySkins;
import org.crforge.desktop.replay.ReplaySkins.PlayerSkins;
import org.crforge.presentation.BattleStart;
import org.crforge.presentation.PresentContext;
import org.crforge.presentation.PresentationPlugin;
import org.crforge.presentation.TickFrame;
import org.crforge.presentation.TowerSkinFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A replay's tower skin data, told to a plugin at each battle start. */
class TowerSkinsHostTest {

  private static BattleSession session() {
    return BattleSession.of(new Standard1v1Battle(GameTables.loadConfigured(), 11, false));
  }

  @Test
  @DisplayName("every battle start of a replay tells its players' sk and seed, unchanged")
  void replay(@TempDir Path services) throws IOException {
    ReplaySkins skins =
        new ReplaySkins(
            1131,
            List.of(
                new PlayerSkins(0, List.of(87000001, 94000033), List.of(94000033), 94000033, true),
                new PlayerSkins(1, List.of(87000001), List.of(), 0, false)));
    try (URLClassLoader loader = recordingPlugin(services)) {
      PresentationHost host = PresentationHost.discover(null, loader);
      host.replay(skins);
      host.follow(session());
      host.follow(session());
      host.close();
    }
    List<TowerSkinFrame> told =
        List.of(
            new TowerSkinFrame(0, List.of(87000001, 94000033), List.of(94000033), 94000033, true),
            new TowerSkinFrame(1, List.of(87000001), List.of(), 0, false));
    assertThat(Starts.last.starts).as("the first start and a restart").hasSize(2);
    assertThat(Starts.last.starts)
        .allSatisfy(
            start -> {
              assertThat(start.replay()).isTrue();
              assertThat(start.rndSeed()).isEqualTo(1131);
              assertThat(start.towerSkins()).isEqualTo(told);
            });
  }

  @Test
  @DisplayName("a battle of no replay starts with no tower skin data, as before")
  void noReplay(@TempDir Path services) throws IOException {
    try (URLClassLoader loader = recordingPlugin(services)) {
      PresentationHost host = PresentationHost.discover(null, loader);
      host.follow(session());
      host.close();
    }
    BattleStart start = Starts.last.starts.get(0);
    assertThat(start.replay()).isFalse();
    assertThat(start.towerSkins()).isEmpty();
    assertThat(start)
        .isEqualTo(
            new BattleStart(start.arenaWidthUnits(), start.arenaHeightUnits(), start.tick()));
  }

  private static URLClassLoader recordingPlugin(Path services) throws IOException {
    Path service = services.resolve("META-INF/services/" + PresentationPlugin.class.getName());
    Files.createDirectories(service.getParent());
    Files.writeString(service, Starts.class.getName() + "\n");
    return new URLClassLoader(
        new URL[] {services.toUri().toURL()}, TowerSkinsHostTest.class.getClassLoader());
  }

  /** A plugin that keeps every battle start. */
  public static final class Starts implements PresentationPlugin {

    static Starts last;

    final List<BattleStart> starts = new ArrayList<>();

    public Starts() {
      last = this;
    }

    @Override
    public String id() {
      return "starts";
    }

    @Override
    public void battleStarted(BattleStart battle) {
      starts.add(battle);
    }

    @Override
    public void tick(TickFrame frame) {}

    @Override
    public void present(PresentContext context) {}
  }
}
