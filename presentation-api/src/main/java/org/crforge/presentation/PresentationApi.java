/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * The version of this API, which a plugin jar declares it was built against, and the rule a host
 * holds the declaration to before it loads anything of the jar.
 *
 * <p>A plugin jar declares the version in its manifest, under {@link #MANIFEST_ATTRIBUTE}, as
 * {@code <major>.<minor>}: the {@link #VERSION} of the API it compiled against. In Gradle:
 *
 * <pre>{@code
 * tasks.jar {
 *     manifest { attributes("Crforge-Presentation-Api" to "1.4") }
 * }
 * }</pre>
 *
 * <p>The minor version rises with each addition to the API, which leaves every plugin built against
 * an older minor working; the major version rises with a change that breaks one. A host takes a
 * plugin of its own major version and of its minor version or an older one, and refuses any other
 * with the reason ({@link #incompatibility}).
 */
public final class PresentationApi {

  /** The major version: raised by a change that breaks a plugin built before it. */
  public static final int MAJOR = 1;

  /** The minor version: raised by each addition, which plugins built before it do not see. */
  public static final int MINOR = 4;

  /** This API's version, {@code <major>.<minor>}. */
  public static final String VERSION = MAJOR + "." + MINOR;

  /** The plugin jar's manifest attribute that names the API version it was built against. */
  public static final String MANIFEST_ATTRIBUTE = "Crforge-Presentation-Api";

  private PresentationApi() {
    // Constants and the compatibility rule only
  }

  /**
   * Why a plugin built against a version cannot run on this API, or null when it can: when it was
   * built against this major version and this minor version or an older one.
   *
   * @param declared the version the plugin's manifest declares, or null when it declares none
   * @return the reason it is refused, a sentence naming both versions, or null for a compatible one
   */
  public static String incompatibility(String declared) {
    if (declared == null || declared.isBlank()) {
      return "it declares no presentation API version (manifest attribute "
          + MANIFEST_ATTRIBUTE
          + "); this crforge runs plugins built for API "
          + MAJOR
          + ".0 to "
          + VERSION;
    }
    String version = declared.strip();
    int dot = version.indexOf('.');
    int major;
    int minor;
    try {
      if (dot < 0) {
        throw new NumberFormatException(version);
      }
      major = Integer.parseInt(version.substring(0, dot));
      minor = Integer.parseInt(version.substring(dot + 1));
    } catch (NumberFormatException e) {
      return "its presentation API version \""
          + version
          + "\" is not <major>.<minor>; this crforge runs plugins built for API "
          + MAJOR
          + ".0 to "
          + VERSION;
    }
    if (major < 0 || minor < 0) {
      return "its presentation API version \"" + version + "\" is negative";
    }
    if (major != MAJOR) {
      return "it was built for presentation API "
          + version
          + ", and this crforge runs API "
          + VERSION
          + ": "
          + (major < MAJOR
              ? "the plugin is too old for this crforge, so update the plugin"
              : "the plugin needs a newer crforge, so update crforge");
    }
    if (minor > MINOR) {
      return "it was built for presentation API "
          + version
          + ", newer than this crforge's API "
          + VERSION
          + ": the plugin needs a newer crforge, so update crforge";
    }
    return null;
  }
}
