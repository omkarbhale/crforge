/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import com.badlogic.gdx.graphics.OrthographicCamera;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.crforge.core.pathfinding.grid.TileMap;
import org.crforge.desktop.battle.BattleAdapter;
import org.crforge.desktop.battle.BattleFrame;
import org.crforge.desktop.battle.BattleSession;
import org.crforge.desktop.render.BattleRenderer;
import org.crforge.presentation.BattleStart;
import org.crforge.presentation.PresentContext;
import org.crforge.presentation.PresentationPlugin;

/**
 * What stands between a screen and whatever draws its arena: the plugin {@link ServiceLoader} finds
 * on the classpath, else the built-in visualizer ({@link BuiltinPresentation}).
 *
 * <p>With the built-in visualizer, the host adds nothing to the screen's frame: no listener on the
 * battle and no frame beyond the one the screen builds. Only a found plugin is told the battle,
 * tick by tick. Any failure of a plugin is contained here: it is logged, the plugin is closed and
 * the built-in visualizer takes over, so that a plugin's bug never stops the battle.
 */
@Slf4j
public final class PresentationHost {

  private final BuiltinPresentation builtin;

  /** What draws the arena: {@link #builtin}, or the plugin found. */
  private PresentationPlugin active;

  /** The battle a plugin was last told of, or null. */
  private BattleSession followed;

  /** What that battle's ticks are collecting for a plugin, or null before the first tick. */
  private TickEventLog events;

  private PresentationHost(BuiltinPresentation builtin, PresentationPlugin active) {
    this.builtin = builtin;
    this.active = active;
  }

  /**
   * The host of a screen, on the plugin found on the thread's class loader, else on the renderer.
   *
   * @param renderer the screen's own renderer, the built-in visualizer
   */
  public static PresentationHost discover(BattleRenderer renderer) {
    return discover(renderer, Thread.currentThread().getContextClassLoader());
  }

  static PresentationHost discover(BattleRenderer renderer, ClassLoader loader) {
    BuiltinPresentation builtin = new BuiltinPresentation(renderer);
    TreeMap<String, PresentationPlugin> found = found(loader);
    if (found.isEmpty()) {
      return new PresentationHost(builtin, builtin);
    }
    if (found.size() > 1) {
      log.warn(
          "{} presentation plugins found, using {}: {}",
          found.size(),
          found.firstKey(),
          found.keySet());
    }
    log.info("Presentation plugin: {}", found.firstKey());
    return new PresentationHost(builtin, found.firstEntry().getValue());
  }

  /**
   * The plugins on a class loader, by id. One that cannot be loaded ends the search, with a log
   * line: the plugins found until then remain, and with none the built-in visualizer draws.
   */
  private static TreeMap<String, PresentationPlugin> found(ClassLoader loader) {
    TreeMap<String, PresentationPlugin> found = new TreeMap<>();
    try {
      for (PresentationPlugin plugin : ServiceLoader.load(PresentationPlugin.class, loader)) {
        found.put(plugin.id(), plugin);
      }
    } catch (ServiceConfigurationError | RuntimeException | LinkageError e) {
      log.error("A presentation plugin could not be loaded", e);
    }
    return found;
  }

  /**
   * Keeps a plugin on the battle on screen, before the frame's steps run. A battle it has not heard
   * of (the first, a restart, a step back, another version or replay) starts for it at its current
   * tick, and each step of it from then on is told.
   *
   * @param session the battle on screen
   */
  public void follow(BattleSession session) {
    if (active == builtin || session == followed) {
      return;
    }
    stopFollowing();
    followed = session;
    contained(
        () -> {
          TileMap map = session.getBattle().getWorld().getTileMap();
          active.battleStarted(
              new BattleStart(map.widthUnits(), map.heightUnits(), session.tick()));
          tellTick(session);
        });
    if (active != builtin) {
      // Attached after the battle's first tell, which reports no tick having run: the log hears
      // from the next step on. It only listens, and the session's own area log already keeps the
      // battle's observer list from being empty.
      events = new TickEventLog();
      session.getBattle().getWorld().addObserver(events);
      session.setStepListener(() -> contained(() -> tellTick(session)));
    }
  }

  private void tellTick(BattleSession session) {
    active.tick(
        events == null
            ? TickFrames.of(BattleAdapter.frame(session))
            : TickFrames.of(
                BattleAdapter.frame(session), events.drainDeaths(), events.drainStrikes()));
  }

  /** Lets go of the battle followed: no more ticks, and its log stops collecting. */
  private void stopFollowing() {
    if (followed != null) {
      followed.setStepListener(null);
    }
    if (events != null) {
      // An observer cannot be detached; a stopped log just ignores what it is told.
      events.stop();
      events = null;
    }
  }

  /**
   * Draws the arena, the viewport set up for it.
   *
   * @param frame the screen's frame, for the built-in visualizer
   * @param camera the arena's camera, for the built-in visualizer
   * @param inputs the rest of what the built-in visualizer draws from
   * @param context where the arena is, for a plugin
   */
  public void present(
      BattleFrame frame,
      OrthographicCamera camera,
      BattleRenderer.Inputs inputs,
      PresentContext context) {
    builtin.prepare(frame, camera, inputs);
    if (active != builtin) {
      contained(() -> active.present(context));
    }
    // Not contained: a failure of the built-in visualizer is the screen's, as it always was. It
    // also draws the frame a plugin failed on.
    if (active == builtin) {
      builtin.present(context);
    }
  }

  /** The screen is going: the plugin is closed. */
  public void close() {
    if (active == builtin) {
      return;
    }
    stopFollowing();
    PresentationPlugin closing = active;
    active = builtin;
    closeQuietly(closing);
  }

  /** Runs a call into a plugin; a failure hands the arena back to the built-in visualizer. */
  private void contained(Runnable call) {
    if (active == builtin) {
      return;
    }
    try {
      call.run();
    } catch (RuntimeException | LinkageError e) {
      log.error("Presentation plugin {} failed, back to the built-in visualizer", name(active), e);
      close();
    }
  }

  private static void closeQuietly(PresentationPlugin plugin) {
    try {
      plugin.close();
    } catch (RuntimeException | LinkageError e) {
      log.error("Presentation plugin {} failed to close", name(plugin), e);
    }
  }

  /** A failed plugin's name for the log: its class, since its id is a call into it. */
  private static String name(PresentationPlugin plugin) {
    return plugin.getClass().getName();
  }
}
