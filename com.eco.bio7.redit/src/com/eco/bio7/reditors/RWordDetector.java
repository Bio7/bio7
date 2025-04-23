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


import org.eclipse.jface.text.rules.IWordDetector;

/**
 * A R aware word detector.
 * Not needed in the adapted R-Editor. Has to be changed !
 */
public class RWordDetector implements IWordDetector {
	
	
	
	public boolean isWordPart(char character) {
		return Character.isJavaIdentifierPart(character);
	}
	
	
	public boolean isWordStart(char character) {
		return Character.isJavaIdentifierStart(character);
	}
}
