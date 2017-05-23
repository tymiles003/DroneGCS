package com.dronegcs.mavlink.is.drone.mission.waypoints;

import com.dronegcs.mavlink.is.drone.mission.DroneMission;
import com.dronegcs.mavlink.is.drone.mission.DroneMissionItem;
import com.dronegcs.mavlink.is.drone.mission.waypoints.interfaces.MavlinkAltitudable;
import com.dronegcs.mavlink.is.protocol.msg_metadata.ardupilotmega.msg_mission_item;
import com.geo_tools.Coordinate;

import java.util.List;

public abstract class SpatialCoordItemDrone extends DroneMissionItem implements MavlinkAltitudable {

	protected Coordinate coordinate;

	public SpatialCoordItemDrone(DroneMission droneMission, Coordinate coord) {
		super(droneMission);
		this.coordinate = coord;
	}

	public SpatialCoordItemDrone(DroneMissionItem item) {
		super(item);
		if (item instanceof SpatialCoordItemDrone) {
			coordinate = ((SpatialCoordItemDrone) item).getCoordinate();
		} else {
			coordinate = new Coordinate(0, 0, 0);
		}
	}

	public void setCoordinate(Coordinate coordNew) {
		coordinate = coordNew;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	@Override
	public List<msg_mission_item> packMissionItem() {
		List<msg_mission_item> list = super.packMissionItem();
		msg_mission_item mavMsg = list.get(0);
		mavMsg.x = (float) coordinate.getLat();
		mavMsg.y = (float) coordinate.getLon();
		mavMsg.z = (float) coordinate.getAltitude();
		return list;
	}

	@Override
	public void unpackMAVMessage(msg_mission_item mavMsg) {
		double alt = mavMsg.z;
		setCoordinate(new Coordinate(mavMsg.x, mavMsg.y, alt));
	}

	@Override
	public void setAltitude(double altitude) {
		coordinate.set(coordinate.getLat(), coordinate.getLon(), altitude);
	}
	
	@Override
	public double getAltitude() {
		return coordinate.getAltitude();
	}

	public void setPosition(Coordinate position) {
		coordinate.set(position.getLat(), position.getLon(), position.getAltitude());
	}
}