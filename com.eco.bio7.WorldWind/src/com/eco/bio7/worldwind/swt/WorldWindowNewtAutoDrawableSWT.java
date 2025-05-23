/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.eco.bio7.worldwind.swt;


import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.WorldWindowGLAutoDrawable;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.render.ScreenCreditController;

import java.util.EventObject;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;

import org.eclipse.swt.widgets.Control;

import com.eco.bio7.worldwind.swt.WorldWindowNewtDrawableSWT;
import com.jogamp.newt.opengl.GLWindow;

/**
 * {@link WorldWindowGLAutoDrawable} subclass used when using a NEWT canvas for
 * rendering. This is required because, in a few methods, the
 * {@link WorldWindowGLAutoDrawable} assumes the {@link GLAutoDrawable} passed
 * to the {@link #initDrawable(GLAutoDrawable)} function is an instanceof
 * {@link java.awt.Component}, which isn't the case for NEWT canvas'.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class WorldWindowNewtAutoDrawableSWT extends WorldWindowGLAutoDrawable implements WorldWindowNewtDrawableSWT
{
	protected GLWindow window;
	protected Control swtControl;

	@Deprecated
	@Override
	public void initDrawable(GLAutoDrawable glAutoDrawable)
	{
		throw new IllegalStateException("WorldWindowNewtDrawable.initDrawable(GLAutoDrawable) should not be invoked"); //$NON-NLS-1$
	}

	@Override
	public void initDrawable(GLWindow window, Control swtControl)
	{
		super.initDrawable(window);
		this.window = window;
		this.swtControl = swtControl;
	}

	@Override
	public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int w, int h)
	{
	}

	@Override
	public void redraw()
	{
		if (swtControl != null)
		{
			swtControl.redraw();
		}
	}

	@Override
	public void endInitialization()
	{
		initializeCreditsController();
	}

	@Override
	protected void initializeCreditsController()
	{
		new ScreenCreditController((WorldWindow) swtControl);
	}

	@Override
	protected void callRenderingListeners(RenderingEvent event)
	{
		//event source should be the world window, not the drawable
		super.callRenderingListeners(new RenderingEvent(translateEventSource(event), event.getStage()));
	}

	@Override
	protected void callPositionListeners(PositionEvent event)
	{
		//event source should be the world window, not the drawable
		super.callPositionListeners(new PositionEvent(translateEventSource(event), event.getScreenPoint(), event
				.getPreviousPosition(), event.getPosition()));
	}

	@Override
	protected void callSelectListeners(SelectEvent event)
	{
		//event source should be the world window, not the drawable
		Object source = translateEventSource(event);
		SelectEvent newEvent =
				event.getMouseEvent() != null ? new SelectEvent(source, event.getEventAction(), event.getMouseEvent(),
						event.getObjects()) : event.getPickRectangle() != null ? new SelectEvent(source,
						event.getEventAction(), event.getPickRectangle(), event.getObjects()) : new SelectEvent(source,
						event.getEventAction(), event.getPickPoint(), event.getObjects());
		super.callSelectListeners(newEvent);
	}

	protected Object translateEventSource(EventObject event)
	{
		//event source should be the world window, not the drawable
		return event.getSource() == window ? swtControl : event.getSource();
	}

	@Override
	public GLContext getContext() {
		// TODO Auto-generated method stub
		return null;
	}
}
