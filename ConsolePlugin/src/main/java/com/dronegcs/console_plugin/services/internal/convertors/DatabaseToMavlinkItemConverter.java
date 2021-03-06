package com.dronegcs.console_plugin.services.internal.convertors;

import com.dronedb.persistence.scheme.*;
import com.dronegcs.console_plugin.mission_editor.MissionsManager;
import com.dronegcs.mavlink.is.drone.mission.DroneMission;
import com.dronegcs.mavlink.is.drone.mission.commands.MavlinkReturnToHome;
import com.dronegcs.mavlink.is.drone.mission.commands.MavlinkTakeoff;
import com.dronegcs.mavlink.is.drone.mission.waypoints.*;
import com.geo_tools.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Created by taljmars on 3/18/17.
 */
@Scope(value = "prototype")
@Component
public class DatabaseToMavlinkItemConverter {

    private final static Logger LOGGER = LoggerFactory.getLogger(DatabaseToMavlinkItemConverter.class);

    @Autowired @NotNull(message = "Internal Error: Failed to get mission manager")
    private MissionsManager missionsManager;

    private DroneMission droneMission;

    public DroneMission convert(Mission mission, DroneMission droneMission) {
        this.droneMission = droneMission;
        this.droneMission.setDefaultAlt(mission.getDefaultAlt());
        Iterator<MissionItem> itr = missionsManager.getMissionItems(mission).iterator();
        while (itr.hasNext())
            eval(itr.next());

        return this.droneMission;
    }

    public void eval(MissionItem missionItem) {
        // Using reflection to call the relevant function
        // It is a factory pattern based on reflection
        LOGGER.debug("Converting {}", missionItem);
        Method[] allMethods = this.getClass().getDeclaredMethods();
        for (int i = 0 ; i < allMethods.length ; i++) {
            Method method = allMethods[i];
            if (!method.getName().equals("visit"))
                continue;
            if (method.getParameterTypes().length != 1)
                continue;
            if (method.getParameterTypes()[0] != missionItem.getClass())
                continue;

            method.setAccessible(true);
            try {
                method.invoke(this, missionItem);
                return;
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Failed to convert missionItem from DB to Mavlink", e);
            }
            break;
        }

        LOGGER.error("Found unknown type: {}, Object: {}", missionItem.getClass(), missionItem);
    }

    private void visit(Land land) {
        MavlinkLand mavlinkLand = new MavlinkLand(droneMission, new Coordinate(land.getLat(), land.getLon()));
        LOGGER.debug("Database Land:\n{}\nWas converted to:\n{}", land, mavlinkLand);
        droneMission.addMissionItem(mavlinkLand);
    }

    private void visit(Takeoff takeoff) {
        MavlinkTakeoff mavlinkTakeoff = new MavlinkTakeoff(droneMission, takeoff.getFinishedAlt());
        LOGGER.debug("Database Takeoff:\n{}\nWas converted to:\n{}", takeoff, mavlinkTakeoff);
        droneMission.addMissionItem(mavlinkTakeoff);
    }

    private void visit(Waypoint waypoint) {
        MavlinkWaypoint mavlinkWaypoint = new MavlinkWaypoint(droneMission, new Coordinate(waypoint.getLat(), waypoint.getLon()));
        mavlinkWaypoint.setYawAngle(waypoint.getYawAngle());
        mavlinkWaypoint.setOrbitCCW(waypoint.isOrbitCCW());
        mavlinkWaypoint.setOrbitalRadius(waypoint.getOrbitalRadius());
        mavlinkWaypoint.setAcceptanceRadius(waypoint.getAcceptanceRadius());
        mavlinkWaypoint.setDelay(waypoint.getDelay());
        mavlinkWaypoint.setAltitude(waypoint.getAltitude());
        LOGGER.debug("Database Waypoint:\n{}\nWas converted to:\n{}", waypoint, mavlinkWaypoint);
        droneMission.addMissionItem(mavlinkWaypoint);
    }

    private void visit(LoiterTurns loiterTurns) {
        MavlinkLoiterTurns mavlinkLoiterTurns = new MavlinkLoiterTurns(droneMission, new Coordinate(loiterTurns.getLat(), loiterTurns.getLon()));
        mavlinkLoiterTurns.setAltitude(loiterTurns.getAltitude());
        mavlinkLoiterTurns.setTurns(loiterTurns.getTurns());
        LOGGER.debug("Database LoiterTurns:\n{}\nWas converted to:\n{}", loiterTurns, mavlinkLoiterTurns);
        droneMission.addMissionItem(mavlinkLoiterTurns);
    }

    private void visit(LoiterTime loiterTime) {
        MavlinkLoiterTime mavlinkLoiterTime = new MavlinkLoiterTime(droneMission, new Coordinate(loiterTime.getLat(), loiterTime.getLon()));
        mavlinkLoiterTime.setAltitude(loiterTime.getAltitude());
        mavlinkLoiterTime.setSeconds(loiterTime.getSeconds());
        LOGGER.debug("Database LoiterTime:\n{}\nWas converted to:\n{}", loiterTime, mavlinkLoiterTime);
        droneMission.addMissionItem(mavlinkLoiterTime);
    }

    private void visit(LoiterUnlimited loiterUnlimited) {
        MavlinkLoiterUnlimited mavlinkLoiterUnlimited = new MavlinkLoiterUnlimited(droneMission, new Coordinate(loiterUnlimited.getLat(), loiterUnlimited.getLon()));
        mavlinkLoiterUnlimited.setAltitude(loiterUnlimited.getAltitude());
        LOGGER.debug("Database LoiterUnlimited:\n{}\nWas converted to:\n{}", loiterUnlimited, mavlinkLoiterUnlimited);
        droneMission.addMissionItem(mavlinkLoiterUnlimited);
    }

    private void visit(ReturnToHome returnToHome) {
        MavlinkReturnToHome mavlinkReturnToHome = new MavlinkReturnToHome(droneMission);
        mavlinkReturnToHome.setHeight(returnToHome.getAltitude());
        LOGGER.debug("Database ReturnToHome:\n{}\nWas converted to:\n{}", returnToHome, mavlinkReturnToHome);
        droneMission.addMissionItem(mavlinkReturnToHome);
    }

    private void visit(RegionOfInterest regionOfInterest) {
        Coordinate coordinate = new Coordinate(regionOfInterest.getLat() ,regionOfInterest.getLon());
        MavlinkRegionOfInterest mavlinkRegionOfInterest = new MavlinkRegionOfInterest(droneMission, coordinate);
        mavlinkRegionOfInterest.setAltitude(regionOfInterest.getAltitude());
        LOGGER.debug("Database RegionOfInterest:\n{}\nWas converted to:\n{}", regionOfInterest, mavlinkRegionOfInterest);
        droneMission.addMissionItem(mavlinkRegionOfInterest);
    }

    private void visit(SplineWaypoint splineWaypoint) {
        Coordinate coordinate = new Coordinate(splineWaypoint.getLat() ,splineWaypoint.getLon());
        MavlinkSplineWaypoint mavlinkSplineWaypoint = new MavlinkSplineWaypoint(droneMission, coordinate);
        mavlinkSplineWaypoint.setDelay(splineWaypoint.getDelay());
        LOGGER.debug("Database SplineWaypoint:\n{}\nWas converted to:\n{}", splineWaypoint, mavlinkSplineWaypoint);
        droneMission.addMissionItem(mavlinkSplineWaypoint);
    }

}
