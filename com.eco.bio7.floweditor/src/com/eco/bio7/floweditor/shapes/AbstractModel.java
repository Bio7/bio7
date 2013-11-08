package com.eco.bio7.floweditor.shapes;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

abstract public class AbstractModel implements IPropertySource,Serializable{

	
	private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
	}

	
	public void firePropertyChange(
		String propName,
		Object oldValue,
		Object newValue) {

		listeners.firePropertyChange(propName, oldValue, newValue);
	}

	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}

	public Object getEditableValue() {
		return this; 
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		
		return new IPropertyDescriptor[0];
	}

	public Object getPropertyValue(Object id) {
		return null;
	}

	public boolean isPropertySet(Object id) {
		return false;
	}

	public void resetPropertyValue(Object id) {
	}

	public void setPropertyValue(Object id, Object value) {
	}

}
