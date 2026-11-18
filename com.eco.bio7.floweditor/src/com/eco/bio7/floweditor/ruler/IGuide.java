/*******************************************************************************
 * Part of the ruler and guide support of the Bio7 flow editor, adapted from
 * the GEF Logic example (org.eclipse.gef.examples.logic) by uniera.org and
 * M. Austenfeld.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     uniera.org - initial API
 *     M. Austenfeld - Bio7
 *******************************************************************************/
package com.eco.bio7.floweditor.ruler;

import java.io.Serializable;
import java.util.Map;

public interface IGuide extends Serializable {
	void attachPart(IArea part, int alignment);
	void detachPart(IArea part);
	int getAlignment(IArea part);
	Map getMap();
	void setHorizontal(boolean b);
}
