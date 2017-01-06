package gui.core.internalFrames;

import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import mavlink.is.drone.Drone;
import mavlink.is.drone.DroneInterfaces.DroneEventsType;
import mavlink.is.drone.DroneInterfaces.OnDroneListener;
import tools.csv.CSV;
import tools.csv.internal.CSVImpl;
import tools.os_utilities.Environment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import gui.is.events.GuiEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;

@Component("internalFrameHeightAndSpeed")
public class InternalFrameHeightAndSpeed extends Pane implements OnDroneListener, Initializable {

	@Autowired @NotNull( message="Internal Error: Failed to get drone" )
	private Drone drone;
	
	private CSV csv;
	
	@FXML private LineChart<String,Number> lineChart;

	/** The time series data. */
	private static XYChart.Series<String, Number> seriesHeight;
	private static XYChart.Series<String, Number> seriesAirSpeed;
	private static XYChart.Series<String, Number> seriesVerticalSpeed;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		loadChart();
	}

	private static int called;
	@PostConstruct
	private void init() throws URISyntaxException {
		if (called++ > 1)
			throw new RuntimeException("Not a Singletone");
		
		csv = new CSVImpl(Environment.getRunningEnvDirectory() + Environment.DIR_SEPERATOR + "HeightAndSpeed.csv");
		csv.open(Arrays.asList("Time", "Height", "VerticalSpeed", "AirSpeed"));
		
		drone.addDroneListener(this);
	}

	private void addValues(double altitude, double airSpeed, double verticalSpeed) {
		Platform.runLater( () -> {
			String timestamp = LocalDateTime.now().toLocalTime().toString();
			
			if (seriesHeight != null)
				seriesHeight.getData().add(new XYChart.Data<String, Number>(timestamp, altitude));
			if (seriesVerticalSpeed != null)
				seriesVerticalSpeed.getData().add(new XYChart.Data<String, Number>(timestamp, verticalSpeed));
			if (seriesAirSpeed != null)
				seriesAirSpeed.getData().add(new XYChart.Data<String, Number>(timestamp, airSpeed));
			
			csv.addEntry(Arrays.asList(timestamp, altitude, airSpeed, verticalSpeed));
		});
	}

	private void loadChart() {
        seriesHeight = new XYChart.Series<String, Number>();
        seriesHeight.setName("Height (m)");
		lineChart.getData().add(seriesHeight);
		
		seriesAirSpeed = new XYChart.Series<String, Number>();
		seriesAirSpeed.setName("Air Speed (m/s)");
		lineChart.getData().add(seriesAirSpeed);
		
		seriesVerticalSpeed = new XYChart.Series<String, Number>();
		seriesVerticalSpeed.setName("Vertical Speed (m/s)");
		lineChart.getData().add(seriesVerticalSpeed);
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		switch (event) {
			case SPEED:
				addValues(drone.getAltitude().getAltitude(), drone.getSpeed().getAirSpeed().valueInMetersPerSecond(), drone.getSpeed().getVerticalSpeed().valueInMetersPerSecond());
				break;
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	@EventListener
	public void onApplicationEvent(GuiEvent command) {
		switch (command.getCommand()) {
		case EXIT:
			if (csv != null) 
				csv.close();
			break;
		}
	}
}
