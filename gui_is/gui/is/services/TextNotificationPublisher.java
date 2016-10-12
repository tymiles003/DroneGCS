package gui.is.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component("textNotificationPublisher")
public class TextNotificationPublisher {
	
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	public void publish(String msg) {
		System.out.println("Publishing event '" + msg + "'");
		applicationEventPublisher.publishEvent(msg);
	}
}
