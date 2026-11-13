/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The rule a plugin's declared API version is held to. */
class PresentationApiVersionTest {

  @Test
  @DisplayName("the version is <major>.<minor> of the two constants")
  void version() {
    assertThat(PresentationApi.VERSION)
        .isEqualTo(PresentationApi.MAJOR + "." + PresentationApi.MINOR);
  }

  @Test
  @DisplayName("a plugin built for this major version and this minor or an older one runs")
  void compatible() {
    assertThat(PresentationApi.incompatibility(PresentationApi.VERSION)).isNull();
    assertThat(PresentationApi.incompatibility(" " + PresentationApi.VERSION + " ")).isNull();
    for (int minor = 0; minor <= PresentationApi.MINOR; minor++) {
      assertThat(PresentationApi.incompatibility(PresentationApi.MAJOR + "." + minor))
          .as("minor %s", minor)
          .isNull();
    }
  }

  @Test
  @DisplayName("a plugin that declares no version, or not <major>.<minor>, is refused")
  void undeclared() {
    assertThat(PresentationApi.incompatibility(null))
        .contains("declares no presentation API version")
        .contains(PresentationApi.MANIFEST_ATTRIBUTE)
        .contains(PresentationApi.VERSION);
    assertThat(PresentationApi.incompatibility(" ")).contains("declares no");
    assertThat(PresentationApi.incompatibility("1")).contains("is not <major>.<minor>");
    assertThat(PresentationApi.incompatibility("one.two")).contains("is not <major>.<minor>");
    assertThat(PresentationApi.incompatibility("1.0.3")).contains("is not <major>.<minor>");
    assertThat(PresentationApi.incompatibility("-1.0")).contains("negative");
  }

  @Test
  @DisplayName("a plugin of another major version, or of a newer minor, is refused, saying which")
  void incompatible() {
    String newerMinor = PresentationApi.MAJOR + "." + (PresentationApi.MINOR + 1);
    assertThat(PresentationApi.incompatibility(newerMinor))
        .contains(newerMinor)
        .contains(PresentationApi.VERSION)
        .contains("update crforge");
    String newerMajor = (PresentationApi.MAJOR + 1) + ".0";
    assertThat(PresentationApi.incompatibility(newerMajor))
        .contains(newerMajor)
        .contains("update crforge");
    String olderMajor = (PresentationApi.MAJOR - 1) + ".9";
    assertThat(PresentationApi.incompatibility(olderMajor))
        .contains(olderMajor)
        .contains("update the plugin");
  }
}
