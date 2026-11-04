/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import com.badlogic.gdx.graphics.OrthographicCamera;
import org.crforge.desktop.battle.BattleFrame;
import org.crforge.desktop.render.BattleRenderer;
import org.crforge.presentation.BattleStart;
import org.crforge.presentation.PresentContext;
import org.crforge.presentation.PresentationPlugin;
import org.crforge.presentation.TickFrame;

/**
 * Today's visualizer as the default plugin: the host uses it when no other is found. It is part of
 * the desktop module, not a user of the API: it draws from what the screen prepares each frame, the
 * frame and inputs {@link BattleRenderer} has always drawn from, which hold overlays' live reads of
 * the battle that a plugin is never given. So it takes nothing from the ticks.
 */
final class BuiltinPresentation implements PresentationPlugin {

  static final String ID = "builtin";

  private final BattleRenderer renderer;

  private BattleFrame frame;

  private OrthographicCamera camera;

  private BattleRenderer.Inputs inputs;

  BuiltinPresentation(BattleRenderer renderer) {
    this.renderer = renderer;
  }

  /** What the next {@link #present} draws. */
  void prepare(BattleFrame frame, OrthographicCamera camera, BattleRenderer.Inputs inputs) {
    this.frame = frame;
    this.camera = camera;
    this.inputs = inputs;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void battleStarted(BattleStart battle) {}

  @Override
  public void tick(TickFrame frame) {}

  @Override
  public void present(PresentContext context) {
    // The workspace draws its own controls outside the arena, so no legacy HUD here.
    renderer.render(frame, camera, inputs, false);
    frame = null;
    camera = null;
    inputs = null;
  }
}
