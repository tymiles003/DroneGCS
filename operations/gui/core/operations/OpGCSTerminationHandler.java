package gui.core.operations;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import gui.is.events.GuiEvent;
import gui.is.events.GuiEvent.COMMAND;
import gui.operations.OperationHandler;
import gui.services.DialogManagerSvc;
import gui.services.EventPublisherSvc;
import logger.Logger;
import mavlink.drone.Drone;

@ComponentScan("mavlink.core.drone")
@ComponentScan("logger")
@ComponentScan("gui.services")
@Component("opGCSTerminationHandler")
public class OpGCSTerminationHandler extends OperationHandler {
	
	@Autowired @NotNull(message = "Internal Error: Failed to get GUI event publisher")
	protected EventPublisherSvc eventPublisherSvc;
	
	@Autowired @NotNull(message = "Internal Error: Failed to get logger")
	private Logger logger;
	
	@Autowired @NotNull(message = "Internal Error: Failed to get dialog manager")
	private DialogManagerSvc dialogManagerSvc;
	
	@Autowired @NotNull(message = "Internal Error: Failed to get drone")
	private Drone drone;
	
	static int called;
	@PostConstruct
	public void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singletone");
	}

	@Override
	public boolean go() throws InterruptedException {
		if (DialogManagerSvc.YES_OPTION == dialogManagerSvc.showConfirmDialog("Are you sure you wand to exit?", "")) {
    		System.out.println("Bye Bye");
    		logger.LogGeneralMessege("");
    		logger.LogGeneralMessege("Summary:");
    		logger.LogGeneralMessege("--------");
    		logger.LogGeneralMessege("Traveled distance: " + drone.getGps().getDistanceTraveled() + "m");
    		logger.LogGeneralMessege("Max Height: " + drone.getAltitude().getMaxAltitude() + "m");
    		logger.LogGeneralMessege("Max Speed: " + drone.getSpeed().getMaxAirSpeed().valueInMetersPerSecond() + "m/s (" + ((int) (drone.getSpeed().getMaxAirSpeed().valueInMetersPerSecond()*3.6)) + "km/h)");
    		logger.LogGeneralMessege("Flight time: " + drone.getState().getFlightTime() + "");
    		eventPublisherSvc.publish(new GuiEvent(COMMAND.EXIT, this));
			logger.close();
			System.exit(0);
    	}
		
		return false;
	}
}
