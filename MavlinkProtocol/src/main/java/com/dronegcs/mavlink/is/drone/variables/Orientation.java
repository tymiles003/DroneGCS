package com.dronegcs.mavlink.is.drone.variables;

import com.dronegcs.mavlink.is.drone.DroneInterfaces.DroneEventsType;
import com.dronegcs.mavlink.is.drone.DroneVariable;
import org.springframework.stereotype.Component;

@Component
public class Orientation extends DroneVariable {
	
	private double roll = 0;
	private double pitch = 0;
	private double yaw = 0;

	public double getRoll() {
		return roll;
	}

	public double getPitch() {
		return pitch;
	}

	public double getYaw() {
		return yaw;
	}

	public void setRollPitchYaw(double roll, double pitch, double yaw) {
		this.roll = roll;
		this.pitch = pitch;
		this.yaw = yaw;
		drone.notifyDroneEvent(DroneEventsType.ATTITUDE);
	}

}