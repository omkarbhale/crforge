/*
 * crforge - https://github.com/voonhous/crforge
 * SPDX-License-Identifier: Apache-2.0
 * Porting this code? Please cite this file and the commit you read: see the README.
 */

package org.crforge.presentation;

/**
 * An attack's own hit that landed in the tick just run: a direct hit, a dash landing's, or one
 * victim's share of the area of either. A projectile's hit is none: its impact is told by the
 * projectile leaving.
 *
 * @param attackerId the id of the entity whose hit it is
 * @param victimId the id of the entity it landed on
 * @param special whether it is a special hit: a loaded or charged special, a complete charge's hit
 *     or a dash landing's
 */
public record StrikeFrame(int attackerId, int victimId, boolean special) {}
