package com.dronegcs.console_plugin.services.internal.convertors;

import com.dronedb.persistence.scheme.*;
import com.dronegcs.mavlink.is.drone.mission.ConvertMavlinkVisitor;
import com.dronegcs.mavlink.is.drone.mission.commands.*;
import com.dronegcs.mavlink.is.drone.mission.survey.MavlinkSurvey;
import com.dronegcs.mavlink.is.drone.mission.waypoints.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taljmars on 3/18/17.
 */
public class MavlinkItemToDatabaseConvertor implements ConvertMavlinkVisitor {

    private Mission mission;
    private List<MissionItem> missionItems;

    public MavlinkItemToDatabaseConvertor() {
        missionItems = new ArrayList<>();
    }

    public void setMission(Mission Mission) {
        this.mission = mission;
    }

    public Mission getMission() {
        return mission;
    }

    public List<MissionItem> getMissionItems() {
        return missionItems;
    }

    @Override
    public void visit(MavlinkLand mavlinkLand) {

    }

    @Override
    public void visit(MavlinkReturnToHome mavlinkReturnToHome) {

    }

    @Override
    public void visit(MavlinkTakeoff mavlinkTakeoff) {

    }

    @Override
    public void visit(MavlinkStructureScanner mavlinkStructureScanner) {

    }

    @Override
    public void visit(MavlinkChangeSpeed mavlinkChangeSpeed) {

    }

    @Override
    public void visit(MavlinkRegionOfInterest mavlinkRegionOfInterest) {

    }

    @Override
    public void visit(MavlinkSurvey mavlinkSurvey) {

    }

    @Override
    public void visit(MavlinkEpmGripper mavlinkEpmGripper) {

    }

    @Override
    public void visit(MavlinkCameraTrigger mavlinkCameraTrigger) {

    }

    @Override
    public void visit(MavlinkSplineWaypoint mavlinkSplineWaypoint) {

    }

    @Override
    public void visit(MavlinkWaypoint mavlinkWaypoint) {
        Waypoint waypoint = new Waypoint();
        waypoint.setDelay(mavlinkWaypoint.getDelay());
        waypoint.setAcceptanceRadius(mavlinkWaypoint.getAcceptanceRadius());
        waypoint.setOrbitalRadius(mavlinkWaypoint.getOrbitalRadius());
        waypoint.setOrbitCCW(mavlinkWaypoint.isOrbitCCW());
        waypoint.setYawAngle(mavlinkWaypoint.getYawAngle());
        waypoint.setLat(mavlinkWaypoint.getCoordinate().getLat());
        waypoint.setLon(mavlinkWaypoint.getCoordinate().getLon());
        waypoint.setAltitude(mavlinkWaypoint.getAltitude());
        mission.getMissionItemsUids().add(waypoint.getObjId());
        missionItems.add(waypoint);
    }

    @Override
    public void visit(MavlinkCircle mavlinkCircle) {
        Circle circle = new Circle();
        circle.setAltitude(mavlinkCircle.getAltitude());
        circle.setTurns(mavlinkCircle.getNumberOfTurns());
        circle.setLat(mavlinkCircle.getCoordinate().getLat());
        circle.setLon(mavlinkCircle.getCoordinate().getLon());
        circle.setRadius(mavlinkCircle.getRadius());
        mission.getMissionItemsUids().add(circle.getObjId());
        missionItems.add(circle);
    }

}
