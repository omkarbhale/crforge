/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop.presentation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.crforge.presentation.PresentationApi;
import org.crforge.presentation.PresentationPlugin;

/**
 * A presentation plugin's jar named at launch ({@code --plugin <jar>}): checked before anything of
 * it is loaded, then opened as a class loader the host's {@link java.util.ServiceLoader} finds the
 * plugin on. The jar must exist, be readable as a jar, declare a presentation API version this
 * crforge runs ({@link PresentationApi#incompatibility}) and name a plugin in its service file;
 * otherwise it is refused, with a message that says why.
 */
public final class PluginJar {

  /** The launch argument that names a plugin jar. */
  public static final String ARGUMENT = "--plugin";

  private static final String SERVICE_FILE =
      "META-INF/services/" + PresentationPlugin.class.getName();

  private PluginJar() {
    // Utility class
  }

  /** A plugin jar refused at launch; its message is the whole reason, for the console. */
  public static final class Refused extends Exception {

    Refused(String message) {
      super(message);
    }
  }

  /**
   * Checks a plugin jar and opens a class loader on it.
   *
   * @param jar the jar the user named
   * @param parent the loader the presentation API and the host's classes come from
   * @return a loader that finds the jar's plugin, its parent first, so the plugin and the host
   *     share one presentation API
   * @throws Refused when the jar is missing, unreadable, built for an API this crforge cannot run,
   *     or names no plugin
   */
  public static ClassLoader open(Path jar, ClassLoader parent) throws Refused {
    String name = jar.toAbsolutePath().normalize().toString();
    if (!Files.isRegularFile(jar)) {
      throw new Refused("Presentation plugin " + name + " refused: no such file");
    }
    String declared;
    try (JarFile file = new JarFile(jar.toFile())) {
      Manifest manifest = file.getManifest();
      declared =
          manifest == null
              ? null
              : manifest.getMainAttributes().getValue(PresentationApi.MANIFEST_ATTRIBUTE);
      String reason = PresentationApi.incompatibility(declared);
      if (reason != null) {
        throw new Refused("Presentation plugin " + name + " refused: " + reason);
      }
      if (file.getEntry(SERVICE_FILE) == null) {
        throw new Refused(
            "Presentation plugin "
                + name
                + " refused: it names no plugin (no "
                + SERVICE_FILE
                + " in the jar)");
      }
    } catch (IOException | SecurityException e) {
      throw new Refused(
          "Presentation plugin "
              + name
              + " refused: it cannot be read as a jar: "
              + e.getMessage());
    }
    try {
      return new URLClassLoader(new URL[] {jar.toUri().toURL()}, parent);
    } catch (MalformedURLException e) {
      throw new Refused("Presentation plugin " + name + " refused: " + e.getMessage());
    }
  }
}
