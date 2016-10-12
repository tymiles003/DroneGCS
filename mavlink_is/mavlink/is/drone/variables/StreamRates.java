package mavlink.is.drone.variables;

import gui.is.services.LoggerDisplayerSvc;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import mavlink.is.drone.Drone;
import mavlink.is.drone.DroneVariable;
import mavlink.is.drone.DroneInterfaces.DroneEventsType;
import mavlink.is.drone.DroneInterfaces.OnDroneListener;
import mavlink.is.drone.Preferences.Rates;
import mavlink.is.protocol.msgbuilder.MavLinkStreamRates;

@Component("streamRates")
public class StreamRates extends DroneVariable implements OnDroneListener {

	private static final long serialVersionUID = -4572905995884327453L;
	
	@Resource(name = "loggerDisplayerSvc")
	private LoggerDisplayerSvc loggerDisplayerSvc;
	
	private boolean streamRatesWasSet = false;
	
	static int called;
	public void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singletone");
		drone.addDroneListener(this);
	}

	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		switch (event) {
		case HEARTBEAT_FIRST:
		case HEARTBEAT_RESTORED:
			setupStreamRatesFromPref();
			break;
		default:
			break;
		}
	}

	public void setupStreamRatesFromPref() {
		if (streamRatesWasSet)
			return;
		
		loggerDisplayerSvc.logGeneral("Setting up stream rates");
		Rates rates = drone.getPreferences().getRates();

		MavLinkStreamRates.setupStreamRates(drone.getMavClient(), rates.extendedStatus,
				rates.extra1, rates.extra2, rates.extra3, rates.position, rates.rcChannels,
				rates.rawSensors, rates.rawController);
		
		streamRatesWasSet = true;
	}
	
	public void prepareStreamRates() {
		streamRatesWasSet = false;
	}

}
