package gui.core.dashboard;

import gui.core.internalFrames.*;
import gui.core.internalPanels.*;
import gui.core.operations.internal.ArmQuad;
import gui.core.operations.internal.TakeoffQuad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Timer;

import flight_controlers.KeyBoardControl;
import gui.is.LoggerDisplayerHandler;
import gui.is.NotificationsHandler;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JDesktopPane;
import logger.Logger;
import mavlink.core.connection.RadioConnection;
import mavlink.core.connection.helper.GCSLocationData;
import mavlink.core.drone.MyDroneImpl;
import mavlink.core.gcs.GCSHeartbeat;
import mavlink.is.drone.Drone;
import mavlink.is.drone.DroneInterfaces.*;
import mavlink.is.protocol.msg_metadata.ApmModes;
import mavlink.is.protocol.msgbuilder.*;
import desktop.logic.*;
import desktop.logic.Clock;
import desktop.logic.Handler;

public class Dashboard implements OnDroneListener, NotificationsHandler, LoggerDisplayerHandler {

	private JFrame frame;
	
	private static RadioConnection radConn;
	public static Drone drone;

    @SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	private static final int LOG_BOX_MAX_LINES = 50;//7;

        
    private static JTextPane logBox;
    public JToggleButton areaLogLockTop;
    
    private JLabel keepAliveLabel;
    //private JLabel lblCriticalMsg;
    
    private JPanelTelemetrySatellite telemetryPanel;

    private JButton btnExit;
    private JButton btnSyncDrone;
    private JButton btnFly;
    private JToggleButton btnArm;
    private JButton btnLandRTL;
    private JButton btnTakeoff;
    private boolean takeOffThreadRunning = false;
    private SwingWorker<Void, Void> takeOffThread = null;
    public JToggleButton btnFollowBeaconStart;
    private SwingWorker<Void, Void> FollowBeaconStartThread = null;
    private JButton btnFollowBeaconShow;
    private JButton btnGCSShow;
    private JButton btnStopFollow;
    private JButton btnHoldPosition;
    private JButton btnStartMission;
    
    private JToolBar tbTelemtry;
	private JToolBar tbContorlButton;
	private JTabbedPane tbSouth;
	private JPanel pnlConfiguration;
	private JToolBar toolBar;
    
	private JProgressBar progressBar;
    
    private boolean motorArmed = false;
    public JScrollPane areaMission = null;
    
    public JCheckBox cbActiveGeofencePerimeterEnforce = null;
    public JCheckBox cbActiveGeofencePerimeterAlert = null;

	private JPanelToolBar pnlToolbar_North;

	public static LoggerDisplayerManager loggerDisplayerManager = null;
	
	public static Dashboard window = null;
	
	
    public static JDesktopPane desktopPane;
    
    public static NotificationManager notificationManager = null;
	
    
		
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {					
					System.out.println("Start Dashboard");
					window = new Dashboard();
					window.initialize();
					
					System.out.println("Start Logger Displayer Manager");
					loggerDisplayerManager = new LoggerDisplayerManager(window, LOG_BOX_MAX_LINES);
					
					System.out.println("Start Outgoing Communication");
					radConn = new RadioConnection();
					radConn.connect();
					
					Handler handler = new desktop.logic.Handler();
					drone = new MyDroneImpl(radConn, new Clock(), handler,FakeFactory.fakePreferences());
					
					
					System.out.println("Start Notifications Manager");
					Timer timer = new Timer();
					notificationManager = new NotificationManager(window);
					timer.scheduleAtFixedRate(notificationManager, 0, NotificationManager.MSG_CHECK_PERIOD);
					
					
					System.out.println("Start GCS Heartbeat");
					GCSHeartbeat gcs = new GCSHeartbeat(drone, 1);
					gcs.setActive(true);
					
					System.out.println("Start Keyboard Stabilizer");
					KeyBoardControl.get();
					
					window.refresh();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	protected void refresh() {
		System.out.println("Sign Dashboard as drone listener");
		drone.addDroneListener(window);
		drone.addDroneListener(telemetryPanel);
		
		JInternalFrameMap.Generate();
		if (drone.isConnectionAlive()) {
			SetHeartBeat(true);
			//SetFlightModeLabel(drone.getState().getMode().getName());
			drone.notifyDroneEvent(DroneEventsType.MODE);
		}
	}

	public void SetDistanceToWaypoint(double d) {
		if (drone.getState().getMode().equals(ApmModes.ROTOR_GUIDED)) {
			//if (drone.getGuidedPoint().isIdle()) {
			if (d == 0) {
				notificationManager.add("In Position");
				loggerDisplayerManager.addGeneralMessegeToDisplay("Guided: In Position");
			}
			else {
				notificationManager.add("Flying to destination");
				loggerDisplayerManager.addGeneralMessegeToDisplay("Guided: Fly to distination");
			}
		}
	}

	/**
	 * Create the application.
	 */
	public Dashboard() {
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame("Quad Ground Station");
		frame.setBounds(100, 100, 450, 300);
		
        frame.setSize(400, 400);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        JPanel eastPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        desktopPane = new JDesktopPane();
        frame.getContentPane().add(desktopPane, BorderLayout.CENTER);               
        
        pnlToolbar_North = new JPanelToolBar();
        frame.getContentPane().add(pnlToolbar_North, BorderLayout.NORTH);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        frame.getContentPane().add(southPanel, BorderLayout.SOUTH);
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        southPanel.add(progressBar, BorderLayout.SOUTH);
        
        toolBar = new JToolBar();
        southPanel.add(toolBar, BorderLayout.CENTER);
        
        tbSouth = new JTabbedPane(JTabbedPane.TOP);
        toolBar.add(tbSouth);
        
        JPanel pnlLogBox = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        logBox = new JTextPane();
        logBox.setLayout(new GridLayout(1,1,0,0));
        JPanel pnlLogbox = new JPanel(new GridLayout(1,1));
        JScrollPane logbox = new JScrollPane(logBox, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        Dimension southPanelDimension = new Dimension(1200, 150);
        
        logbox.setPreferredSize(southPanelDimension);
        logBox.setEditorKit(new HTMLEditorKit());
        logBox.setEditable(false);
        pnlLogbox.add(logbox);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1.0;	// request any extra vertical space
        pnlLogBox.add(pnlLogbox, c);
        
        JPanel pnlLogToolBox = new JPanel(new GridLayout(0,1,0,0));     
        areaLogLockTop = new JToggleButton("Top");        
        areaLogLockTop.addActionListener(e -> {if (areaLogLockTop.isSelected()) logbox.getVerticalScrollBar().setValue(0);});
        
        pnlLogToolBox.add(areaLogLockTop);
        
        JButton areaLogClear = new JButton("CLR");
        areaLogClear.addActionListener(e -> logBox.setText(""));
        pnlLogToolBox.add(areaLogClear);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1.0;   //request any extra vertical space
        pnlLogBox.add(pnlLogToolBox, c);
        
        tbSouth.addTab("Log Book", null, pnlLogBox, null);
        
        pnlConfiguration = new JPanel();
        JScrollPane areaConfiguration = new JScrollPane(pnlConfiguration, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        areaConfiguration.setPreferredSize(southPanelDimension);
        
        cbActiveGeofencePerimeterAlert = new JCheckBox("Active GeoFence/Perimeter Alert");
        cbActiveGeofencePerimeterAlert.addActionListener( e -> drone.getPerimeter().setAlert(cbActiveGeofencePerimeterAlert.isSelected() ? true : false));
        cbActiveGeofencePerimeterAlert.setSelected(false);
        pnlConfiguration.add(cbActiveGeofencePerimeterAlert);
        
        cbActiveGeofencePerimeterEnforce = new JCheckBox("Active GeoFence/Perimeter Enforcement");
        cbActiveGeofencePerimeterEnforce.addActionListener( e -> drone.getPerimeter().setEnforce(cbActiveGeofencePerimeterEnforce.isSelected() ? true : false));
        cbActiveGeofencePerimeterEnforce.setSelected(false);
        pnlConfiguration.add(cbActiveGeofencePerimeterEnforce);
        
        tbSouth.addTab("Configuration", null, areaConfiguration, null);
        
        areaMission = new JScrollPane(null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        areaMission.setPreferredSize(southPanelDimension);
        tbSouth.addTab("Mission", null, areaMission, null);
        
        frame.getContentPane().add(eastPanel, BorderLayout.EAST);
        //for (int i =0 ; i < LOG_BOX_MAX_LINES ; i++) addMessegeToDisplay("-", Type.GENERAL, true);
       
        ////////////////////////////////////////////////////
        ////////////////////////////////////////////////////
        /////////////////  Control Panel  //////////////////
        
        /*JComboBox<String> algo_combo = new JComboBox<>(new String[] {
        		KeyBoardControl.ModesTitles.get(Modes.ARDUCOPTER), KeyBoardControl.ModesTitles.get(Modes.WIFI), "NONE"});
        algo_combo.addItemListener(new ItemListener() {
        	@Override
            public void itemStateChanged(ItemEvent e) {
        		if (e.getStateChange() == e.SELECTED) {
        			JComboBox cb = (JComboBox) e.getSource();
        			String text_sel = (String)cb.getSelectedItem();
        			KeyBoardControl.get().SetModeByString(text_sel);
        			System.err.println("SELECTED " + text_sel);
        		}
            }
        });*/
        
        tbContorlButton = new JToolBar();
        eastPanel.add(tbContorlButton);
        
        JPanel eastPanel_buttons = new JPanel(new GridLayout(0, 2, 1, 1));
        tbContorlButton.add(eastPanel_buttons);
        
        btnExit = new JButton("Exit");
        btnExit.addActionListener( e -> {
        	if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Are you sure you wand to exit?", "", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
        		System.out.println("Bye Bye");
        		Logger.LogGeneralMessege("");
        		Logger.LogGeneralMessege("Summary:");
        		Logger.LogGeneralMessege("--------");
        		Logger.LogGeneralMessege("Traveled distance: " + drone.getGps().getDistanceTraveled() + "m");
        		Logger.LogGeneralMessege("Flight time: " + drone.getState().getFlightTime() + "");
				Logger.close();
				System.exit(0);
        	}
		});
        eastPanel_buttons.add(btnExit);
        
        btnSyncDrone = new JButton("Sync Drone");
        btnSyncDrone.addActionListener( e -> {
        	loggerDisplayerManager.addGeneralMessegeToDisplay("Syncing Drone parameters");
    		resetProgressBar();
    		drone.getParameters().refreshParameters();
        });
        eastPanel_buttons.add(btnSyncDrone);
        
        btnFly = new JButton("Controler: RC");
        //btnFly.addKeyListener(KeyBoardControl.get());
        btnFly.addActionListener( e -> {
        	KeyBoardControl.get().HoldIfNeeded();
        	Object[] options = {"KeyBoard", "RC Controller"};
        	int n = JOptionPane.showOptionDialog(null, "Choose Controler", "",
        		    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
        		    options, options[1]); //default button title
        	if (n == 0) {
        		KeyBoardControl.get().ReleaseIfNeeded();
        		KeyBoardControl.get().Activate();
        		int eAvg = drone.getRC().getAverageThrust();
        		loggerDisplayerManager.addGeneralMessegeToDisplay("Setting Keyboard Thrust starting value to " + eAvg);
        		KeyBoardControl.get().SetThrust(eAvg);
        		btnFly.setText("Controler: Keyboard");
        	}
        	else {
        		KeyBoardControl.get().ReleaseIfNeeded();
        		KeyBoardControl.get().Deactivate();
        		int[] rcOutputs = {0, 0, 0, 0, 0, 0, 0, 0};
        		MavLinkRC.sendRcOverrideMsg(drone, rcOutputs);
        		try {
					Thread.sleep(200);
					MavLinkRC.sendRcOverrideMsg(drone, rcOutputs);
            		Thread.sleep(200);
            		MavLinkRC.sendRcOverrideMsg(drone, rcOutputs);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
        		btnFly.setText("Controler: RC");
        	}
        	loggerDisplayerManager.addGeneralMessegeToDisplay("Start Fly '" + options[n] + "'");
        });
        eastPanel_buttons.add(btnFly);        
        
        btnArm = new JToggleButton("Arm Motors");
        btnArm.addActionListener( e -> {
        	if (btnArm.isSelected()) {
        		motorArmed = true;
        		loggerDisplayerManager.addOutgoingMessegeToDisplay("arm");
        		MavLinkArm.sendArmMessage(drone, true);
        	}
        	else {
        		// Not selected
        		if (drone.getState().isFlying()) {
        			Object[] options = {"Land", "RTL", "Cancel"};
                	int n = JOptionPane.showOptionDialog(null, "Drone is flying, dis-arming motor is dangeures, what what you like to do?", "", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                		    null, options, options[0]);
                	if (n == 0) {
                		MavLinkModes.changeFlightMode(drone, ApmModes.ROTOR_LAND);
                		loggerDisplayerManager.addGeneralMessegeToDisplay("Landing");
                		notificationManager.add("Landing");
                	}
                	else if (n == 1) {
                		MavLinkModes.changeFlightMode(drone, ApmModes.ROTOR_RTL);
                		loggerDisplayerManager.addGeneralMessegeToDisplay("RTL");
                		notificationManager.add("RTL");
                	}
                	else 
                		btnArm.setSelected(true);
        		}
        		else {
        			motorArmed = false;
        			loggerDisplayerManager.addOutgoingMessegeToDisplay("disarm");
        			MavLinkArm.sendArmMessage(drone, false);
        		}
        	}
        });
        eastPanel_buttons.add(btnArm);
        
        btnLandRTL = new JButton("Land/RTL");
        btnLandRTL.addActionListener( e -> {
    		KeyBoardControl.get().HoldIfNeeded();
    		Object[] options = {"Land", "RTL", "Cancel"};
        	int n = JOptionPane.showOptionDialog(null, "Choose Land Option", "", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
        		    null, options, drone.getGps().isPositionValid() ? options[1] : options[0]);
        	if (n == 0) {
        		MavLinkModes.changeFlightMode(drone, ApmModes.ROTOR_LAND);
        		loggerDisplayerManager.addGeneralMessegeToDisplay("Landing");
        		notificationManager.add("Landing");
        	}
        	else if(n == 1) {
        		MavLinkModes.changeFlightMode(drone, ApmModes.ROTOR_RTL);
        		loggerDisplayerManager.addGeneralMessegeToDisplay("Comming back to lunch position");
        		notificationManager.add("Return To Lunch");
        	}
        	KeyBoardControl.get().ReleaseIfNeeded();
        });
        eastPanel_buttons.add(btnLandRTL);
        
        btnTakeoff = new JButton("Takeoff");
        btnTakeoff.addActionListener( e -> {
    		if (takeOffThreadRunning) {
    			JOptionPane.showMessageDialog(null, "Takeoff procedure was already started");
    			return;
    		}
    		takeOffThreadRunning = true;
    		takeOffThread = new SwingWorker<Void, Void>(){
    			
    			@Override
       			protected Void doInBackground() throws Exception {
    				Logger.LogGeneralMessege("Takeoff thread Stated!");
    				
	        		if (!Dashboard.drone.getState().isArmed()) {
						JOptionPane.showMessageDialog(null, "Quad will automatically be armed");
					}
	        		
	        		Object val = JOptionPane.showInputDialog(null, "Choose altitude", "", JOptionPane.OK_CANCEL_OPTION, null, null, 5);
	        		if (val == null) {
	        			System.out.println(getClass().getName() + " Takeoff canceled");
	        			return null;
	        		}
	        		
	        		try {
	        			double real_value = Double.parseDouble((String) val);
	        			System.out.println(getClass().getName() + " Required hight is " + real_value);
	        			if (real_value < 0 || real_value > 15) {
	        				JOptionPane.showMessageDialog(null, "Altitude limition during takeoff is limited to 15");
	        				return null;
	        			}
	        			
	        			ArmQuad armQuad = new ArmQuad(drone);
						TakeoffQuad toff = new TakeoffQuad(drone, real_value);
						armQuad.setNext(toff);
	        			armQuad.go();
	        		}
	        		catch (NumberFormatException e) {
	        			JOptionPane.showMessageDialog(null, "Failed to get required height for value '" + val + "'");
	        		}
	        		catch (Exception e) {
	        			JOptionPane.showMessageDialog(null, "Failed to get required height for value '" + val + "'\n" + e.getMessage());
	        		}
	        		return null;
    			}
    			

       			@Override
                protected void done() {
       				takeOffThreadRunning = false;
       				Logger.LogGeneralMessege("Takeoff thread Done!");
                }
    		};
    		takeOffThread.execute();
        });
        eastPanel_buttons.add(btnTakeoff);
        
        btnFollowBeaconShow = new JButton("Show Beacon");
        btnFollowBeaconShow.addActionListener( e -> {        		
    		SwingWorker<Void, Void> w = new SwingWorker<Void, Void>(){
				
       			@Override
       			protected Void doInBackground() throws Exception {	       				
					drone.getBeacon().syncBeacon();
					return null;
				}
			};
   		
			w.execute();
        });
        eastPanel_buttons.add(btnFollowBeaconShow);
        
        btnFollowBeaconStart = new JToggleButton("Lock on Beacon");
        btnFollowBeaconStart.addActionListener( e -> {
    		if (btnFollowBeaconStart.isSelected()) {        			
    			FollowBeaconStartThread = new SwingWorker<Void, Void>(){
    				@Override
	       			protected Void doInBackground() throws Exception {
		        		if (!Dashboard.drone.getState().isArmed()) {
							JOptionPane.showMessageDialog(null, "Quad will automatically be armed");
						}
		        		
		        		if (!Dashboard.drone.getState().isFlying()) {
			        		Object val = JOptionPane.showInputDialog(null, "Choose altitude", "", JOptionPane.OK_CANCEL_OPTION, null, null, 5);
			        		if (val == null) {
			        			System.out.println(getClass().getName() + " Takeoff canceled");
			        			btnFollowBeaconStart.setSelected(false);				        			
			        			return null;
			        		}
		        		
			        		try {
			        			double real_value = Double.parseDouble((String) val);
			        			System.out.println(getClass().getName() + " Required height is " + real_value);
			        			if (real_value < 0 || real_value > 15) {
			        				JOptionPane.showMessageDialog(null, "Altitude limition during takeoff is limited to 15");
			        				btnFollowBeaconStart.setSelected(false);
			        				return null;
			        			}
			        			
			        			ArmQuad armQuad = new ArmQuad(drone);
	        					if (!armQuad.go()) {
	        						btnFollowBeaconStart.setSelected(false);
	        						return null;
	        					}
			        			
			        			TakeoffQuad toff = new TakeoffQuad(drone, real_value);
			        			if (!toff.go()) {
        							btnFollowBeaconStart.setSelected(false);
        							return null;
        						}
    		        			
    		        			//ActivateBeacon();
			        			drone.getFollow().toggleFollowMeState();
			        		}
			        		catch (NumberFormatException e) {
			        			JOptionPane.showMessageDialog(null, "Failed to get required height for value '" + val + "'");
			        		}
			        		catch (InterruptedException e) {
			        			JOptionPane.showMessageDialog(null, "Beacon lock operation was cancel");
			        		}
			        		catch (Exception e) {
			        			JOptionPane.showMessageDialog(null, "Failed to get required height for value '" + val + "'\n" + e.getMessage());
			        		}
		        		}
		        		else {
							//ActivateBeacon();
		        			drone.getFollow().toggleFollowMeState();
		        		}
						return null;
    				}
    			};
    			FollowBeaconStartThread.execute();
    		}
    		else {
    			// Not selected
    			//DeactivateBeacon();
    			drone.getFollow().toggleFollowMeState();
    			loggerDisplayerManager.addErrorMessegeToDisplay("Not Selected");
        		if (FollowBeaconStartThread != null)
        			FollowBeaconStartThread.cancel(true);
        		
        		FollowBeaconStartThread = null;
        	}
        });
        eastPanel_buttons.add(btnFollowBeaconStart);
        
        btnGCSShow = new JButton("Get GCS Position");
        btnGCSShow.addActionListener( e -> {
    		SwingWorker<Void, Void> w = new SwingWorker<Void, Void>(){
				
       			@Override
       			protected Void doInBackground() throws Exception {
					GCSLocationData gcslocation = GCSLocationData.fetch();
					if (gcslocation == null) {
						loggerDisplayerManager.addErrorMessegeToDisplay("Failed to get beacon point from the web");
						return null;
					}
					drone.getGCS().setPosition(gcslocation.getCoordinate());
					drone.getGCS().UpdateAll();
					return null;
				}
			};
   		
			w.execute();
        });
        eastPanel_buttons.add(btnGCSShow);
        
        btnStopFollow = new JButton("Kill Follow");
        btnStopFollow.addActionListener( e -> {        		
    		SwingWorker<Void, Void> w = new SwingWorker<Void, Void>(){
				
       			@Override
       			protected Void doInBackground() throws Exception {
       				//DeactivateBeacon();
					return null;
				}
			};
   		
			w.execute();
        });
        eastPanel_buttons.add(btnStopFollow);
        
        btnHoldPosition = new JButton("Hold Position");
        btnHoldPosition.addActionListener( e -> {
    		if (!drone.getState().isArmed()) {
    			loggerDisplayerManager.addErrorMessegeToDisplay("Quad must be armed in order to change this mode");
    			return;
    		}
    		
    		if (drone.getGps().isPositionValid()) {
    			drone.getState().changeFlightMode(ApmModes.ROTOR_POSHOLD);
    			loggerDisplayerManager.addGeneralMessegeToDisplay("Flight Mode set to 'Position Hold' - GPS");
    		}
    		else {
    			drone.getState().changeFlightMode(ApmModes.ROTOR_ALT_HOLD);
    			loggerDisplayerManager.addGeneralMessegeToDisplay("Flight Mode set to 'Altitude Hold' - Barometer");
    		}
        });
        btnHoldPosition.setEnabled(false);
        eastPanel_buttons.add(btnHoldPosition);
        
        btnStartMission = new JButton("Start Mission");
        btnStartMission.addActionListener( e -> drone.getState().changeFlightMode(ApmModes.ROTOR_AUTO));
        
        btnStartMission.setEnabled(false);
        eastPanel_buttons.add(btnStartMission);
        
        tbTelemtry = new JToolBar();
        eastPanel.add(tbTelemtry);
        
        telemetryPanel = new JPanelTelemetrySatellite();
        tbTelemtry.add(telemetryPanel);
        
        frame.setVisible(true);
        
        setButtonControl(false);
	}
	
	/*private void ActivateBeacon() throws InterruptedException {
		int delay = 5;
		addGeneralMessegeToDisplay("Start following beacon in ...");
		while (delay > 0) {
			addGeneralMessegeToDisplay("" + delay);
			Thread.sleep(1000);
			delay--;
		}
		addGeneralMessegeToDisplay("Go");
		drone.getBeacon().setActive(true);
	}*/
	
	/*private void DeactivateBeacon() {
		loggerDisplayerManager.addGeneralMessegeToDisplay("Stopping Follow");
		drone.getBeacon().setActive(false);
	}*/

	public void SetHeartBeat(boolean on) {
		if (on) {
			keepAliveLabel.setText("Connected");
			keepAliveLabel.setForeground(Color.GREEN);
			return;
		}
		
		keepAliveLabel.setText("Disconnected");
		keepAliveLabel.setForeground(Color.RED);
	}
	
	private void setButtonControl(boolean val) {
		btnArm.setEnabled(val);
		btnFly.setEnabled(val);
		btnLandRTL.setEnabled(val);
		btnTakeoff.setEnabled(val);
		btnHoldPosition.setEnabled(val);
		btnStartMission.setEnabled(val);
		btnFollowBeaconShow.setEnabled(val);
		btnFollowBeaconStart.setEnabled(val);
		btnGCSShow.setEnabled(val);
		btnStopFollow.setEnabled(val);
	}

	public void VerifyBattery(double bat) {
		//final Color orig_color = lblBattery.getBackground();
		if (drone.getState().isFlying() && bat < 100) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			//PaintAllWindow(Color.RED);
			notificationManager.add("Low Battery");
		}
		else {
			//lblBattery.setForeground(Color.BLACK);
			//Color c = new Color(238, 238 ,238);
			//PaintAllWindow(orig_color);
		}
		//lblBattery.setText((bat < 0 ? 0 : bat) + "%");
	}
	
	public void PaintAllWindow(Color c) {
		if (c == telemetryPanel.getBackground()) {
			System.out.println("Same Color");
			return;
		}
		telemetryPanel.setBackground(c);
		int cnt = telemetryPanel.getComponentCount();
		for (int i = 0 ; i < cnt ; i++) {
			telemetryPanel.getComponent(i).setBackground(c);
			telemetryPanel.getComponent(i).repaint();
		}
		telemetryPanel.repaint();
		
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		switch (event) {
			case LEFT_PERIMETER:
				notificationManager.add("Outside Perimeter");
				loggerDisplayerManager.addErrorMessegeToDisplay("Quad left the perimeter");
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			case ENFORCING_PERIMETER:
				notificationManager.add("Enforcing Perimeter");
				loggerDisplayerManager.addErrorMessegeToDisplay("Enforcing Perimeter");
				return;
			case ORIENTATION:
				SetDistanceToWaypoint(drone.getMissionStats().getDistanceToWP().valueInMeters());
				return;
			case BATTERY:
				VerifyBattery(drone.getBattery().getBattRemain());
				return;
			case HEARTBEAT_FIRST:
				loggerDisplayerManager.addErrorMessegeToDisplay("Quad Connected");
				SetHeartBeat(true);
				return;
			case HEARTBEAT_RESTORED:
				loggerDisplayerManager.addErrorMessegeToDisplay("Quad Connected");
				SetHeartBeat(true);
				return;
			case HEARTBEAT_TIMEOUT:
				loggerDisplayerManager.addErrorMessegeToDisplay("Quad Disconnected");
				SetHeartBeat(false);
				return;
			case MODE:
				frame.setTitle(frame.getTitle() + "(" + drone.getState().getMode().getName() + ")");
				return;
			case PARAMETER:
				LoadParameter(drone.getParameters().getExpectedParameterAmount());
				return;
			case PARAMETERS_DOWNLOADED:
				loggerDisplayerManager.addGeneralMessegeToDisplay("Parameters Downloaded succussfully");
				return;
			case TEXT_MESSEGE:
				loggerDisplayerManager.addIncommingMessegeToDisplay(drone.getMessegeQueue().pop());
				return;
			case ARMING:
				motorArmed = drone.getState().isArmed();
				btnArm.setSelected(motorArmed);
				return;
			case WARNING_SIGNAL_WEAK:
				loggerDisplayerManager.addErrorMessegeToDisplay("Warning: Weak signal");
				loggerDisplayerManager.addErrorMessegeToDisplay("Warning: Weak signal");
				loggerDisplayerManager.addErrorMessegeToDisplay("Warning: Weak signal");
				java.awt.Toolkit.getDefaultToolkit().beep();
				java.awt.Toolkit.getDefaultToolkit().beep();
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			case FOLLOW_START:
				loggerDisplayerManager.addGeneralMessegeToDisplay("Follow Me Started");
				return;
			case FOLLOW_UPDATE:
				loggerDisplayerManager.addGeneralMessegeToDisplay("Follow Me Updated");
				return;
			case FOLLOW_STOP:
				loggerDisplayerManager.addGeneralMessegeToDisplay("Follow Me Ended");
				return;
		}
	}

	private void LoadParameter(int expectedParameterAmount) {
		setProgressBar(0, drone.getParameters().getLoadedDownloadedParameters(), drone.getParameters().getExpectedParameterAmount());
		int prc = (int) (((double) (drone.getParameters().getLoadedDownloadedParameters()) / drone.getParameters().getExpectedParameterAmount()) * 100);
		if (prc > 95) {
			System.out.println(getClass().getName() + " Setup stream rate");
			//MavLinkStreamRates.setupStreamRates(drone.getMavClient(), 1, 1, 1, 1, 1, 1, 1, 1);
			MavLinkStreamRates.setupStreamRates(drone.getMavClient(), 1, 1, 1, 1, 4, 1, 1, 1);
			setButtonControl(true);
			System.out.println(getClass().getName() + " " + drone.getParameters().getParameter("MOT_SPIN_ARMED"));
			if (drone.isConnectionAlive()) {
				SetHeartBeat(true);
				//SetFlightModeLabel(drone.getState().getMode().getName());
				drone.notifyDroneEvent(DroneEventsType.MODE);
			}
		}
	}

	public void setProgressBar(int min, int current, int max) {
		if (!progressBar.isVisible() || progressBar.getMaximum() != max) {
			progressBar.setMinimum(min);
			progressBar.setValue(current);
			progressBar.setMaximum(max);
			progressBar.setVisible(true);
		}
		progressBar.setValue(current);
		
		if (progressBar.getValue() == progressBar.getMaximum()) {
			progressBar.setVisible(false);
			progressBar.setValue(0);
		}
	}
	
	public void setProgressBar(int min, int max) {
		setProgressBar(min, progressBar.getValue()+1, max);
	}

	public void resetProgressBar() {
		progressBar.setValue(0);
	}

	@Override
	public void ClearNotification() {
		pnlToolbar_North.ClearNotification();
	}

	@Override
	public void SetNotification(String notification) {
		pnlToolbar_North.SetNotification(notification);
	}

	@Override
	public String getDisplayedLoggerText() {
		if (logBox == null)
			System.err.println("LogBox was not created");
		return logBox.getText();
	}   
	
	@Override
	public void setDisplayedLoggerText(String text) {
		if (logBox == null)
			System.err.println("LogBox was not created");
		logBox.setText(text);
	}
}
