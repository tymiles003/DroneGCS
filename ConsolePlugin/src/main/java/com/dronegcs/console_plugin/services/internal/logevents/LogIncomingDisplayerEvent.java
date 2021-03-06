package com.dronegcs.console_plugin.services.internal.logevents;

import com.generic_tools.logger.Logger.Type;

/**
 * Incoming event type
 * 
 * @author taljmars
 *
 */
public class LogIncomingDisplayerEvent extends LogAbstractDisplayerEvent {
	
	/**
	 * @param message
	 */
	public LogIncomingDisplayerEvent(String message) {
		super(Type.INCOMING, message);
	}
}
