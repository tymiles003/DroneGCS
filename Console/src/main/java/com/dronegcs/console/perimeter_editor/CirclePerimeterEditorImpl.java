package com.dronegcs.console.perimeter_editor;

import com.dronedb.persistence.scheme.apis.DroneDbCrudSvcRemote;
import com.dronedb.persistence.scheme.apis.MissionCrudSvcRemote;
import com.dronedb.persistence.scheme.apis.QuerySvcRemote;
import com.dronedb.persistence.scheme.perimeter.CirclePerimeter;
import com.dronedb.persistence.scheme.perimeter.Perimeter;
import com.dronegcs.console.services.DialogManagerSvc;
import com.dronegcs.console.services.LoggerDisplayerSvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

/**
 * Created by oem on 3/26/17.
 */
@Scope(value = "prototype")
@Component
public class CirclePerimeterEditorImpl extends PerimeterEditorImpl<CirclePerimeter> implements ClosablePerimeterEditor<CirclePerimeter>, CirclePerimeterEditor {

    @Autowired
    @NotNull(message = "Internal Error: Failed to get log displayer")
    private LoggerDisplayerSvc loggerDisplayerSvc;

    @Autowired @NotNull(message = "Internal Error: Failed to get dialog manager")
    private DialogManagerSvc dialogManagerSvc;

    @Autowired @NotNull(message = "Internal Error: Failed to get drone object crud")
    private DroneDbCrudSvcRemote droneDbCrudSvcRemote;

    @Autowired @NotNull(message = "Internal Error: Failed to get query")
    private QuerySvcRemote querySvcRemote;

    @Autowired @NotNull(message = "Internal Error: Failed to get mission object crud")
    private MissionCrudSvcRemote missionCrudSvcRemote;

    @Override
    public CirclePerimeter open(CirclePerimeter perimeter) {
        return null;
    }

    @Override
    public CirclePerimeter open(String perimeter) {
        return null;
    }

    @Override
    public CirclePerimeter close(boolean shouldSave) {
        return null;
    }

    @Override
    public void setRadius(int radius) {

    }
}
