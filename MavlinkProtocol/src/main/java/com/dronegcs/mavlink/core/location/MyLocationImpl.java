package com.dronegcs.mavlink.core.location;

import com.dronegcs.mavlink.core.connection.helper.BeaconData;
import com.dronegcs.mavlink.core.connection.helper.BeaconDataFactory;
import com.dronegcs.mavlink.is.location.Location;
import com.dronegcs.mavlink.is.location.LocationFinder;
import com.dronegcs.mavlink.is.location.LocationReceiver;
import com.generic_tools.logger.Logger;
import com.geo_tools.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class MyLocationImpl implements LocationFinder {
	
	private static final int UPDATE_INTERVAL = 1000;// TALMA was 500;
	private static final double SPEED = 3;

	@Autowired @NotNull(message = "Internal Error: Failed to get com.dronegcs.gcsis.logger")
	private Logger logger;

	@Autowired @NotNull(message = "Internal Error: Failed to get beacon factory")
	private BeaconDataFactory beaconDataFactory;
	
	private Set<LocationReceiver> receivers = null;
	private TimerTask scTimerTask = null;

	
	public MyLocationImpl() {
		receivers = new HashSet<LocationReceiver>();
	}

	@Override
	public void addLocationListener(LocationReceiver receiver) {
		this.receivers.add(receiver);
	}
	
	@Override
	public void removeLocationListener(LocationReceiver receiver) {
		this.receivers.remove(receiver);
	}

	@Override
	public void enableLocationUpdates() {
		Timer timer = new Timer();
		logger.LogDesignedMessege(getClass() + " Location updates started!");
		scTimerTask = new TimerTask() {
			@Override
			public void run() {
				
				BeaconData beaconDate = beaconDataFactory.fetch();
				if (beaconDate == null) {
					logger.LogErrorMessege("Failed to get beacon point from the web");
					return;
				}
				logger.LogDesignedMessege("Request took " + beaconDate.getFetchTime() + "ms");				
				Coordinate coord = beaconDate.getCoordinate().dot(1);
				
				for (LocationReceiver lr : receivers)
					lr.onLocationChanged(new Location(coord, 0, (float) SPEED, true));
			}
		};
		timer.scheduleAtFixedRate(scTimerTask, 0, UPDATE_INTERVAL);

	}

	@Override
	public void disableLocationUpdates() {
		if (scTimerTask != null)
			scTimerTask.cancel();
		
		scTimerTask = null;
		
		logger.LogDesignedMessege(getClass() + " Location updates canceled!");
	}
	
	static int called;
	@PostConstruct
	public void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singleton");
	}
}