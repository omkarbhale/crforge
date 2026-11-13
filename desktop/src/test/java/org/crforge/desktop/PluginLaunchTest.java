/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.crforge.desktop.presentation.PluginJar;
import org.crforge.desktop.presentation.PresentationHost;
import org.crforge.presentation.BattleStart;
import org.crforge.presentation.PresentContext;
import org.crforge.presentation.PresentationApi;
import org.crforge.presentation.PresentationPlugin;
import org.crforge.presentation.TickFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A presentation plugin jar named at launch ({@code --plugin}): checked, refused or taken. */
class PluginLaunchTest {

  private static final String SERVICE_FILE =
      "META-INF/services/" + PresentationPlugin.class.getName();

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();

  private final ByteArrayOutputStream err = new ByteArrayOutputStream();

  /** Opens the plugin a launch with these arguments names, as the launcher does. */
  private ClassLoader launch(String... args) {
    return DesktopLauncher.openPlugin(
        args,
        PluginLaunchTest.class.getClassLoader(),
        new PrintStream(out, true, StandardCharsets.UTF_8),
        new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  private String err() {
    return err.toString(StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("with no plugin named, the launcher's own class loader stays and nothing is said")
  void noPlugin() {
    ClassLoader own = PluginLaunchTest.class.getClassLoader();
    assertThat(launch()).isSameAs(own);
    assertThat(launch("--replay", "some.json")).isSameAs(own);
    assertThat(out.size()).isZero();
    assertThat(err.size()).isZero();
  }

  @Test
  @DisplayName("a compatible jar is taken, and the host draws with the plugin it names")
  void compatible(@TempDir Path dir) throws IOException {
    Path jar = jar(dir.resolve("renderer.jar"), PresentationApi.VERSION, true);

    ClassLoader loader = launch("--plugin", jar.toString());

    assertThat(loader).isNotNull().isNotSameAs(PluginLaunchTest.class.getClassLoader());
    assertThat(out.toString(StandardCharsets.UTF_8))
        .contains("renderer.jar")
        .contains(PresentationApi.VERSION);
    assertThat(err.size()).isZero();

    // The screens find it on the thread's class loader, as the launcher sets it.
    Thread thread = Thread.currentThread();
    ClassLoader before = thread.getContextClassLoader();
    Named.created = 0;
    try {
      thread.setContextClassLoader(loader);
      PresentationHost host = PresentationHost.discover(null);
      assertThat(Named.created).as("the jar's plugin was made").isOne();
      host.close();
    } finally {
      thread.setContextClassLoader(before);
    }
  }

  @Test
  @DisplayName("a jar built for a newer API is refused, naming both versions and what to update")
  void newerApi(@TempDir Path dir) throws IOException {
    String newer = PresentationApi.MAJOR + "." + (PresentationApi.MINOR + 1);
    Path jar = jar(dir.resolve("renderer.jar"), newer, true);
    assertThat(launch("--plugin", jar.toString())).isNull();
    assertThat(err())
        .contains("Presentation plugin")
        .contains("renderer.jar")
        .contains("refused")
        .contains(newer)
        .contains(PresentationApi.VERSION)
        .contains("update crforge");
  }

  @Test
  @DisplayName("a jar of another major version is refused")
  void otherMajor(@TempDir Path dir) throws IOException {
    String other = (PresentationApi.MAJOR + 1) + ".0";
    assertThat(launch("--plugin", jar(dir.resolve("a.jar"), other, true).toString())).isNull();
    assertThat(err()).contains("refused").contains(other);
  }

  @Test
  @DisplayName("a jar that declares no API version is refused, naming the manifest attribute")
  void undeclared(@TempDir Path dir) throws IOException {
    assertThat(launch("--plugin", jar(dir.resolve("a.jar"), null, true).toString())).isNull();
    assertThat(err())
        .contains("declares no presentation API version")
        .contains(PresentationApi.MANIFEST_ATTRIBUTE);
  }

  @Test
  @DisplayName("a jar that names no plugin is refused")
  void noService(@TempDir Path dir) throws IOException {
    Path jar = jar(dir.resolve("a.jar"), PresentationApi.VERSION, false);
    assertThat(launch("--plugin", jar.toString())).isNull();
    assertThat(err()).contains("names no plugin").contains(SERVICE_FILE);
  }

  @Test
  @DisplayName("a missing file, a file that is no jar and a flag with no file are refused")
  void unreadable(@TempDir Path dir) throws IOException {
    assertThat(launch("--plugin", dir.resolve("missing.jar").toString())).isNull();
    assertThat(err()).contains("missing.jar").contains("no such file");

    Path text = Files.writeString(dir.resolve("notes.jar"), "not a jar");
    assertThat(launch("--plugin", text.toString())).isNull();
    assertThat(err()).contains("notes.jar").contains("cannot be read as a jar");

    assertThat(launch("--plugin")).isNull();
    assertThat(err()).contains("--plugin names no plugin jar");
  }

  @Test
  @DisplayName("the check refuses through its own exception, with the reason as its message")
  void refusedDirectly(@TempDir Path dir) {
    assertThatThrownBy(
            () -> PluginJar.open(dir.resolve("gone.jar"), PluginLaunchTest.class.getClassLoader()))
        .isInstanceOf(PluginJar.Refused.class)
        .hasMessageContaining("gone.jar");
  }

  /**
   * Writes a plugin jar: a manifest with the API version, if any, and the service file naming
   * {@link Named}, if asked. The class itself is the test's own, found through the jar loader's
   * parent, as a plugin's copy of the API is.
   */
  private static Path jar(Path file, String version, boolean service) throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (version != null) {
      manifest.getMainAttributes().putValue(PresentationApi.MANIFEST_ATTRIBUTE, version);
    }
    try (OutputStream stream = Files.newOutputStream(file);
        JarOutputStream jar = new JarOutputStream(stream, manifest)) {
      if (service) {
        jar.putNextEntry(new JarEntry(SERVICE_FILE));
        jar.write((Named.class.getName() + "\n").getBytes(StandardCharsets.UTF_8));
        jar.closeEntry();
      }
    }
    return file;
  }

  /** The plugin the jars name. */
  public static final class Named implements PresentationPlugin {

    static int created;

    public Named() {
      created++;
    }

    @Override
    public String id() {
      return "named";
    }

    @Override
    public void battleStarted(BattleStart battle) {}

    @Override
    public void tick(TickFrame frame) {}

    @Override
    public void present(PresentContext context) {}
  }
}
