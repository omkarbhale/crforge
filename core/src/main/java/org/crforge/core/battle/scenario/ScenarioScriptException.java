/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.core.battle.scenario;

import java.util.List;

/**
 * A scenario file the reader refuses ({@link ScenarioScript#read}), with every problem it found,
 * one a line, each starting with the field it is in, such as {@code plays[2].card}.
 */
public final class ScenarioScriptException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  /** The problems, one a line. */
  private final List<String> problems;

  public ScenarioScriptException(List<String> problems) {
    super(
        "the scenario is refused, "
            + problems.size()
            + (problems.size() == 1 ? " problem: " : " problems: ")
            + String.join("; ", problems));
    this.problems = List.copyOf(problems);
  }

  /** Every problem found, one a line. */
  public List<String> problems() {
    return problems;
  }
}
