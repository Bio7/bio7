/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.simulation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field of a simulation model as a setting: the HTML export shows it as
 * a control (a slider with min and max, else a number field, a checkbox for a
 * boolean, a choice with options, else a text field). The value is read again
 * at every step (live) or, with restart = true, at the next setup. Settings
 * with the same group are shown below a heading.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Param {

	/** The label (default: the name of the field). */
	String label() default "";

	/** The minimum of a slider (min and max both set: a slider). */
	double min() default Double.NaN;

	/** The maximum of a slider. */
	double max() default Double.NaN;

	/** The step of a slider (default: 1 for int fields, (max - min) / 100 else). */
	double step() default Double.NaN;

	/** The options of a String field (a choice). */
	String[] options() default {};

	/** true: a change restarts the simulation (setup) instead of a live change. */
	boolean restart() default false;

	/** The group of the setting: a heading above the settings of the group (empty: no heading). */
	String group() default "";
}
