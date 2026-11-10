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

package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;

import com.eco.bio7.rbridge.backend.RBackend;

/**
 * An R script for ExecuteRScript.run(...) with the connection of Bio7 (Rserve or
 * embedded R), e.g., as a lambda:
 *
 * <pre>
 * ExecuteRScript.run((RBackend r, IProgressMonitor monitor) -&gt; {
 * 	r.eval("print(runif(10))");
 * }, 1);
 * </pre>
 */
@FunctionalInterface
public interface RBackendScript {

	void evaluate(RBackend backend, IProgressMonitor monitor) throws Exception;
}
