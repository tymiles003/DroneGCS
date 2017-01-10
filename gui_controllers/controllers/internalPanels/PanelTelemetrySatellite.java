package controllers.internalPanels;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import mavlink.drone.Drone;
import mavlink.drone.DroneInterfaces.DroneEventsType;
import mavlink.drone.DroneInterfaces.OnDroneListener;
import validations.RuntimeValidator;

import java.net.URL;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import gui.services.LoggerDisplayerSvc;
import gui.services.TextNotificationPublisherSvc;

@Component("telemetrySatellite")
public class PanelTelemetrySatellite extends VBox implements OnDroneListener, Initializable {
	
	@FXML private Label lblStatus;
	@FXML private Label lblFlightMode;
	@FXML private Label lblSignal;
	@FXML private Label lblBattery;
	@FXML private ProgressBar batteryBar;
	@FXML private Label lblHeight;
	@FXML private Label lblFlightTime;
	@FXML private Label lblFlightDistance;

	@FXML private Label lblEngine1;
	@FXML private Label lblEngine2;
	@FXML private Label lblEngine3;
	@FXML private Label lblEngine4;

	@FXML private Label lblThrust;
	@FXML private Label lblYaw;
	@FXML private Label lblPitch;
	@FXML private Label lblRoll;
	
	@FXML private TextField lblCriticalMsg;
	
	@Autowired @NotNull(message = "Internal Error: Failed to get text publisher")
	private TextNotificationPublisherSvc textNotificationPublisherSvc;
	
	@Autowired @NotNull(message = "Internal Error: Failed to get logger displayer")
	private LoggerDisplayerSvc loggerDisplayerSvc;
	
	@Autowired @NotNull(message = "Internal Error: Failed to get drone")
	public Drone drone;
	
	@Autowired
	private RuntimeValidator runtimeValidator;
	
	private static int called;
	@PostConstruct
	private void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singletone");	
		
		if (!runtimeValidator.validate(this))
			throw new RuntimeException("Validation failed");
		else
			System.err.println("Validation Succeeded for instance of " + getClass());
		
		drone.addDroneListener(this);
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		lblCriticalMsg.setEditable(false);
		batteryBar.setProgress(0);
		SetHeartBeat(true);
	}
	
	protected void SetFlightModeLabel(String name) {
		lblFlightMode.setText(name);
	}
	
	public void SetLblHeight(double ht) {
		lblHeight.setText(String.format("%.1f", ht) + "m");
	}
	
	public void SetSignal(int signalStrength) {
		lblSignal.setText(signalStrength + "%");
	}
	
	private void setFlightTime(long flightTime) {
		lblFlightTime.setText(flightTime + " sec (" + flightTime/60 + " min)");
	}

	private void setDistanceTraveled(double distanceTraveled) {
		lblFlightDistance.setText(String.format("%.1f", distanceTraveled) + "m");
	}
	
	public void SetBattery(double bat) {
		lblBattery.setText((bat < 0 ? 0 : bat) + "%");
		
		if (drone.getState().isArmed()) {
			if (bat == 50 || bat == 49) {
				textNotificationPublisherSvc.publish("Battery at 50%");
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			}
			
			if (bat == 25 || bat == 24) {
				textNotificationPublisherSvc.publish("Battery at 25%");
				java.awt.Toolkit.getDefaultToolkit().beep();
				lblBattery.setStyle("-fx-background-color: #red");
				return;
			}
			
			if (bat == 10 || bat == 9) {
				textNotificationPublisherSvc.publish("Critical: battery below 10%");
				lblBattery.setStyle("-fx-background-color: #red");
				java.awt.Toolkit.getDefaultToolkit().beep();
				java.awt.Toolkit.getDefaultToolkit().beep();
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			}
			
			if (bat < 10) {
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			}
		}
	}
	
	public void setRCActual(int e1, int e2, int e3, int e4) {
		lblEngine1.setText(e1 + "");
		lblEngine2.setText(e2 + "");    
		lblEngine3.setText(e3 + "");
		lblEngine4.setText(e4 + "");
	}
	
	public void setRCSend(int roll, int pitch, int thrust, int yaw) {
		lblRoll.setText("Roll: " + roll);
		lblPitch.setText("Pitch: " + pitch);
		lblThrust.setText("Thrust: " + thrust);
		lblYaw.setText("Yaw: " + yaw);
	}
	
	public void SetHeartBeat(boolean on) {
		if (on) {
			lblStatus.setText("Connected");
			lblStatus.setTextFill(Color.web("#008000"));
			return;
		}
		
		lblStatus.setText("Disconnected");
		lblStatus.setTextFill(Color.web("#FF0000"));
	}
	
	public void ClearNotification() {
		lblCriticalMsg.setVisible(false);
	}

	public void SetNotification(String notification) {
		lblCriticalMsg.setVisible(true);
		if (lblCriticalMsg.getStyle().equals("-fx-control-inner-background: orange;"))
			lblCriticalMsg.setStyle("-fx-control-inner-background: blue;");
		else
			lblCriticalMsg.setStyle("-fx-control-inner-background: orange;");
		
		lblCriticalMsg.setText(notification);
	}
	
	@EventListener
	public void onApplicationEvent(String notification) {
		SetNotification(notification);
	}

	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		Platform.runLater( () -> {
			switch (event) {
				case ORIENTATION:
					SetLblHeight(drone.getAltitude().getAltitude());
					return;
				case HEARTBEAT_FIRST:
					loggerDisplayerSvc.logGeneral("Quad Connected");
					SetHeartBeat(true);
					return;
				case HEARTBEAT_RESTORED:
					loggerDisplayerSvc.logGeneral("Quad Connection Restored");
					SetHeartBeat(true);
					return;
				case DISCONNECTED:
				case HEARTBEAT_TIMEOUT:
					loggerDisplayerSvc.logError("Quad Disconnected");
					SetLblHeight(0);
					SetSignal(0);
					SetBattery(0);
					SetFlightModeLabel("Unknown");
					SetHeartBeat(false);
					return;
				case RADIO:
					SetSignal(drone.getRadio().getSignalStrength());
					return;
				case RC_OUT:
					setRCActual(drone.getRC().out[0], drone.getRC().out[1], drone.getRC().out[2], drone.getRC().out[3]);
					return;
				case RC_IN:
					setRCSend(drone.getRC().in[0], drone.getRC().in[1], drone.getRC().in[2], drone.getRC().in[3]);
					return;
				case GPS:
					setDistanceTraveled(drone.getGps().getDistanceTraveled());
					return;
				case STATE:
					setFlightTime(drone.getState().getFlightTime());
					return;
				case BATTERY:
					SetBattery(drone.getBattery().getBattRemain());
					Platform.runLater( () -> batteryBar.setProgress(drone.getBattery().getBattRemain() / 100.0));
					return;
				case MODE:
					SetFlightModeLabel(drone.getState().getMode().getName());
					return;
			}
		});
	}
}
