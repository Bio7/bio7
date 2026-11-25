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
package com.eco.bio7.floweditor.figures;

/**
 * A shape of a flow which shows that it is executed (the highlight replaces the
 * former white background).
 */
public interface RunningFigure {

	/** Shows (true) or hides the highlight of the execution. */
	void setRunning(boolean running);
}
