/**
 * (c)Copyleft uniera.org
 */
package com.eco.bio7.floweditor.ruler;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.rulers.RulerProvider;

/**
 * 
 * Comment on DiagramRuler here
 *
 * @author Song Sun
 * 
 */
public class DiagramRuler implements Serializable {

	public static final String PROPERTY_CHILDREN = "ruler children changed"; //$NON-NLS-1$

	public static final String PROPERTY_UNIT = "units changed"; //$NON-NLS-1$

	static final long serialVersionUID = 1;

	protected PropertyChangeSupport listeners = new PropertyChangeSupport(this);

	private int unit;

	private boolean horizontal;

	// TO DO Remove 'transient' in future. guides can't remove.
	private List guides;

	public DiagramRuler(boolean isHorizontal) {
		this(isHorizontal, RulerProvider.UNIT_PIXELS);
	}

	public DiagramRuler(boolean isHorizontal, int unit) {
		horizontal = isHorizontal;
		setUnit(unit);
	}

	public void addGuide(IGuide guide) {
		if (!guides.contains(guide)) {
			guide.setHorizontal(!isHorizontal());
			guides.add(guide);
			listeners.firePropertyChange(PROPERTY_CHILDREN, null, guide);
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
	}

	// the returned list should not be modified
	public List getGuides() {
		if (guides == null) {
			guides = new ArrayList();
		}
		return guides;
	}

	public int getUnit() {
		return unit;
	}

	public boolean isHidden() {
		return false;
	}

	public boolean isHorizontal() {
		return horizontal;
	}

	public void removeGuide(IGuide guide) {
		if (guides.remove(guide)) {
			listeners.firePropertyChange(PROPERTY_CHILDREN, null, guide);
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}

	public void setHidden(boolean isHidden) {
	}

	public void setUnit(int newUnit) {
		if (unit != newUnit) {
			int oldUnit = unit;
			unit = newUnit;
			listeners.firePropertyChange(PROPERTY_UNIT, oldUnit, newUnit);
		}
	}

}
