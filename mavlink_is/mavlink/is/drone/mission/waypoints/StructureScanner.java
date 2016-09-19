package mavlink.is.drone.mission.waypoints;

import java.util.ArrayList;
import java.util.List;

import mavlink.is.drone.mission.Mission;
import mavlink.is.drone.mission.MissionItem;
import mavlink.is.drone.mission.MissionItemType;
import mavlink.is.drone.mission.survey.CameraInfo;
import mavlink.is.drone.mission.survey.Survey;
import mavlink.is.drone.mission.survey.SurveyData;
import mavlink.is.drone.mission.survey.grid.GridBuilder;
import mavlink.is.protocol.msg_metadata.ardupilotmega.msg_mission_item;
import mavlink.is.protocol.msg_metadata.enums.MAV_CMD;
import mavlink.is.shapes.polygon.Polygon;
import mavlink.is.utils.coordinates.Coord2D;
import mavlink.is.utils.coordinates.Coord3D;
import mavlink.is.utils.geoTools.GeoTools;
import mavlink.is.utils.units.Altitude;
import mavlink.is.utils.units.Length;

public class StructureScanner extends SpatialCoordItem {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3448030594573447989L;
	private Length radius = new Length(10.0);
	private Altitude heightStep = new Altitude(5);
	private int numberOfSteps = 2;
	private boolean crossHatch = false;
	SurveyData survey = new SurveyData();

	public StructureScanner(Mission mission, Coord3D coord) {
		super(mission,coord);
	}

	public StructureScanner(MissionItem item) {
		super(item);
	}

	@Override
	public List<msg_mission_item> packMissionItem() {
		List<msg_mission_item> list = new ArrayList<msg_mission_item>();
		packROI(list);
		packCircles(list);
		if (crossHatch) {
			packHatch(list);			
		}
		return list;
	}

	private void packROI(List<msg_mission_item> list) {
		RegionOfInterest roi = new RegionOfInterest(mission, new Coord3D(
				coordinate, new Altitude(0.0)));
		list.addAll(roi.packMissionItem());
	}

	private void packCircles(List<msg_mission_item> list) {
		for (double altitude = coordinate.getAltitude().valueInMeters(); altitude <= getTopHeight().valueInMeters(); altitude += heightStep.valueInMeters()) {
			Circle circle = new Circle(mission, new Coord3D(coordinate,	new Altitude(altitude)));
			circle.setRadius(radius.valueInMeters());
			list.addAll(circle.packMissionItem());
		}
	}

	private void packHatch(List<msg_mission_item> list) {
		Polygon polygon = new Polygon();
		for (double angle = 0; angle <= 360; angle += 10) {
			polygon.addPoint(GeoTools.newCoordFromBearingAndDistance(coordinate,
					angle, radius.valueInMeters()));
		}

		Coord2D corner = GeoTools.newCoordFromBearingAndDistance(coordinate,
				-45, radius.valueInMeters()*2);
		
		
		survey.setAltitude(getTopHeight());
		
		try {
			survey.update(0.0, survey.getAltitude(), survey.getOverlap(), survey.getSidelap());
			GridBuilder grid = new GridBuilder(polygon, survey, corner);
			for (Coord2D point : grid.generate(false).gridPoints) {
				list.add(Survey.packSurveyPoint(point, getTopHeight()));
			}
			
			survey.update(90.0, survey.getAltitude(), survey.getOverlap(), survey.getSidelap());
			GridBuilder grid2 = new GridBuilder(polygon, survey, corner);
			for (Coord2D point : grid2.generate(false).gridPoints) {
				list.add(Survey.packSurveyPoint(point, getTopHeight()));
			}
		} catch (Exception e) { // Should never fail, since it has good polygons
		}

	}

	public List<Coord2D> getPath() {
		List<Coord2D> path = new ArrayList<Coord2D>();
		for (msg_mission_item msg_mission_item : packMissionItem()) {
			if (msg_mission_item.command == MAV_CMD.MAV_CMD_NAV_WAYPOINT) {
				path.add(new Coord2D(msg_mission_item.x, msg_mission_item.y));
			}
			if (msg_mission_item.command == MAV_CMD.MAV_CMD_NAV_LOITER_TURNS) {
				for (double angle = 0; angle <= 360; angle += 12) {
					path.add(GeoTools.newCoordFromBearingAndDistance(coordinate,angle, radius.valueInMeters()));
				}
			}
			
		}
		return path;

	}

	@Override
	public void unpackMAVMessage(msg_mission_item mavMsg) {
	}

	@Override
	public MissionItemType getType() {
		return MissionItemType.CYLINDRICAL_SURVEY;
	}
	


	private Altitude getTopHeight() {
		return new Altitude(coordinate.getAltitude().valueInMeters()+ (numberOfSteps-1)*heightStep.valueInMeters());
	}

	public Altitude getEndAltitude() {
		return heightStep;
	}

	public int getNumberOfSteps() {
		return numberOfSteps;
	}

	public Length getRadius() {
		return radius;
	}

	public Coord2D getCenter() {
		return coordinate;
	}

	public void setRadius(int newValue) {
		radius = new Length(newValue);
	}

	public void enableCrossHatch(boolean isEnabled) {
		crossHatch = isEnabled;
	}

	public boolean isCrossHatchEnabled() {
		return crossHatch;
	}

	public void setAltitudeStep(int newValue) {
		heightStep = new Altitude(newValue);		
	}

	public void setNumberOfSteps(int newValue) {
		numberOfSteps = newValue;	
	}

	public void setCamera(CameraInfo cameraInfo) {
		survey.setCameraInfo(cameraInfo);
	}

	public String getCamera() {
		return survey.getCameraName();
	}

}