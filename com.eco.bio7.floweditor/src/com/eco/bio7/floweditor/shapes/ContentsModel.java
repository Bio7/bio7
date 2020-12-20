package com.eco.bio7.floweditor.shapes;

import java.util.ArrayList;
import java.util.List;

public class ContentsModel extends AbstractModel {
	
	public static final String P_CHILDREN = "_children";

	private List children = new ArrayList(); 

	public void addChild(Object child) {
		children.add(child);
		
		firePropertyChange(P_CHILDREN, null, null);
	}

	public List getChildren() {
		return children; 
	}

	public void removeChild(Object child) {
		
		children.remove(child);
		
		firePropertyChange(P_CHILDREN, null, null);
	}

}
