package com.eco.bio7.floweditor.ruler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.rulers.RulerChangeListener;
import org.eclipse.gef.rulers.RulerProvider;


/**
 * @author Pratik Shah
 * Adapted for the Bio7 application.
 */
public class Bio7RulerProvider extends RulerProvider {

	private DiagramRuler ruler;

	private PropertyChangeListener rulerListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			
			if (evt.getPropertyName().equals(DiagramRuler.PROPERTY_CHILDREN)) {
				DiagramGuide guide = (DiagramGuide) evt.getNewValue();
				if (getGuides().contains(guide)) {
					guide.addPropertyChangeListener(guideListener);
				} else {
					guide.removePropertyChangeListener(guideListener);
				}
				for (int i = 0; i < listeners.size(); i++) {
					((RulerChangeListener) listeners.get(i))
							.notifyGuideReparented(guide);
				}
			} else {
				for (int i = 0; i < listeners.size(); i++) {
					((RulerChangeListener) listeners.get(i))
							.notifyUnitsChanged(ruler.getUnit());
				}
			}
		}
	};

	private PropertyChangeListener guideListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			//Plugin.dprint(evt.getPropertyName());
			if (evt.getPropertyName().equals(DiagramGuide.PROPERTY_CHILDREN)) {
				for (int i = 0; i < listeners.size(); i++) {
					((RulerChangeListener) listeners.get(i))
							.notifyPartAttachmentChanged(evt.getNewValue(), evt
									.getSource());
				}
			} else {
				for (int i = 0; i < listeners.size(); i++) {
					((RulerChangeListener) listeners.get(i))
							.notifyGuideMoved(evt.getSource());
				}
			}
		}
	};

	public Bio7RulerProvider(DiagramRuler ruler) {
		
		this.ruler = ruler;
		this.ruler.addPropertyChangeListener(rulerListener);
		List guides = getGuides();
		for (int i = 0; i < guides.size(); i++) {
			((DiagramGuide) guides.get(i))
					.addPropertyChangeListener(guideListener);
		}
	}

	public List getAttachedModelObjects(Object guide) {
		return new ArrayList(((DiagramGuide) guide).getParts());
	}

	public Command getCreateGuideCommand(int position) {
		return new GuideCreateCommand(ruler, position);
	}

	public Command getDeleteGuideCommand(Object guide) {
		return new GuideDeleteCommand((DiagramGuide) guide, ruler);
	}

	public Command getMoveGuideCommand(Object guide, int pDelta) {
		return new GuideMoveCommand((DiagramGuide) guide, pDelta);
	}

	public int[] getGuidePositions() {
		List guides = getGuides();
		int[] result = new int[guides.size()];
		for (int i = 0; i < guides.size(); i++) {
			result[i] = ((DiagramGuide) guides.get(i)).getPosition();
		}
		return result;
	}

	public Object getRuler() {
		return ruler;
	}

	public int getUnit() {
		return ruler.getUnit();
	}

	public void setUnit(int newUnit) {
		ruler.setUnit(newUnit);
	}

	public int getGuidePosition(Object guide) {
		return ((DiagramGuide) guide).getPosition();
	}

	public List getGuides() {
		return ruler.getGuides();
	}
}
