package mavlink.is.drone.variables;

import java.io.Serializable;
import java.util.List;

import gui.core.dashboard.Dashboard;
import gui.core.mapObjects.Coordinate;
import gui.is.interfaces.ICoordinate;
import gui.is.interfaces.MapPolygon;
import logger.Logger;
import mavlink.core.drone.MyDroneImpl;
import mavlink.is.drone.DroneVariable;
import mavlink.is.drone.DroneInterfaces.DroneEventsType;
import mavlink.is.protocol.msg_metadata.ApmModes;
import mavlink.is.utils.coordinates.Coord2D;
import mavlink.is.utils.geoTools.GeoTools;

public class Perimeter  extends DroneVariable implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7429107483849276132L;
	private MapPolygon pPolygon;
	private boolean pEnforce;
	private boolean pAlert;
	private Coord2D pLastPosition = null;
	private Coord2D pLastPositionInPerimeter = null;
	private ApmModes pMode;
	private boolean pEnforcePermeterRunning = false;
	
	public Perimeter(MyDroneImpl myDroneImpl) {
		super(myDroneImpl);
		pEnforce = false;
		pAlert = false;
		pPolygon = null;
		pMode = myDroneImpl.getState().getMode();
	}
	
	public void setPolygon(MapPolygon perimeterPoly) {
		pPolygon = perimeterPoly;
	}
	
	public void setPosition(Coord2D position) {
		pLastPosition = position;
		
		if (pEnforce || pAlert) {
			if (!isContained()) {
				myDrone.notifyDroneEvent(DroneEventsType.LEFT_PERIMETER);
				if (pEnforce) {
					myDrone.notifyDroneEvent(DroneEventsType.ENFORCING_PERIMETER);
					try {
						if (!pEnforcePermeterRunning) {
							pMode = myDrone.getState().getMode();							
							Dashboard.loggerDisplayerManager.addErrorMessegeToDisplay("Changing flight from " + pMode.getName() + " to " + ApmModes.ROTOR_GUIDED.getName() + " (Enforcing perimeter)");
							myDrone.getGuidedPoint().forcedGuidedCoordinate(getClosestPointOnPerimeterBorder());
							pEnforcePermeterRunning = true;
						}
					} catch (Exception e) {
						Logger.LogErrorMessege(e.toString());
						e.printStackTrace();
					}
				}
			}
			else {
				if (pEnforcePermeterRunning) {
					Dashboard.loggerDisplayerManager.addErrorMessegeToDisplay("Changing flight from " + ApmModes.ROTOR_GUIDED.getName() + " back to " + pMode.getName());
					myDrone.getState().changeFlightMode(pMode);
					pEnforcePermeterRunning = false;
				}
				
			}
		}
	}
	
	public void setEnforce(boolean enforce) {
		if (enforce)
			Dashboard.loggerDisplayerManager.addGeneralMessegeToDisplay("Enable Perimeter enforcement");
		else
			Dashboard.loggerDisplayerManager.addGeneralMessegeToDisplay("Disable Perimeter enforcement");
		pEnforce = enforce;
	}
	
	public void setAlert(boolean alert) {
		if (alert)
			Dashboard.loggerDisplayerManager.addGeneralMessegeToDisplay("Enable perimeter alert");
		else
			Dashboard.loggerDisplayerManager.addGeneralMessegeToDisplay("Disable perimeter alert");
		pAlert = alert;
	}
	
	public boolean isContained() {
		if (pPolygon == null || pLastPosition == null)
			return true;
		
		boolean res = pPolygon.contains(pLastPosition.convertToCoordinate());
		if (res) {
			pLastPositionInPerimeter = pLastPosition;
		}
		return res;
	}
	
	public Coord2D getClosestPointOnPerimeterBorder() {
		if (pLastPositionInPerimeter == null) {
			Dashboard.loggerDisplayerManager.addGeneralMessegeToDisplay("Last position not exist, return closest corner in it");
			return getClosestCornerInPoligon();
		}
		
		return pLastPositionInPerimeter;
	}
	
	public Coord2D getClosestCornerInPoligon() {
		Coordinate closestPoint = new Coordinate(0,0);
		double min_dist = Integer.MAX_VALUE;
		
		List<? extends ICoordinate> lst = pPolygon.getPoints();
		for (int i = 0 ; i < lst.size() ; i++) {
			Coordinate corner = (Coordinate) lst.get(i);
			
			double val = GeoTools.getDistance(pLastPosition, corner.ConvertToCoord2D()).valueInMeters();
			if (val < min_dist) {
				min_dist = val;
				closestPoint = corner;
			}
		}
		
		return closestPoint.ConvertToCoord2D();
	}

	public boolean isAlert() {
		return pAlert;
	}

	public boolean isEnforce() {
		return pEnforce;
	}
}
