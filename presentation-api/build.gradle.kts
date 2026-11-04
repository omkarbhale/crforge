/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

plugins {
    `java-library`
}

// The presentation API names no dependency on purpose: with nothing else on its compile classpath,
// no type of the battle core or of a graphics engine can reach its surface.
