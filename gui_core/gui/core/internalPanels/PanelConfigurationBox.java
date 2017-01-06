package gui.core.internalPanels;

import java.net.URL;
import java.util.ResourceBundle;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gui.is.events.GuiEvent;
import gui.is.events.GuiEvent.COMMAND;
import gui.is.services.EventPublisherSvc;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import mavlink.is.drone.Drone;
import tools.validations.RuntimeValidator;

@Component("areaConfiguration")
public class PanelConfigurationBox extends Pane implements Initializable {
	
	@Autowired @NotNull(message = "Internal Error: Failed to get GUI event publisher")
	protected EventPublisherSvc eventPublisherSvc;
	
	@Autowired @NotNull
	private RuntimeValidator runtimeValidator;
	
	@Autowired @NotNull
	private Drone drone;
	
	@FXML private CheckBox cbActiveGeofencePerimeterAlertOnly;
	@FXML private ComboBox<Integer> cmbframeContainerCells;
	@FXML private Button btnUpdateDevice;
	@FXML private TextField txtDeviceId;
	
	private static int called = 0;
	@PostConstruct
	private void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singletone");
		
		if (!runtimeValidator.validate(this))
			throw new RuntimeException("Validation failed");
		else
			System.err.println("Validation Succeeded for instance of " + getClass());
	}
	
	@Override 
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        cbActiveGeofencePerimeterAlertOnly.setOnAction( e -> drone.getPerimeter().setAlertOnly(cbActiveGeofencePerimeterAlertOnly.isSelected() ? true : false));
        cmbframeContainerCells.setOnAction( e -> eventPublisherSvc.publish(new GuiEvent(COMMAND.SPLIT_FRAMECONTAINER, cmbframeContainerCells.getValue())));
        btnUpdateDevice.setOnAction( e -> eventPublisherSvc.publish(new GuiEvent(COMMAND.CAMERA_DEVICEID,  Integer.parseInt(txtDeviceId.getText())  )));
	}
}
