package mavlink.core.gcs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import mavlink.is.drone.Drone;
import mavlink.is.protocol.msgbuilder.MavLinkHeartbeat;

/**
 * This class is used to send periodic heartbeat messages to the drone.
 */

@Component("gcsHeartbeat")
public class GCSHeartbeat {

	/**
	 * This is the drone to send the heartbeat message to.
	 */
	@Resource(name = "drone")
	private Drone drone;

	/**
	 * This is the heartbeat period in seconds.
	 */
	private int period;

	/**
	 * ScheduledExecutorService used to periodically schedule the heartbeat.
	 */
	private ScheduledExecutorService heartbeatExecutor;

	/**
	 * Runnable used to send the heartbeat.
	 */
	private final Runnable heartbeatRunnable = new Runnable() {
		@Override
		public void run() {
			System.out.println(getClass() + " Sending HB");
			drone.getMavClient();
			MavLinkHeartbeat.sendMavHeartbeat(drone);
		}
	};

	static int called;
	@PostConstruct
	public void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singletone");
		setFrq(1);
		setActive(true);
	}

	/**
	 * Set the state of the heartbeat.
	 * 
	 * @param active
	 *            true to activate the heartbeat, false to deactivate it
	 */
	public void setActive(boolean active) {
		if (active) {
			heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
			heartbeatExecutor
					.scheduleWithFixedDelay(heartbeatRunnable, 0, period, TimeUnit.SECONDS);
		} else if (heartbeatExecutor != null) {
			heartbeatExecutor.shutdownNow();
			heartbeatExecutor = null;
		}
	}

	public void setFrq(int i) {
		period = i;
	}
}
