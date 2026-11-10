/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rffm.graphics;

/**
 * A graphics device whose size can change (see RSession.resizeDevice, which also
 * draws the plot again in the new size).
 */
public interface ResizableGraphicsDevice extends RGraphicsDevice {

	/** Changes the size in pixels (called in the thread of R). */
	void resize(int width, int height);
}
