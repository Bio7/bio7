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
