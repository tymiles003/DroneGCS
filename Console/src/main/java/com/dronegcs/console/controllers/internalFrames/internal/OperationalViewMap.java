//==============================================================================
//   JFXMapPane is a Java library for parsing raw weather data
//   Copyright (C) 2012 Jeffrey L Smith
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//    
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//    
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//    
//  For more information, please email jsmith.carlsbad@gmail.com
//    
//==============================================================================
package com.dronegcs.console.controllers.internalFrames.internal;

import java.awt.Point;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

import com.dronedb.persistence.scheme.CirclePerimeter;
import com.dronedb.persistence.scheme.Mission;
import com.dronedb.persistence.scheme.Perimeter;
import com.dronedb.persistence.scheme.PolygonPerimeter;
import com.dronegcs.console.controllers.internalFrames.internal.view_tree_layers.LayerMission;
import com.dronegcs.console.controllers.internalFrames.internal.view_tree_layers.LayerPolygonPerimeter;
import com.dronegcs.console_plugin.mission_editor.MissionEditor;
import com.dronegcs.console_plugin.mission_editor.MissionUpdateException;
import com.dronegcs.console_plugin.mission_editor.MissionsManager;
import com.dronegcs.console_plugin.perimeter_editor.PerimeterUpdateException;
import com.dronegcs.console_plugin.perimeter_editor.PerimeterEditor;
import com.dronegcs.console_plugin.perimeter_editor.PerimetersManager;
import com.dronegcs.console_plugin.perimeter_editor.PolygonPerimeterEditor;
import com.dronegcs.console_plugin.services.EventPublisherSvc;
import javafx.scene.input.MouseButton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.dronegcs.console.controllers.internalFrames.internal.view_tree_layers.LayerCircledPerimeter;
import com.dronegcs.console.controllers.internalFrames.internal.view_tree_layers.LayerPerimeter;
import com.generic_tools.devices.KeyBoardController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import com.gui.core.mapTreeObjects.Layer;
import com.gui.core.mapViewer.LayeredViewMap;
import com.gui.core.mapViewerObjects.MapLineImpl;
import com.gui.core.mapViewerObjects.MapMarkerCircle;
import com.gui.core.mapViewerObjects.MapMarkerDot;
import com.gui.core.mapViewerObjects.MapVectorImpl;
import com.gui.is.interfaces.mapObjects.MapLine;
import com.gui.is.interfaces.mapObjects.MapMarker;
import com.dronegcs.console_plugin.services.DialogManagerSvc;
import com.dronegcs.console_plugin.services.LoggerDisplayerSvc;
import com.dronegcs.console_plugin.services.TextNotificationPublisherSvc;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import com.dronegcs.mavlink.is.drone.Drone;
import com.dronegcs.mavlink.is.drone.DroneInterfaces.DroneEventsType;
import com.dronegcs.mavlink.is.drone.DroneInterfaces.OnDroneListener;
import com.dronegcs.mavlink.is.drone.variables.GuidedPoint;
import com.dronegcs.mavlink.is.drone.variables.Home;
import com.geo_tools.Coordinate;
import com.geo_tools.GeoTools;
import com.dronegcs.console_plugin.services.internal.logevents.QuadGuiEvent;
import com.generic_tools.validations.RuntimeValidator;
import com.generic_tools.validations.ValidatorResponse;

/**
 *
 * @author taljmars
 */

@Component
public class OperationalViewMap extends LayeredViewMap implements
OnDroneListener, EventHandler<ActionEvent> {
	
	@Autowired @NotNull( message = "Internal Error: Failed to get drone" )
	private Drone drone;

	@Autowired @NotNull( message = "Internal Error: Failed to get mission manager" )
	private MissionsManager missionsManager;

	@Autowired @NotNull( message = "Internal Error: Failed to get perimeter manager" )
	private PerimetersManager perimetersManager;
	
	@Autowired @NotNull( message = "Internal Error: Failed to get keyboard controller" )
	private KeyBoardController keyboardController;
	
	@Autowired @NotNull( message = "Internal Error: Failed to get notification publisher" )
	private TextNotificationPublisherSvc textNotificationPublisherSvc;
	
	@Autowired @NotNull( message = "Internal Error: Failed to get com.generic_tools.logger displayer" )
	private LoggerDisplayerSvc loggerDisplayerSvc;
	
	@Autowired @NotNull(message = "Internal Error: Failed to get dialog manager")
	private DialogManagerSvc dialogManagerSvc;

	@Autowired @NotNull( message = "Internal Error: Failed to get event publisher service" )
	protected EventPublisherSvc eventPublisherSvc;

	@Autowired @NotNull(message = "Internal Error: Failed to get application context")
	public ApplicationContext applicationContext;

	@Resource(type = OperationalViewTree.class)
	public void setOperationalViewTree(OperationalViewTree operationalViewTree) {
		super.setCheckBoxViewTree(operationalViewTree);
	}

	public OperationalViewTree getOperationalViewTree() {
		return (OperationalViewTree) super.getCheckBoxViewTree();
	}
	
	@Autowired
	private RuntimeValidator runtimeValidator;

	private boolean lockMapOnMyPosition = true;
	private boolean paintTrail = true;
	private MapLineImpl myTrailPath = null;
	
	private MapMarkerDot guidedPoint = null;
	private MapMarkerDot myPos = null;
	private MapLine bearing = null;
	private MapMarkerDot myHome = null;
	private MapMarkerDot myBeacon = null;
	private MapMarkerDot myGCS = null;
	
	// DroneMission Builder
	private LayerMission modifyiedLayerMissionOriginal = null;

	// Perimeter Builder
	private LayerPerimeter modifyiedLayerPerimeterOriginal = null;
	
	private boolean isPerimeterBuildMode = false;
	private boolean isMissionBuildMode = false;
	
	private CheckBox cbLockMyPos;
	private CheckBox cbFollowTrail;
	
	private ContextMenu popup;

	private MissionEditor missionEditor;
	private PerimeterEditor perimeterEditor;
	
	private static int called;
	@PostConstruct
	private void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singleton");
		
		setDisplayPosition(new Coordinate(32.0684, 34.8248), 10);
		drone.addDroneListener(this);

		ValidatorResponse validatorResponse = runtimeValidator.validate(this);
		if (validatorResponse.isFailed())
			throw new RuntimeException(validatorResponse.toString());
	}
	
	private ContextMenu buildPopup(Point point) {
		// TODO: remove it
		System.out.println("Building popup");
		ContextMenu popup = new ContextMenu();		
		
		MenuItem menuItemFlyTo = new MenuItem("Fly to Position");
		MenuItem menuItemMissionBuild = new MenuItem("Build Mission");
		MenuItem menuItemMissionAddWayPoint = new MenuItem("Add Way Point");
		MenuItem menuItemMissionAddCircle = new MenuItem("Add Circle");
		MenuItem menuItemMissionAddROI = new MenuItem("Add ROI");
		MenuItem menuItemMissionSetHome = new MenuItem("Set Home");
		MenuItem menuItemMissionSetLandPoint = new MenuItem("Set Land Point");
		MenuItem menuItemMissionSetRTL = new MenuItem("Set RTL");
		MenuItem menuItemMissionSetTakeOff = new MenuItem("Set MavlinkTakeoff");
		MenuItem menuItemDist = new MenuItem("Distance -m");
		MenuItem menuItemPerimeterBuild = new MenuItem("Build Perimeter");
		MenuItem menuItemPerimeterAddPoint = new MenuItem("Add Point");
		MenuItem menuItemSyncMission = new MenuItem("Sync Mission");
		MenuItem menuItemFindClosest = new MenuItem("Find closest Here");

		menuItemFlyTo.setDisable(!drone.getGps().isPositionValid());
		menuItemDist.setDisable(!drone.getGps().isPositionValid());
		menuItemMissionAddWayPoint.setVisible(isMissionBuildMode);
		menuItemMissionAddCircle.setVisible(isMissionBuildMode);
		menuItemMissionAddROI.setVisible(isMissionBuildMode);
		menuItemMissionSetHome.setVisible(drone.getGps().isPositionValid() && !isMissionBuildMode && !isPerimeterBuildMode);
		menuItemMissionSetLandPoint.setVisible(isMissionBuildMode);
		menuItemMissionSetRTL.setVisible(isMissionBuildMode);
		menuItemMissionSetTakeOff.setVisible(isMissionBuildMode);
		menuItemMissionBuild.setDisable(isMissionBuildMode || isPerimeterBuildMode);
		menuItemPerimeterBuild.setDisable(isMissionBuildMode || isPerimeterBuildMode);
		menuItemSyncMission.setDisable(isMissionBuildMode || isPerimeterBuildMode);
		menuItemPerimeterAddPoint.setVisible(isPerimeterBuildMode && (modifyiedLayerPerimeterOriginal instanceof LayerPolygonPerimeter));

		if (drone.getGps().isPositionValid()) {
			Coordinate iCoord = getPosition(point);
			Coordinate to = new Coordinate(iCoord.getLat(), iCoord.getLon());
			Coordinate from = drone.getGps().getPosition();
			int dist = (int) GeoTools.getDistance(from, to);
			menuItemDist.setText("Distance " + dist + "m");
		}

		// Create the popup menu.
		popup.getItems().add(menuItemFlyTo);
		popup.getItems().add(menuItemDist);
		//popup.getItems().addSeparator();
		popup.getItems().add(menuItemMissionBuild);
		popup.getItems().add(menuItemPerimeterBuild);
		//popup.getItems().addSeparator();
		popup.getItems().add(menuItemMissionAddWayPoint);
		popup.getItems().add(menuItemMissionAddCircle);
		popup.getItems().add(menuItemMissionAddROI);
		popup.getItems().add(menuItemMissionSetLandPoint);
		popup.getItems().add(menuItemMissionSetRTL);
		popup.getItems().add(menuItemMissionSetHome);
		popup.getItems().add(menuItemMissionSetTakeOff);
		popup.getItems().add(menuItemPerimeterAddPoint);
		//popup.getItems().addSeparator();
		popup.getItems().add(menuItemSyncMission);
		popup.getItems().add(menuItemFindClosest);
		
		menuItemFindClosest.setOnAction( arg -> {
			MapMarker mm = new MapMarkerDot(getPosition(point));
			addMapMarker(mm);
		});

		menuItemFlyTo.setOnAction( arg -> {
				if (!drone.getGps().isPositionValid()) {
					dialogManagerSvc.showAlertMessageDialog("Drone must have a GPS connection to use guideness");
					return;
				}
				if (!GuidedPoint.isGuidedMode(drone)) {
					int n = dialogManagerSvc.showConfirmDialog("Drone Mode must be changed to GUIDED inorder to set point.\n"
									+ "Would you like to change mode?", "");
					if (n == DialogManagerSvc.NO_OPTION) {
						return;
					}
				}

				try {
					if (guidedPoint != null) {
						guidedPoint.setColor(Color.GRAY);
					}

					Coordinate coord = getPosition(point);
					Coordinate coord2d = new Coordinate(coord.getLat(), coord.getLon());

					drone.getGuidedPoint().forcedGuidedCoordinate(coord2d);

					guidedPoint = new MapMarkerDot(coord.getLat(), coord.getLon());
					addMapMarker(guidedPoint);
					loggerDisplayerSvc.logGeneral("Flying to guided point " + guidedPoint.getCoordinate().toString());
				} catch (Exception e1) {
					dialogManagerSvc.showErrorMessageDialog("Failed to handle FlyTo request", e1);
				}
			}
		);

		menuItemSyncMission.setOnAction( arg -> {
				System.out.println(getClass().getName() + " Start Sync DroneMission");
				drone.getWaypointManager().getWaypoints();
				loggerDisplayerSvc.logOutgoing("Send Sync Request");
			}
		);
		
		menuItemPerimeterBuild.setOnAction( arg -> {
			try {
				System.out.println(getClass().getName() + " Start GeoFence");
				String[] options = { "Cycle", "Polygon", "Cancel" };
				int n = dialogManagerSvc
						.showOptionsDialog("Choose a way to create perimeter.",
								"Perimeter Limitation",
								null, options,
								options[2]);
				switch (n) {
				case 0:
					removeMapMarker(perimeterBreachPointMarker);
					perimeterBreachPointMarker = null;
					loggerDisplayerSvc.logGeneral("Start GeoFence of perimeter type");
					
					if (modifyiedLayerPerimeterOriginal == null) {
						modifyiedLayerPerimeterOriginal = new LayerCircledPerimeter(OperationalViewTree.EDIT_PREFIX + "New Circled Perimeter", this);
						modifyiedLayerPerimeterOriginal.setApplicationContext(applicationContext);
						getOperationalViewTree().addLayer(modifyiedLayerPerimeterOriginal);

						isPerimeterBuildMode = true;
						perimeterEditor = perimetersManager.openPerimeterEditor(modifyiedLayerPerimeterOriginal.getName(), CirclePerimeter.class);
						modifyiedLayerPerimeterOriginal.setPerimeter(perimeterEditor.getModifiedPerimeter());

						System.err.println(getOperationalViewTree().dumpTree());
						super.startModifiedLayerMode(modifyiedLayerPerimeterOriginal);
					}
					
					loggerDisplayerSvc.logGeneral("Start GeoFence of manual circle type");
					break;
				case 1:
					removeMapMarker(perimeterBreachPointMarker);
					perimeterBreachPointMarker = null;
					loggerDisplayerSvc.logGeneral("Start GeoFence of perimeter type");

					if (modifyiedLayerPerimeterOriginal == null) {
						modifyiedLayerPerimeterOriginal = new LayerPolygonPerimeter(OperationalViewTree.EDIT_PREFIX + "New Polygon Perimeter", this);
						modifyiedLayerPerimeterOriginal.setApplicationContext(applicationContext);
						getOperationalViewTree().addLayer(modifyiedLayerPerimeterOriginal);

						isPerimeterBuildMode = true;
						perimeterEditor = perimetersManager.openPerimeterEditor(modifyiedLayerPerimeterOriginal.getName(), PolygonPerimeter.class);
						modifyiedLayerPerimeterOriginal.setPerimeter(perimeterEditor.getModifiedPerimeter());

						System.err.println(getOperationalViewTree().dumpTree());
						super.startModifiedLayerMode(modifyiedLayerPerimeterOriginal);
					}

					break;
				default:
					return;
				}
			}
			catch (PerimeterUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});

		menuItemMissionBuild.setOnAction( arg -> {
			try {
				if (modifyiedLayerMissionOriginal == null) {
					//modifyiedLayerMission = (LayerMission) AppConfig.context.getBean("layerMission");
					// TALMA i am trying not to use bean here
					modifyiedLayerMissionOriginal = new LayerMission(OperationalViewTree.EDIT_PREFIX + "New DroneMission", this);
					modifyiedLayerMissionOriginal.setApplicationContext(applicationContext);
					getOperationalViewTree().addLayer(modifyiedLayerMissionOriginal);
					//TODO: Fix after handling db
//					DroneMission msn = applicationContext.getBean(DroneMission.class);
//					msn.setDrone(drone);
//					modifyiedLayerMission.setMission(msn);
					isMissionBuildMode = true;
					missionEditor = missionsManager.openMissionEditor(modifyiedLayerMissionOriginal.getName());
					modifyiedLayerMissionOriginal.setMission(missionEditor.getModifiedMission());
					
					super.startModifiedLayerMode(modifyiedLayerMissionOriginal);
					eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_EDITING_STARTED, modifyiedLayerMissionOriginal));
				}
			}
			catch (MissionUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});

		menuItemMissionAddWayPoint.setOnAction( arg -> {
			try {
				missionEditor.addWaypoint(getPosition(point));
				eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_UPDATED_BY_MAP, modifyiedLayerMissionOriginal));
				modifyiedLayerMissionOriginal.regenerateMapObjects();
			}
			catch (MissionUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});

		menuItemMissionAddCircle.setOnAction( arg -> {
			try {
				missionEditor.addCirclePoint(getPosition(point));
				eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_UPDATED_BY_MAP, modifyiedLayerMissionOriginal));
				modifyiedLayerMissionOriginal.regenerateMapObjects();
			}
			catch (MissionUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});

		menuItemMissionSetLandPoint.setOnAction( arg -> {
			try {
				missionEditor.addLandPoint(getPosition(point));
				eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_UPDATED_BY_MAP, modifyiedLayerMissionOriginal));
				modifyiedLayerMissionOriginal.regenerateMapObjects();
			}
			catch (MissionUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});
		
		menuItemMissionAddROI.setOnAction( arg -> {
			try{
				missionEditor.addRegionOfInterest(getPosition(point));
				eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_UPDATED_BY_MAP, modifyiedLayerMissionOriginal));
				modifyiedLayerMissionOriginal.regenerateMapObjects();
			}
				catch (MissionUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});

		menuItemMissionSetRTL.setOnAction( arg -> {
			try {
				missionEditor.addReturnToLunch();
				eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_UPDATED_BY_MAP, modifyiedLayerMissionOriginal));
				modifyiedLayerMissionOriginal.regenerateMapObjects();
			}
			catch (MissionUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});

		menuItemMissionSetTakeOff.setOnAction( arg -> {
			try {
				missionEditor.addTakeOff();
				eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_UPDATED_BY_MAP, modifyiedLayerMissionOriginal));
				modifyiedLayerMissionOriginal.regenerateMapObjects();
			}
			catch (MissionUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});

		menuItemPerimeterAddPoint.setOnAction( arg -> {
			try {
				((PolygonPerimeterEditor) perimeterEditor).addPoint(getPosition(point));
				modifyiedLayerPerimeterOriginal.regenerateMapObjects();
			}
			catch (PerimeterUpdateException e) {
				loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
			}
		});

		return popup;
	}
	
	@Override
	protected void HandleMouseClick(MouseEvent me) {
		// TODO: remove peint
		System.out.println("Mouse click " + me);
		if (popup != null)
			popup.hide();
		
		if (!me.getButton().equals(MouseButton.SECONDARY))
			return;

		System.out.println("Get point");
		Point point = new Point((int) me.getX(), (int) me.getY());
		popup = buildPopup(point);
		//this.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {if (e.getButton() == MouseButton.SECONDARY)   popup.show(this, e.getScreenX(), e.getScreenY()); else popup.hide();});
		popup.show(this, me.getScreenX(), me.getScreenY());
	}
	
	private void SetBearing(double deg) {
		if (!drone.getGps().isPositionValid())
			return;

		if (bearing != null && bearing.getBearing() == deg)
			return;

		if (bearing != null)
			removeMapLine(bearing);

		Coordinate start = drone.getGps().getPosition();
		Coordinate end = GeoTools.newCoordFromBearingAndDistance(start, deg /* + 180 */, 300);

		bearing = new MapVectorImpl(start, end, Color.RED);
		addMapLine(bearing);
	}

	private void removeBearing() {
		if (bearing != null) {
			removeMapLine(bearing);
			bearing = null;
		}
	}

	private MapMarkerCircle myMapCircle25 = null;
	private MapMarkerCircle myMapCircle50 = null;
	private MapMarkerCircle myMapCircle75 = null;
	private MapMarkerCircle myMapCircle100 = null;

	private void SetHome(Home home) {
		if (home == null || !home.isValid())
			return;

		if (myHome != null && myHome.getCoordinate().equals(home.getCoord()))
			return;

		if (myHome != null)
			removeMapMarker(myHome);

		if (myMapCircle25 != null)
			removeMapMarker(myMapCircle25);

		if (myMapCircle50 != null)
			removeMapMarker(myMapCircle50);

		if (myMapCircle75 != null)
			removeMapMarker(myMapCircle75);

		if (myMapCircle100 != null)
			removeMapMarker(myMapCircle100);

		myHome = new MapMarkerDot(Color.BLUE, home.getCoord());
		addMapMarker(myHome);

		myMapCircle25 = new MapMarkerCircle(myHome.getCoordinate(), 25);
		myMapCircle50 = new MapMarkerCircle(myHome.getCoordinate(), 50);
		myMapCircle75 = new MapMarkerCircle(myHome.getCoordinate(), 70);
		myMapCircle100 = new MapMarkerCircle(myHome.getCoordinate(), 100);
		addMapMarker(myMapCircle25);
		addMapMarker(myMapCircle50);
		addMapMarker(myMapCircle75);
		addMapMarker(myMapCircle100);

		loggerDisplayerSvc.logGeneral("Setting new Home position");
	}

	private void EditModeOff() {
		isMissionBuildMode = false;
		isPerimeterBuildMode = false;

		loggerDisplayerSvc.logGeneral("Edit mode is off");
	}

	private void EditModeOn() {
		loggerDisplayerSvc.logGeneral("Edit mode is on");
	}

	private static Coordinate perimeterBreachPoint = null;
	private static MapMarkerDot perimeterBreachPointMarker = null;

	private void SetPerimeterBreachPoint() {
		if (perimeterBreachPointMarker == null) {
			perimeterBreachPoint = drone.getPerimeter().getClosestPointOnPerimeterBorder();
			perimeterBreachPointMarker = new MapMarkerDot(perimeterBreachPoint.getLat(),perimeterBreachPoint.getLon());
			addMapMarker(perimeterBreachPointMarker);
		}
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		Platform.runLater( () -> {
			switch (event) {
			case GPS:
				SetMyPosition(drone.getGps().getPosition());
				return;
			case HOME:
				SetHome(drone.getHome());
				return;
			case HEARTBEAT_TIMEOUT:
				loggerDisplayerSvc.logError("Heartbeat reached timeout");
				removeBearing();
			case DISCONNECTED:
				SetLastKnownPosition();
				return;
			case ORIENTATION:
				SetBearing(drone.getNavigation().getNavBearing());
				return;
			case LEFT_PERIMETER:
				SetPerimeterBreachPoint();
				return;
			case BEACON_BEEP:
				UpdateBeaconOnMap(drone.getBeacon().getPosition());
				return;
			case GCS_LOCATION:
				if (drone.getGCS().getPosition() == null) {
					loggerDisplayerSvc.logError("GCS location doesn't exist");
					return;
				}
				UpdateGCSOnMap(drone.getGCS().getPosition().dot(1));
				SetBearing(drone.getNavigation().getNavBearing());
				return;
			}
		});
	}

	private void UpdateGCSOnMap(Coordinate coord) {
		if (myGCS == null) {
			myGCS = new MapMarkerDot(coord.getLat(), coord.getLon());
			myGCS.setColor(Color.MAGENTA); 
		} 
		else if (myGCS.getCoordinate() == coord) {
			return;
		} 
		else {
			removeMapMarker(myGCS);
			myGCS = new MapMarkerDot(Color.MAGENTA, coord);
		}

		addMapMarker(myGCS);
		loggerDisplayerSvc.logGeneral("GCS was updated");
	}

	private void UpdateBeaconOnMap(Coordinate coord) {
		if (myBeacon == null) {
			myBeacon = new MapMarkerDot(coord.getLat(),coord.getLon());
			myBeacon.setColor(Color.MAGENTA); 
		}
		else if (myBeacon.getCoordinate() == coord) {
			return;
		}
		else {
			removeMapMarker(myBeacon);
			myBeacon = new MapMarkerDot(coord.getLat(),coord.getLon());
			myBeacon.setColor(Color.MAGENTA); 
		}

		addMapMarker(myBeacon);
		loggerDisplayerSvc.logGeneral("Beacon was updated");
	}
	
	@Override
	public HBox getMapButtomPane() {
		HBox hb = super.getMapButtomPane();
		
		cbLockMyPos = new CheckBox("Lock On My Position");
		cbLockMyPos.setSelected(true);
		cbLockMyPos.setOnAction(this);
		hb.getChildren().add(cbLockMyPos);
		
		cbFollowTrail = new CheckBox("Paint Trail");
		cbFollowTrail.setSelected(true);
		cbFollowTrail.setOnAction(this);
		hb.getChildren().add(cbFollowTrail);
		
		return hb;
	}

	private void SetLastKnownPosition() {
		if (myPos == null || myPos.getColor() == Color.RED)
			return;

		MapMarkerDot tmp = new MapMarkerDot( Color.RED, myPos.getCoordinate());
		removeMapMarker(myPos);
		myPos = tmp;
		addMapMarker(myPos);
	}

	private void SetMyPositionMarker(Coordinate coord) {
		Coordinate c = new Coordinate(coord.getLat(), coord.getLon());
		if (myPos != null && myPos.getCoordinate().getLat() == c.getLat()
				&& myPos.getCoordinate().getLon() == c.getLon()) {
			// System.out.println(getClass().getName() + " Same position");
			return;
		}

		MapMarkerDot tmp = new MapMarkerDot(Color.GREENYELLOW, c);

		if (myPos != null) {
			removeMapMarker(myPos);
		} else {
			// During the first time we have a GPS lock
			setDisplayPosition(c, 17);
		}
		addMapMarker(tmp);
		myPos = tmp;

		if (lockMapOnMyPosition)
			setDisplayPosition(myPos.getCoordinate(), getZoom());
	}

	private void SetMyPositionTrail(Coordinate coord) {
		if (!paintTrail)
			return;

		// Handling trail
		if (myTrailPath == null)
			myTrailPath = new MapLineImpl();

		int trailSize = myTrailPath.getCoordinates().size();
		if (trailSize >= 1 && myTrailPath.getCoordinates().get(trailSize - 1) == coord)
			return;

		myTrailPath.addCoordinate(coord);

		if (trailSize >= 2) {
			removeMapLine(myTrailPath);
			addMapLine(myTrailPath);
		}
	}

	private void SetMyPosition(Coordinate coord) {
		if (coord == null) {
			System.err.println("No Positions");
			return;
		}

		SetMyPositionMarker(coord);
		SetMyPositionTrail(coord);
	}
	
	@SuppressWarnings("incomplete-switch")
	@EventListener
	public void onApplicationEvent(QuadGuiEvent command) {
		try {
			switch (command.getCommand()) {
			case EDITMODE_EXISTING_LAYER_START:
				EditModeOn();
				Layer layer = (Layer) command.getSource();
				if (layer instanceof LayerMission) {
					System.out.println("Working on DroneMission Layer");
					modifyiedLayerMissionOriginal = (LayerMission) layer;
					super.startModifiedLayerMode(modifyiedLayerMissionOriginal);
					isMissionBuildMode = true;
					missionEditor = missionsManager.openMissionEditor(modifyiedLayerMissionOriginal.getMission());
					Mission mission = missionEditor.getModifiedMission();
					modifyiedLayerMissionOriginal.setName(mission.getName());
					eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_EDITING_STARTED, modifyiedLayerMissionOriginal));
				} else if (layer instanceof LayerPolygonPerimeter || layer instanceof LayerCircledPerimeter) {
					System.out.println("Working on Perimeter Layer");
					modifyiedLayerPerimeterOriginal = (LayerPerimeter) layer;
					super.startModifiedLayerMode(modifyiedLayerPerimeterOriginal);
					isPerimeterBuildMode = true;
					perimeterEditor = perimetersManager.openPerimeterEditor(modifyiedLayerPerimeterOriginal.getName(), modifyiedLayerPerimeterOriginal.getPerimeter().getClass());
					Perimeter perimeter = perimeterEditor.getModifiedPerimeter();
					modifyiedLayerPerimeterOriginal.setName(perimeter.getName());
				} else {
					System.out.println("Unrecognized Layer");
					EditModeOff();
					return;
				}

				break;
			case MISSION_UPDATED_BY_TABLE:
				LayerMission layerMission = (LayerMission) command.getSource();
				layerMission.regenerateMapObjects();
				break;
			}
		}
		catch (MissionUpdateException | PerimeterUpdateException e) {
			loggerDisplayerSvc.logError("Critical Error: failed to update item in database, error: " + e.getMessage());
		}
	}

	@Override
	public void LayerEditorCancel() {
		super.finishModifiedLayerMode();

		// Perimeters
		if (isPerimeterBuildMode) {
			Perimeter perimeter = perimetersManager.closePerimeterEditor(perimeterEditor, false);
			perimeterEditor = null;
			modifyiedLayerPerimeterOriginal.setPerimeter(perimeter);
			modifyiedLayerPerimeterOriginal.regenerateMapObjects();
			eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.LAYER_PERIMETER_EDITING_FINISHED, this.modifyiedLayerPerimeterOriginal));
			modifyiedLayerPerimeterOriginal = null;
		}

		// Missions
		if (isMissionBuildMode) {
			Mission mission = missionsManager.closeMissionEditor(missionEditor, false);
			missionEditor = null;
			modifyiedLayerMissionOriginal.setMission(mission);
			modifyiedLayerMissionOriginal.regenerateMapObjects();
			eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_EDITING_FINISHED, this.modifyiedLayerMissionOriginal));
			modifyiedLayerMissionOriginal = null;
		}
		
		isMissionBuildMode = false;
		isPerimeterBuildMode = false;
	}

	@Override
	public void LayerEditorSave() {
		ValidatorResponse validatorResponse = runtimeValidator.validate(getOperationalViewTree());
		if (validatorResponse.isFailed())
			throw new RuntimeException(validatorResponse.toString());

//		String missionName = null;
//		if (modifyiedLayerMissionOriginal != null) {
//			modifyiedLayerMissionOriginal.setName(modifyiedLayerMissionOriginal.getName().substring(0, modifyiedLayerMissionOriginal.getName().length() - 1));
//			missionName = modifyiedLayerMissionOriginal.getName();
//		}

		super.finishModifiedLayerMode();

		if (isPerimeterBuildMode) {
			Perimeter perimeter = perimetersManager.closePerimeterEditor(perimeterEditor, true);
			perimeterEditor = null;
			modifyiedLayerPerimeterOriginal.setPerimeter(perimeter);
			modifyiedLayerPerimeterOriginal.setName(perimeter.getName());
			eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.LAYER_PERIMETER_EDITING_FINISHED, this.modifyiedLayerPerimeterOriginal));
		}
		

		if (isMissionBuildMode) {
			Mission mission = missionsManager.closeMissionEditor(missionEditor, true);
			missionEditor = null;
			modifyiedLayerMissionOriginal.setMission(mission);
			modifyiedLayerMissionOriginal.setName(mission.getName());
			eventPublisherSvc.publish(new QuadGuiEvent(QuadGuiEvent.QUAD_GUI_COMMAND.MISSION_EDITING_FINISHED, this.modifyiedLayerMissionOriginal));
		}

		modifyiedLayerMissionOriginal = null;
		modifyiedLayerPerimeterOriginal = null;
		
		isMissionBuildMode = false;
		isPerimeterBuildMode = false;

		getOperationalViewTree().refresh();
	}

	@Override
	public void handle(ActionEvent event) {
		if (event.getSource().equals(cbLockMyPos)) {
			if (cbLockMyPos.isSelected()) {
				loggerDisplayerSvc.logGeneral("Lock on my position");
				lockMapOnMyPosition = true;
			} else {
				loggerDisplayerSvc.logGeneral("Release lock on my position");
				lockMapOnMyPosition = false;
			}
			return;
		}
		
		if (event.getSource().equals(cbFollowTrail)) {
			if (cbFollowTrail.isSelected()) {
				loggerDisplayerSvc.logGeneral("Paint My Trail");
				myTrailPath = null;
				paintTrail = true;
			} else {
				loggerDisplayerSvc.logGeneral("Stop Paint My Trail");
				paintTrail = false;
				removeMapLine(myTrailPath);
				myTrailPath = null;
			}
			return;
		}
	}
}