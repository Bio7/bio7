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
 * Marks a method of a simulation model (public, without parameters) as an
 * action: a button in the settings of Bio7 and of the HTML export (e.g., "Add
 * a glider"). The view is updated after the call. The buttons of a group are
 * shown below the settings of the group (sorted by the label).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {

	/** The label of the button (default: the name of the method). */
	String label() default "";

	/** The group (the heading of the settings with the same group, empty: the buttons at the end). */
	String group() default "";
}
