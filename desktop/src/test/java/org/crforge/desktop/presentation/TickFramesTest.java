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
import java.util.List;
import org.crforge.core.battle.data.GameTables;
import org.crforge.core.pathfinding.GridEntityState;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.replay.ReplayFile;
import org.crforge.desktop.replay.ReplayPlayer;
import org.crforge.presentation.EntityState;
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
  @DisplayName("reading a frame after every tick leaves the battle as it plays without them")
  void readingChangesNothing() throws IOException {
    GameTables tables = GameTables.loadConfigured();
    ReplayFile replay =
        ReplayFile.read(Path.of(System.getProperty("crforge.exampleReplay")), tables);
    assumeTrue(
        replay.playable(), "the example replay plays only on the tables of its own data version");

    ReplayPlayer read = new ReplayPlayer(replay, tables);
    List<TickFrame> heard = new ArrayList<>();
    read.getSession()
        .setStepListener(() -> heard.add(TickFrames.of(BattleAdapter.frame(read.getSession()))));
    while (read.step()) {
      // to the replay's end
    }

    ReplayPlayer unread = new ReplayPlayer(replay, tables);
    while (unread.step()) {
      // to the replay's end
    }

    assertThat(heard).isNotEmpty();
    assertThat(unread.getSession().tick()).isEqualTo(read.getSession().tick());
    assertThat(TickFrames.of(BattleAdapter.frame(unread.getSession())))
        .isEqualTo(heard.get(heard.size() - 1));
  }
}
