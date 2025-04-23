/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.reditors;


import org.eclipse.jface.text.rules.IWhitespaceDetector;


public class RWhitespaceDetector implements IWhitespaceDetector {

	
	public boolean isWhitespace(char character) {
		return Character.isWhitespace(character);
	}
}
