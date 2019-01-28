package com.eco.bio7.spatial;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;

/**
 * This class offers static methods to detect custom mouse and key events in the
 * Spatial panel of Bio7.
 * 
 * @author Bio7
 * 
 */
public class SpatialEvents {

	protected static boolean mouseDragged;

	protected static MouseEvent dragEvent;

	protected static boolean mousePressed;

	protected static MouseEvent pressEvent;

	protected static boolean mouseClicked;

	protected static MouseEvent clickEvent;

	protected static boolean rightMouseClicked;

	protected static MouseEvent rightClickEvent;

	protected static boolean mouseWheelClicked;

	protected static MouseEvent wheelClickEvent;

	protected static boolean mouseDoubleClicked;

	protected static MouseEvent doubleClickEvent;

	protected static boolean mouseTripleClicked;

	protected static MouseEvent tripleClickEvent;

	protected static boolean mouseWheelMoved;

	protected static MouseEvent mouseWheelEvent;

	protected static boolean mouseMoved;

	protected static MouseEvent mouseMoveEvent;

	protected static boolean keyPressed;

	protected static KeyEvent keyPressEvent;

	protected static boolean mouseEntered;

	protected static MouseEvent mouseEnteredEvent;

	protected static boolean mouseExited;

	protected static MouseEvent mouseExitedEvent;

	protected static boolean keyReleased;

	protected static KeyEvent keyReleaseEvent;

	protected static boolean keyTyped;

	protected static KeyEvent keyTypedEvent;

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getDragEvent() {
		return dragEvent;
	}

	protected static void setDragEvent(MouseEvent dragEvent) {
		SpatialEvents.dragEvent = dragEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse wheel
	 * event occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseWheelMoved() {
		boolean mouseWheelTemp = mouseWheelMoved;
		mouseWheelMoved = false;
		return mouseWheelTemp;
	}

	protected static void setMouseWheelMoved(boolean mouseWheelMoved) {
		SpatialEvents.mouseWheelMoved = mouseWheelMoved;
	}

	/**
	 * This method returns a MouseWheelEvent instance for the specified event.
	 * 
	 * @return a MouseWheelEvent instance.
	 */
	public static MouseEvent getMouseWheelEvent() {

		return mouseWheelEvent;
	}

	protected static void setMouseWheelEvent(MouseEvent mouseWheelEvent) {
		SpatialEvents.mouseWheelEvent = mouseWheelEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseClicked() {
		boolean mouseClickedTemp = mouseClicked;
		mouseClicked = false;
		return mouseClickedTemp;
	}

	protected static void setMouseClicked(boolean mouseClicked) {
		SpatialEvents.mouseClicked = mouseClicked;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getClickEvent() {
		return clickEvent;
	}

	protected static void setClickEvent(MouseEvent clickEvent) {
		SpatialEvents.clickEvent = clickEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseDragged() {
		boolean mouseDraggedTemp = mouseDragged;
		mouseDragged = false;
		return mouseDraggedTemp;
	}

	protected static void setMouseDragged(boolean mouseDragged) {
		SpatialEvents.mouseDragged = mouseDragged;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMousePressed() {
		boolean mousePressedTemp = mousePressed;
		mousePressed = false;
		return mousePressedTemp;
	}

	protected static void setMousePressed(boolean mousePressed) {
		SpatialEvents.mousePressed = mousePressed;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getPressEvent() {
		return pressEvent;
	}

	protected static void setPressEvent(MouseEvent pressEvent) {
		SpatialEvents.pressEvent = pressEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseDoubleClicked() {
		boolean mouseDoubleClickedTemp = mouseDoubleClicked;
		mouseDoubleClicked = false;

		return mouseDoubleClickedTemp;
	}

	protected static void setMouseDoubleClicked(boolean mouseDoubleClicked) {
		SpatialEvents.mouseDoubleClicked = mouseDoubleClicked;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getDoubleClickEvent() {
		return doubleClickEvent;
	}

	protected static void setDoubleClickEvent(MouseEvent doubleClickEvent) {
		SpatialEvents.doubleClickEvent = doubleClickEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isRightMouseClicked() {
		boolean rightMouseClickedTemp = rightMouseClicked;
		rightMouseClicked = false;

		return rightMouseClickedTemp;
	}

	protected static void setRightMouseClicked(boolean rightMouseClicked) {
		SpatialEvents.rightMouseClicked = rightMouseClicked;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getRightClickEvent() {
		return rightClickEvent;
	}

	protected static void setRightClickEvent(MouseEvent rightClickEvent) {
		SpatialEvents.rightClickEvent = rightClickEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseMoved() {
		boolean mouseMovedTemp = mouseMoved;
		mouseMoved = false;
		return mouseMovedTemp;
	}

	protected static void setMouseMoved(boolean mouseMoved) {
		SpatialEvents.mouseMoved = mouseMoved;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getMouseMoveEvent() {
		return mouseMoveEvent;
	}

	protected static void setMouseMoveEvent(MouseEvent mouseMoveEvent) {
		SpatialEvents.mouseMoveEvent = mouseMoveEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseEntered() {
		boolean mouseEnteredTemp = mouseEntered;
		mouseEntered = false;
		return mouseEnteredTemp;
	}

	protected static void setMouseEntered(boolean mouseEntered) {
		SpatialEvents.mouseEntered = mouseEntered;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getMouseEnteredEvent() {
		return mouseEnteredEvent;
	}

	protected static void setMouseEnteredEvent(MouseEvent mouseEnteredEvent) {
		SpatialEvents.mouseEnteredEvent = mouseEnteredEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseExited() {
		boolean mouseExitedTemp = mouseExited;
		mouseExited = false;
		return mouseExitedTemp;
	}

	protected static void setMouseExited(boolean mouseExited) {
		SpatialEvents.mouseExited = mouseExited;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getMouseExitedEvent() {
		return mouseExitedEvent;
	}

	protected static void setMouseExitedEvent(MouseEvent mouseExitedEvent) {
		SpatialEvents.mouseExitedEvent = mouseExitedEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseTripleClicked() {
		boolean mouseTripleClickedTemp = mouseTripleClicked;
		mouseTripleClicked = false;

		return mouseTripleClickedTemp;
	}

	protected static void setMouseTripleClicked(boolean mouseTripleClicked) {
		SpatialEvents.mouseTripleClicked = mouseTripleClicked;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getTripleClickEvent() {
		return tripleClickEvent;
	}

	protected static void setTripleClickEvent(MouseEvent tripleClickEvent) {
		SpatialEvents.tripleClickEvent = tripleClickEvent;
	}

	/**
	 * This method returns the boolean value true if the specified mouse event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isMouseWheelClicked() {
		boolean mouseWheelClickedTemp = mouseWheelClicked;
		mouseWheelClicked = false;
		return mouseWheelClickedTemp;
	}

	protected static void setMouseWheelClicked(boolean mouseWheelClicked) {
		SpatialEvents.mouseWheelClicked = mouseWheelClicked;
	}

	/**
	 * This method returns a MouseEvent instance for the specified event.
	 * 
	 * @return a MouseEvent instance.
	 */
	public static MouseEvent getWheelClickEvent() {
		return wheelClickEvent;
	}

	protected static void setWheelClickEvent(MouseEvent wheelClickEvent) {
		SpatialEvents.wheelClickEvent = wheelClickEvent;
	}

	/* The key events! */
	/**
	 * This method returns the boolean value true if the specified key event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isKeyPressed() {
		boolean keyPressedTemp = keyPressed;
		keyPressed = false;
		return keyPressedTemp;
	}

	protected static void setKeyPressed(boolean keyPressed) {
		SpatialEvents.keyPressed = keyPressed;
	}

	/**
	 * This method returns a KeyEvent instance for the specified event.
	 * 
	 * @return a KeyEvent instance.
	 */
	public static KeyEvent getKeyPressEvent() {
		return keyPressEvent;
	}

	protected static void setKeyPressEvent(KeyEvent keyEvent) {
		SpatialEvents.keyPressEvent = keyEvent;
	}

	/**
	 * This method returns the boolean value true if the specified key event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isKeyReleased() {
		boolean keyReleasedTemp = keyReleased;
		keyReleased = false;
		return keyReleasedTemp;
	}

	protected static void setKeyReleased(boolean keyReleased) {
		SpatialEvents.keyReleased = keyReleased;
	}

	/**
	 * This method returns a KeyEvent instance for the specified event.
	 * 
	 * @return a KeyEvent instance.
	 */
	public static KeyEvent getKeyReleaseEvent() {
		return keyReleaseEvent;
	}

	protected static void setKeyReleaseEvent(KeyEvent keyReleaseEvent) {
		SpatialEvents.keyReleaseEvent = keyReleaseEvent;
	}

	/**
	 * This method returns the boolean value true if the specified key event
	 * occurred.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isKeyTyped() {
		boolean keyTypedTemp = keyTyped;
		keyTyped = false;
		return keyTypedTemp;
	}

	protected static void setKeyTyped(boolean keyTyped) {
		SpatialEvents.keyTyped = keyTyped;
	}

	/**
	 * This method returns a KeyEvent instance for the specified event.
	 * 
	 * @return a KeyEvent instance.
	 */
	public static KeyEvent getKeyTypedEvent() {
		return keyTypedEvent;
	}

	protected static void setKeyTypedEvent(KeyEvent keyTypedEvent) {
		SpatialEvents.keyTypedEvent = keyTypedEvent;
	}

}
