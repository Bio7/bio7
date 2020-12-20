/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.awt;

import gov.nasa.worldwind.util.Logging;
import java.awt.event.*;
import java.util.*;
import com.eco.bio7.worldwind.WorldWindOptionsView;
import com.eco.bio7.worldwind.WorldWindView;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.PlatformUI;
import gov.nasa.worldwind.poi.PointOfInterest;


/**
 * @author dcollins
 * @version $Id: KeyEventState.java 2193 2014-08-01 23:33:16Z dcollins $
 */
public class KeyEventState implements KeyListener, MouseListener {

    protected static class InputState {

        protected int eventType;
        protected int keyOrButtonCode;
        protected long timestamp;

        public InputState(int eventType, int keyOrButtonCode, long timestamp) {
            this.eventType = eventType;
            this.keyOrButtonCode = keyOrButtonCode;
            this.timestamp = timestamp;
        }

        public int getEventType() {
            return this.eventType;
        }

        public int getKeyOrButtonCode() {
            return this.keyOrButtonCode;
        }

        public long getTimestamp() {
            return this.timestamp;
        }
    }

    protected Map<Object, InputState> keyStateMap = new HashMap<>();
    protected int modifiersEx;
    protected int mouseModifiersEx;
    protected String lookupString;

    public KeyEventState() {
    }

    public boolean isKeyDown(int keyCode) {
        InputState state = this.getKeyState(keyCode);
        return state != null && state.getEventType() == KeyEvent.KEY_PRESSED;
    }

    public int keyState(int keyCode) {
        InputState state = this.getKeyState(keyCode);
        return state != null && state.getEventType() == KeyEvent.KEY_PRESSED ? 1 : 0;
    }

    public int getNumKeysDown() {
        if (keyStateMap.isEmpty()) {
            return (0);
        }
        int numKeys = 0;
        for (Object o : this.keyStateMap.keySet()) {
            //Integer key = (KeyEvent) o;
            InputState is = this.keyStateMap.get(o);
            if (is.getEventType() == KeyEvent.KEY_PRESSED) {
                numKeys++;
            }

        }
        return (numKeys);
    }

    public int getNumButtonsDown() {
        if (keyStateMap.isEmpty()) {
            return (0);
        }
        int numKeys = 0;
        for (Object o : this.keyStateMap.keySet()) {
            InputState is = this.keyStateMap.get(o);
            if (is.getEventType() == MouseEvent.MOUSE_PRESSED) {
                numKeys++;
            }

        }
        return (numKeys);
    }

    /**
     * @return The same value as {@link #getModifiersEx()}.
     * @deprecated Use {@link #getModifiersEx()} instead
     */
    @Deprecated
    public int getModifiers() {
        String msg = Logging.getMessage("generic.OperationDeprecatedAndChanged", "getModifiers", "getModifiersEx");
        Logging.logger().severe(msg);
        return this.modifiersEx;
    }

    /**
     * @return The extended event modifiers.
     */
    public int getModifiersEx() {
        return this.modifiersEx;
    }

    /**
     * @return The same value as {@link #getMouseModifiersEx()}.
     * @deprecated Use {@link #getMouseModifiersEx()} instead
     */
    @Deprecated
    public int getMouseModifiers() {
        String msg = Logging.getMessage("generic.OperationDeprecatedAndChanged", "getMouseModifiers", "getMouseModifiersEx");
        Logging.logger().severe(msg);
        return this.mouseModifiersEx;
    }

    /**
     * @return The extended mouse event modifiers.
     */
    public int getMouseModifiersEx() {
        return this.mouseModifiersEx;
    }

    public void clearKeyState() {
        this.keyStateMap.clear();
        this.modifiersEx = 0;
        this.mouseModifiersEx = 0;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e)
    {
        this.onKeyEvent(e, KeyEvent.KEY_PRESSED);
        int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_F2) {
			if (WorldWindView.getInstance().getFull() == null) {
				WorldWindView.getInstance().createFullscreen(0);
			}
		} 
		
		else if (keyCode == KeyEvent.VK_F3) {
			if (WorldWindView.getInstance().getFull() == null) {
				WorldWindView.getInstance().createFullscreen(1);
			}
		}
		
		
		else if (keyCode == KeyEvent.VK_ESCAPE) {
			WorldWindView.getInstance().recreateGLCanvas();
			if (WorldWindView.getInstance().getFull() != null) {
				WorldWindView.getInstance().getFull().exit();
			}
			WorldWindView.getInstance().setFull(null);
		}

		else if (keyCode == KeyEvent.VK_F4) {

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.asyncExec(new Runnable() {

				public void run() {
					List sc = WorldWindOptionsView.getScrolLList();
					if (sc.getSelectionIndices().length == 0) {
						sc.setSelection(0);
					}
					String pos = sc.getItem(sc.getSelectionIndex());

					/* Left out the name! */
					String[] p = pos.split(",");
					lookupString = (p[1] + "," + p[2]);

					if (lookupString == null || lookupString.length() < 1)
						return;
					WorldWindOptionsView inst = WorldWindOptionsView.optionsInstance;
					java.util.List<PointOfInterest> poi = inst.parseSearchValues(lookupString);

					if (poi != null) {
						if (poi.size() == 1) {
							inst.moveToLocation(poi.get(0));

						}

					}
					sc.setSelection((sc.getSelectionIndex() + 1) % sc.getItemCount());
				}

			});

		} else if (keyCode == KeyEvent.VK_LEFT) {

		}
    }

    @Override
    public void keyReleased(KeyEvent e) {
        this.removeKeyState(e);
    }

    protected void onKeyEvent(KeyEvent e, int eventType) {
        if (e == null) {
            return;
        }

        long timestamp = this.getTimeStamp(e, eventType, this.keyStateMap.get(e.getKeyCode()));
        this.setKeyState(e.getKeyCode(), new InputState(eventType, e.getKeyCode(), timestamp));
        this.setModifiersEx(e.getModifiersEx());
    }

    @Override
    public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
    }

    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {
        long timestamp = this.getTimeStamp(e, MouseEvent.MOUSE_PRESSED, this.keyStateMap.get(e.getModifiersEx()));
        this.setKeyState(e.getButton(), new InputState(MouseEvent.MOUSE_PRESSED, e.getButton(), timestamp));
        this.setMouseModifiersEx(e.getModifiersEx());
    }

    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        this.keyStateMap.remove(e.getButton());
        this.setMouseModifiersEx(0);
    }

    @Override
    public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(java.awt.event.MouseEvent mouseEvent) {

    }

    protected InputState getKeyState(int keyCode) {
        return this.keyStateMap.get(keyCode);
    }

    protected void setKeyState(int keyCode, InputState state) {
        this.keyStateMap.put(keyCode, state);
    }

    /**
     * @param modifiers Unused.
     * @deprecated Use {@link #setModifiersEx(int)} instead
     */
    @Deprecated
    protected void setModifiers(int modifiers) {
        String msg = Logging.getMessage("generic.OperationDeprecatedAndChanged", "setModifiers", "setModifiersEx");
        Logging.logger().severe(msg);
    }

    protected void setModifiersEx(int modifiersEx) {
        this.modifiersEx = modifiersEx;
    }

    protected void setMouseModifiersEx(int modifiersEx) {
        this.mouseModifiersEx = modifiersEx;
    }

    /**
     * @param modifiers Unused.
     * @deprecated Use {@link #setMouseModifiersEx(int)} instead
     */
    @Deprecated
    protected void setMouseModifiers(int modifiers) {
        String msg = Logging.getMessage("generic.OperationDeprecatedAndChanged", "setMouseModifiers", "setMouseModifiersEx");
        Logging.logger().severe(msg);
    }

    protected void removeKeyState(KeyEvent e) {
        this.keyStateMap.remove(e.getKeyCode());
        this.setModifiersEx(e.getModifiersEx());
    }

    protected long getTimeStamp(InputEvent e, int eventType, InputState currentState) {
        // If the current state for this input event type exists and is not null, then keep the current timestamp.
        if (currentState != null && currentState.getEventType() == eventType) {
            return currentState.getTimestamp();
        }
        // Otherwise return the InputEvent's timestamp.
        return e.getWhen();
    }
}
