package gui.core.internalPanels;

import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import gui.core.internalPanels.internal.EditingCell;
import gui.core.internalPanels.internal.MissionItemTableEntry;
import gui.core.mapTreeObjects.LayerMission;
import gui.is.events.GuiEvent;
import gui.is.events.GuiEvent.COMMAND;
import gui.is.services.EventPublisherSvc;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;
import mavlink.is.drone.mission.Mission;
import mavlink.is.drone.mission.MissionItem;
import mavlink.is.drone.mission.MissionItemType;
import mavlink.is.drone.mission.waypoints.Circle;
import mavlink.is.drone.mission.waypoints.RegionOfInterest;
import mavlink.is.drone.mission.waypoints.Waypoint;
import mavlink.is.drone.mission.waypoints.interfaces.Altitudable;
import mavlink.is.drone.mission.waypoints.interfaces.Delayable;
import mavlink.is.drone.mission.waypoints.interfaces.Radiusable;
import mavlink.is.protocol.msg_metadata.ardupilotmega.msg_mission_item;
import mavlink.is.utils.units.Altitude;

@Component("areaMission")
public class PanelMissionBox extends Pane implements Initializable {
	
	@Autowired @NotNull( message = "Internal Error: Fail to get event publisher" )
	protected EventPublisherSvc eventPublisherSvc;
	
	@FXML private TableView<MissionItemTableEntry> table;
    
	@FXML private TableColumn<MissionItemTableEntry,Integer> order;
	@FXML private TableColumn<MissionItemTableEntry,String> type;
	@FXML private TableColumn<MissionItemTableEntry,Double> lat;
	@FXML private TableColumn<MissionItemTableEntry,Double> lon;
	@FXML private TableColumn<MissionItemTableEntry,Double> height;
	@FXML private TableColumn<MissionItemTableEntry,Double> delay;
	@FXML private TableColumn<MissionItemTableEntry,Double> radius;
	@FXML private TableColumn<MissionItemTableEntry,String> setUp;
	@FXML private TableColumn<MissionItemTableEntry,String> setDown;
	@FXML private TableColumn<MissionItemTableEntry,String> remove;
	
	private LayerMission layerMission;
	
	static int called;
	@PostConstruct
	public void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singletone");
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {		
		setPrefHeight(Screen.getPrimary().getBounds().getHeight()*0.25);
		
	    table.prefHeightProperty().bind(prefHeightProperty());
	    
	    Callback<TableColumn<MissionItemTableEntry, Double>, TableCell<MissionItemTableEntry, Double>> cellFactory = new Callback<TableColumn<MissionItemTableEntry, Double>, TableCell<MissionItemTableEntry, Double>>() {
             public TableCell<MissionItemTableEntry, Double> call(TableColumn<MissionItemTableEntry, Double> p) {
                return new EditingCell<Double>(new DoubleStringConverter());
             }
         };
        
        order.setCellValueFactory(new PropertyValueFactory<MissionItemTableEntry,Integer>("order"));
        type.setCellValueFactory(new PropertyValueFactory<MissionItemTableEntry,String>("type"));
        lat.setCellValueFactory(new PropertyValueFactory<MissionItemTableEntry,Double>("lat"));
        lon.setCellValueFactory(new PropertyValueFactory<MissionItemTableEntry,Double>("lon"));
        
        height.setCellValueFactory(new PropertyValueFactory<MissionItemTableEntry,Double>("height"));
        height.setCellFactory(cellFactory);
        height.setOnEditCommit( t -> {
        	MissionItemTableEntry entry = (MissionItemTableEntry) t.getTableView().getItems().get(t.getTablePosition().getRow());
        	if (entry.getMissionItem() instanceof Altitudable) {
        		Altitudable wp = (Altitudable) entry.getMissionItem();
        		wp.setAltitude(new Altitude(t.getNewValue()));
        	}
        	generateMissionTable(true);
        	eventPublisherSvc.publish(new GuiEvent(COMMAND.MISSION_UPDATED_BY_TABLE, layerMission));
        });
        
        delay.setCellValueFactory(new PropertyValueFactory<MissionItemTableEntry,Double>("delay"));
        delay.setCellFactory(cellFactory);
        delay.setOnEditCommit( t -> {
        	MissionItemTableEntry entry = (MissionItemTableEntry) t.getTableView().getItems().get(t.getTablePosition().getRow());
        	if (entry.getMissionItem() instanceof Delayable) {
        		Delayable wp = (Delayable) entry.getMissionItem();
        		wp.setDelay(t.getNewValue());
        	}
        	generateMissionTable(true);
        	eventPublisherSvc.publish(new GuiEvent(COMMAND.MISSION_UPDATED_BY_TABLE, layerMission));
        });
        
        radius.setCellValueFactory(new PropertyValueFactory<MissionItemTableEntry,Double>("radius"));
        radius.setCellFactory(cellFactory);
        radius.setOnEditCommit( t -> {
        	MissionItemTableEntry entry = (MissionItemTableEntry) t.getTableView().getItems().get(t.getTablePosition().getRow());
        	if (entry.getMissionItem() instanceof Radiusable) {
        		Radiusable wp = (Radiusable) entry.getMissionItem();
        		wp.setRadius(t.getNewValue());
        	}
        	generateMissionTable(true);
        	eventPublisherSvc.publish(new GuiEvent(COMMAND.MISSION_UPDATED_BY_TABLE, layerMission));
        });
        
        setUp.setCellFactory( param -> {
            final TableCell<MissionItemTableEntry, String> cell = new TableCell<MissionItemTableEntry, String>() {
                final Button btn = new Button("Up");
                @Override
                public void updateItem( String item, boolean empty ) {
                    super.updateItem( item, empty );
                    setGraphic( null );
                    setText( null );
                    if ( !empty && getIndex() > 0 ) {
                        btn.setOnAction( ( ActionEvent event ) -> {
                        	MissionItemTableEntry entry = getTableView().getItems().get( getIndex() );
                        	Mission mission = entry.getMissionItem().getMission();
                            mission.getItems().remove(getIndex());
                            mission.getItems().add(getIndex() - 1, entry.getMissionItem());
                            generateMissionTable(true);
                            eventPublisherSvc.publish(new GuiEvent(COMMAND.MISSION_UPDATED_BY_TABLE, layerMission));
                        });
                        setGraphic( btn );
                    }
                }
            };
            return cell;
        });
        
        setDown.setCellFactory( param -> {
            final TableCell<MissionItemTableEntry, String> cell = new TableCell<MissionItemTableEntry, String>() {
                final Button btn = new Button("Down");
                @Override
                public void updateItem( String item, boolean empty ) {
                    super.updateItem( item, empty );
                    setGraphic( null );
                    setText( null );
                    if ( !empty && getIndex() < getTableView().getItems().size() - 1 ) {
                        btn.setOnAction( ( ActionEvent event ) -> {
                        	MissionItemTableEntry entry = getTableView().getItems().get( getIndex() );
                        	Mission mission = entry.getMissionItem().getMission();
                            mission.getItems().remove(getIndex());
                            mission.getItems().add(getIndex() + 1, entry.getMissionItem());
                            generateMissionTable(true);
                            eventPublisherSvc.publish(new GuiEvent(COMMAND.MISSION_UPDATED_BY_TABLE, layerMission));
                        });
                        setGraphic( btn );
                    }
                }
            };
            return cell;
        });
        
        remove.setCellFactory( param -> {
            final TableCell<MissionItemTableEntry, String> cell = new TableCell<MissionItemTableEntry, String>() {
                final Button btn = new Button("X");
                @Override
                public void updateItem( String item, boolean empty ) {
                    super.updateItem( item, empty );
                    setGraphic( null );
                    setText( null );
                    if ( !empty ) {
                        btn.setOnAction( ( ActionEvent event ) -> {
                        	MissionItemTableEntry entry = getTableView().getItems().get( getIndex() );
                        	Mission mission = entry.getMissionItem().getMission();
                            mission.getItems().remove(getIndex());
                            generateMissionTable(true);
                            eventPublisherSvc.publish(new GuiEvent(COMMAND.MISSION_UPDATED_BY_TABLE, layerMission));
                        });
                        setGraphic( btn );
                    }
                }
            };
            return cell;
        });
	}

	@SuppressWarnings("unchecked")
	public void generateMissionTable(boolean editmode) {
		MissionItemTableEntry entry = null;
		
		if (table == null) {
			throw new RuntimeException("Failed to get Table");
		}
		
		if (layerMission == null) {
			table.setItems(null);
			return;
		}
		
		Mission mission = layerMission.getMission();
		if (mission == null) {
			table.setItems(null);
			return;
		}
		
		// Setting columns
		table.getColumns().removeAll(table.getColumns());
		if (!editmode)
			table.getColumns().addAll(order, type, lat, lon, height, delay, radius);
		else 
			table.getColumns().addAll(order, type, lat, lon, height, delay, radius, setUp, setDown, remove);
		
		height.setEditable(editmode);
		delay.setEditable(editmode);
		radius.setEditable(editmode);
		
		// Start loading items
		ObservableList<MissionItemTableEntry> data = FXCollections.observableArrayList();

		Iterator<MissionItem> it = mission.getItems().iterator();
		int i = 0;
		while (it.hasNext()) {
			System.out.println("Loop ");
			MissionItem mItem = it.next();
			
			switch (mItem.getType()) {
				case WAYPOINT: {
					Waypoint wp = (Waypoint) mItem;
					entry = new MissionItemTableEntry(i, MissionItemType.WAYPOINT, wp.getCoordinate().getLat(), wp.getCoordinate().getLng(), wp.getCoordinate().getAltitude().valueInMeters(), wp.getDelay(), 0.0, mItem);
					break;
				}
				case SPLINE_WAYPOINT:
					//return new SplineWaypoint(referenceItem);
					break;
				case TAKEOFF: {
					msg_mission_item msg = mItem.packMissionItem().get(0);
					double alt = (double) msg.z;
					entry = new MissionItemTableEntry(i, MissionItemType.TAKEOFF, 0.0, 0.0, alt, 0.0, 0.0,  mItem); 
					break;	
				}
				case CHANGE_SPEED:
					//return new ChangeSpeed(referenceItem);
				case CAMERA_TRIGGER:
					//return new CameraTrigger(referenceItem);
				case EPM_GRIPPER:
					//return new EpmGripper(referenceItem);
				case RTL: {
					entry = new MissionItemTableEntry(i, MissionItemType.RTL, 0.0, 0.0, 0.0, 0.0, 0.0, mItem); 
					break;
				}
				case LAND: {
					entry = new MissionItemTableEntry(i, MissionItemType.LAND, 0.0, 0.0, 0.0, 0.0, 0.0, mItem);
					break;
				}
				case CIRCLE: { // Loiter
					Circle wp = (Circle) mItem;
					entry = new MissionItemTableEntry(i, MissionItemType.CIRCLE, wp.getCoordinate().getLat(), wp.getCoordinate().getLng(), wp.getCoordinate().getAltitude().valueInMeters(), 0.0, wp.getRadius(), mItem);
					break;
				}
				case ROI:
					RegionOfInterest roi = (RegionOfInterest) mItem;
					entry = new MissionItemTableEntry(i, MissionItemType.ROI, roi.getCoordinate().getLat(), roi.getCoordinate().getLng(), roi.getCoordinate().getAltitude().valueInMeters(), 0.0, 0.0, mItem);
					break;
				case SURVEY:
					//return new Survey(referenceItem.getMission(), Collections.<Coord2D> emptyList());
				case CYLINDRICAL_SURVEY:
					//return new StructureScanner(referenceItem);
				default:
					break;
			}
			
			i++;
			data.add(entry);
		}
		
		
		table.setItems(data);
		table.setEditable(editmode);
	}
	
	private void setLayerMission(LayerMission layerMission) {
		this.layerMission = layerMission;
	}
	
	private void unsetLayerMission() {
		this.layerMission = null;
	}
	
	@SuppressWarnings("incomplete-switch")
	@EventListener
	public void onApplicationEvent(GuiEvent command) {
		Platform.runLater( () -> {
			LayerMission layerMission;
			switch (command.getCommand()) {
				case MISSION_EDITING_STARTED:
				case MISSION_UPDATED_BY_MAP:
					layerMission = (LayerMission) command.getSource();
					setLayerMission(layerMission);
					generateMissionTable(true);
					break;
				case MISSION_EDITING_FINISHED:
					unsetLayerMission();
					generateMissionTable(false);
					break;
				case MISSION_VIEW_ONLY_STARTED:
					layerMission = (LayerMission) command.getSource();
					setLayerMission(layerMission);
					generateMissionTable(false);
					break;
				case MISSION_VIEW_ONLY_FINISHED:
					setLayerMission(null);
					generateMissionTable(false);
					break;
			}
		});
	}

}
