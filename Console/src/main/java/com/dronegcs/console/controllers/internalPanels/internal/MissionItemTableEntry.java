package com.dronegcs.console.controllers.internalPanels.internal;

import com.dronedb.persistence.scheme.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class MissionItemTableEntry {
	
    private final SimpleIntegerProperty order;
    private final SimpleStringProperty type;
    private final SimpleDoubleProperty lat, lon, height, delay, radius;
    private final SimpleObjectProperty<MissionItem> missionItem;
 
    public MissionItemTableEntry(	Integer pOrder, 
    				String missionItemType,
    				Double pLat, Double pLon, Double pHeight, Double pDelay, Double pRadius,
    				MissionItem pDroneMissionItem) {
    	
        this.order = new SimpleIntegerProperty(pOrder);
        this.type = new SimpleStringProperty(missionItemType);
        this.lat = new SimpleDoubleProperty(pLat);
        this.lon = new SimpleDoubleProperty(pLon);
        this.height = new SimpleDoubleProperty(pHeight);
        this.delay = new SimpleDoubleProperty(pDelay);
        this.radius = new SimpleDoubleProperty(pRadius);
        this.missionItem = new SimpleObjectProperty<MissionItem>(pDroneMissionItem);
    }


	public Integer getOrder() {return this.order.get();}
    public void setOrder(Integer pOrder) {this.order.set(pOrder);}
        
    public String getType() {return this.type.get();}
    public void setType(String pType) {this.type.set(pType);}
    
    public Double getLat() {return this.lat.get();}
    public void setLat(Double pLat) {this.lat.set(pLat);}
    
    public Double getLon() {return this.lon.get();}
    public void setLon(Double pLon) {this.lon.set(pLon);}
    
    public Double getHeight() {return this.height.get();}
    public void setHeight(Double pHeight) {this.height.set(pHeight);}
    
    public Double getDelay() {return this.delay.get();}
    public void setDelay(Double pDelay) {this.delay.set(pDelay);}
    
    public Double getRadius() {return this.radius.get();}
    public void setRadius(Double pRadius) {this.radius.set(pRadius);}  
    
    public MissionItem getMissionItem() {return this.missionItem.get();}
    public void setMissionItem(MissionItem droneMissionItem) {this.missionItem.set(droneMissionItem);}
}