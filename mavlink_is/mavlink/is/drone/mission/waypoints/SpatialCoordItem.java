package mavlink.is.drone.mission.waypoints;

import java.util.List;

import mavlink.is.drone.mission.Mission;
import mavlink.is.drone.mission.MissionItem;
import mavlink.is.drone.mission.waypoints.interfaces.Altitudable;
import mavlink.is.protocol.msg_metadata.ardupilotmega.msg_mission_item;
import mavlink.is.utils.coordinates.Coord2D;
import mavlink.is.utils.coordinates.Coord3D;
import mavlink.is.utils.units.Altitude;

public abstract class SpatialCoordItem extends MissionItem implements Altitudable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8097881358834461060L;
	protected Coord3D coordinate;

	public SpatialCoordItem(Mission mission, Coord3D coord) {
		super(mission);
		this.coordinate = coord;
	}

	public SpatialCoordItem(MissionItem item) {
		super(item);
		if (item instanceof SpatialCoordItem) {
			coordinate = ((SpatialCoordItem) item).getCoordinate();
		} else {
			coordinate = new Coord3D(0, 0, new Altitude(0));
		}
	}

	public void setCoordinate(Coord3D coordNew) {
		coordinate = coordNew;
	}

	public Coord3D getCoordinate() {
		return coordinate;
	}

	@Override
	public List<msg_mission_item> packMissionItem() {
		List<msg_mission_item> list = super.packMissionItem();
		msg_mission_item mavMsg = list.get(0);
		mavMsg.x = (float) coordinate.getLat();
		mavMsg.y = (float) coordinate.getLng();
		mavMsg.z = (float) coordinate.getAltitude().valueInMeters();
		return list;
	}

	@Override
	public void unpackMAVMessage(msg_mission_item mavMsg) {
		Altitude alt = new Altitude(mavMsg.z);
		setCoordinate(new Coord3D(mavMsg.x, mavMsg.y, alt));
	}

	@Override
	public void setAltitude(Altitude altitude) {
		coordinate.set(coordinate.getLat(), coordinate.getLng(), altitude);
	}
	
	@Override
	public Altitude getAltitude() {
		return coordinate.getAltitude();
	}

	public void setPosition(Coord2D position) {
		coordinate.set(position);
	}
}