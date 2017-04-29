package com.dronegcs.console_plugin.mission_editor;

import com.dronedb.persistence.scheme.*;
import com.dronedb.persistence.ws.internal.*;
import com.dronedb.persistence.ws.internal.DatabaseRemoteValidationException;
import com.dronegcs.console_plugin.services.DialogManagerSvc;
import com.dronegcs.console_plugin.services.LoggerDisplayerSvc;
import com.geo_tools.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by taljmars on 3/25/17.
 */
@Scope(scopeName = "prototype")
@Component
public class MissionEditorImpl implements ClosableMissionEditor {

    @Autowired @NotNull(message = "Internal Error: Failed to get log displayer")
    private LoggerDisplayerSvc loggerDisplayerSvc;

    @Autowired @NotNull(message = "Internal Error: Failed to get dialog manager")
    private DialogManagerSvc dialogManagerSvc;

    @Autowired @NotNull(message = "Internal Error: Failed to get drone object crud")
    private DroneDbCrudSvcRemote droneDbCrudSvcRemote;

    @Autowired @NotNull(message = "Internal Error: Failed to get query")
    private QuerySvcRemote querySvcRemote;

    @Autowired @NotNull(message = "Internal Error: Failed to get mission object crud")
    private MissionCrudSvcRemote missionCrudSvcRemote;

    private Mission mission;
    private Mission originalMission;

    @Override
    public Mission open(Mission mission) throws MissionUpdateException {
        loggerDisplayerSvc.logGeneral("Setting new mission to mission editor");
        try {
            this.mission = mission;
            this.originalMission = missionCrudSvcRemote.cloneMission(this.mission);
            return this.mission;
        } catch (DatabaseRemoteValidationException e) {
            throw new MissionUpdateException(e.getMessage());
        }
    }

    @Override
    public Mission open(String missionName) throws MissionUpdateException {
        loggerDisplayerSvc.logGeneral("Setting new mission to mission editor");
        try {
            this.mission = (Mission) droneDbCrudSvcRemote.create(Mission.class.getName());
            this.originalMission = null;
            this.mission.setName(missionName);
            this.mission = (Mission) droneDbCrudSvcRemote.update(this.mission);
            return this.mission;
        }
        catch (DatabaseRemoteValidationException e) {
            throw new MissionUpdateException(e.getMessage());
        }
    }

    @Override
    public Mission close(boolean shouldSave) {
        Mission res = this.mission;
        if (!shouldSave) {
            droneDbCrudSvcRemote.delete(mission);
            res = this.originalMission;
        }
        else {
            if (originalMission != null) droneDbCrudSvcRemote.delete(originalMission);
        }
        System.out.println("Before resting " + res);
        this.originalMission = null;
        this.mission = null;
        loggerDisplayerSvc.logGeneral("DroneMission editor finished");
        System.out.println("After resting " + res);
        return res;
    }

    @Override
    public Waypoint createWaypoint() {
        return (Waypoint) missionCrudSvcRemote.createMissionItem(Waypoint.class.getName());
    }

    @Override
    public Waypoint addWaypoint(Coordinate position) throws MissionUpdateException {
        if (isLastItemLandOrRTL()) {
            dialogManagerSvc.showAlertMessageDialog("Waypoints cannot be added to once there is a Land/RTL point");
            return null;
        }
        Waypoint waypoint = createWaypoint();
        Coordinate c3 = new Coordinate(position, 20);
        waypoint.setLat(c3.getLat());
        waypoint.setLon(c3.getLon());
        waypoint.setAltitude(c3.getAltitude());
        return updateMissionItem(waypoint);
    }

    @Override
    public Circle createCirclePoint() {
        return (Circle) missionCrudSvcRemote.createMissionItem(Circle.class.getName());
    }

    @Override
    public Circle addCirclePoint(Coordinate position) throws MissionUpdateException {
        if (isLastItemLandOrRTL()) {
            dialogManagerSvc.showAlertMessageDialog("Waypoints cannot be added to once there is a Land/RTL point");
            return null;
        }
        Circle circle = createCirclePoint();
        Coordinate c3 = new Coordinate(position, 20);
        circle.setLon(c3.getLon());
        circle.setLat(c3.getLat());
        circle.setAltitude(c3.getAltitude());
        return updateMissionItem(circle);
    }

    @Override
    public Land createLandPoint() {
        return (Land) missionCrudSvcRemote.createMissionItem(Land.class.getName());
    }

    @Override
    public Land addLandPoint(Coordinate position) throws MissionUpdateException {
        if (isLastItemLandOrRTL()) {
            dialogManagerSvc.showAlertMessageDialog("RTL/MavlinkLand point was already defined");
            return null;
        }

        Land land = createLandPoint();
        land.setAltitude(20.0);
        land.setLat(position.getLat());
        land.setLon(position.getLon());
        return updateMissionItem(land);
    }

    @Override
    public ReturnToHome createReturnToLunch() {
        return (ReturnToHome) missionCrudSvcRemote.createMissionItem(ReturnToHome.class.getName());
    }

    @Override
    public ReturnToHome addReturnToLunch() throws MissionUpdateException {
        if (isLastItemLandOrRTL()) {
            dialogManagerSvc.showAlertMessageDialog("RTL/MavlinkLand point was already defined");
            return null;
        }
        ReturnToHome returnToHome = createReturnToLunch();
        returnToHome.setAltitude(0.0);
        return updateMissionItem(returnToHome);
    }

    @Override
    public Takeoff createTakeOff() {
        return (Takeoff) missionCrudSvcRemote.createMissionItem(Takeoff.class.getName());
    }

    @Override
    public Takeoff addTakeOff() throws MissionUpdateException {
        if (isFirstItemTakeoff()) {
            dialogManagerSvc.showAlertMessageDialog("MavlinkTakeoff point was already defined");
            return null;
        }

        String val = dialogManagerSvc.showInputDialog("Choose altitude", "",null, null, "5");
        if (val == null) {
            System.out.println(getClass().getName() + " MavlinkTakeoff canceled");
            dialogManagerSvc.showAlertMessageDialog("MavlinkTakeoff must be defined with height");
            return null;
        }
        double altitude = Double.parseDouble((String) val);
        Takeoff takeoff = createTakeOff();
        takeoff.setFinishedAlt(altitude);
        return updateMissionItem(takeoff);
    }

    @Override
    public RegionOfInterest createRegionOfInterest() {
        return (RegionOfInterest) missionCrudSvcRemote.createMissionItem(RegionOfInterest.class.getName());
    }

    @Override
    public RegionOfInterest addRegionOfInterest(Coordinate position) throws MissionUpdateException {
        if (isLastItemLandOrRTL()) {
            dialogManagerSvc.showAlertMessageDialog("Waypoints cannot be added to once there is a MavlinkLand/RTL point");
            return null;
        }

        RegionOfInterest regionOfInterest = createRegionOfInterest();
        regionOfInterest.setAltitude(20.0);
        regionOfInterest.setLat(position.getLat());
        regionOfInterest.setLon(position.getLon());
        return updateMissionItem(regionOfInterest);
    }

    @Override
    public Mission getModifiedMission() {
        return this.mission;
    }

    @Override
    public Mission update(Mission mission) throws MissionUpdateException {
        try {
            this.mission = (Mission) droneDbCrudSvcRemote.update(mission);
            return this.mission;
        }
        catch (Exception e) {
            loggerDisplayerSvc.logError(e.getMessage());
            throw new MissionUpdateException(e.getMessage());
        }
    }

    @Override
    public <T extends MissionItem> void removeMissionItem(T missionItem) throws MissionUpdateException {
        mission.getMissionItemsUids().remove(missionItem.getKeyId().getObjId());
        try {
            droneDbCrudSvcRemote.update(mission);
        }
        catch (com.dronedb.persistence.ws.internal.DatabaseRemoteValidationException e) {
            throw new MissionUpdateException(e.getMessage());
        }
    }

    @Override
    public List<MissionItem> getMissionItems() {
        List<MissionItem> missionItemList = new ArrayList<>();
        List<String> uuidList = mission.getMissionItemsUids();
        uuidList.forEach((String uuid) -> missionItemList.add((MissionItem) droneDbCrudSvcRemote.readByClass(uuid, MissionItem.class.getName())));
        return missionItemList;
    }

    @Override
    public <T extends MissionItem> T updateMissionItem(T missionItem) throws MissionUpdateException {
        // Update Item
        T res = null;
        try {
            res = (T) droneDbCrudSvcRemote.update(missionItem);
            mission.getMissionItemsUids().add(res.getKeyId().getObjId());
            // Update Mission
            droneDbCrudSvcRemote.update(mission);
            return res;
        }
        catch (DatabaseRemoteValidationException e) {
            throw new MissionUpdateException(e.getMessage());
        }
    }

    // Utilities

    private boolean isLastItemLandOrRTL() {
        if (mission.getMissionItemsUids().isEmpty())
            return false;

        String lastUid = mission.getMissionItemsUids().get(mission.getMissionItemsUids().size() - 1);
        MissionItem last = (MissionItem) droneDbCrudSvcRemote.readByClass(lastUid, MissionItem.class.getName());
        return (last instanceof ReturnToHome) || (last instanceof Land);
    }

    public boolean isFirstItemTakeoff() {
        if (mission.getMissionItemsUids().isEmpty())
            return false;

        String uid = mission.getMissionItemsUids().get(0);
        MissionItem missionItem = (MissionItem) droneDbCrudSvcRemote.readByClass(uid, MissionItem.class.getName());
        return missionItem instanceof Takeoff;
    }

}