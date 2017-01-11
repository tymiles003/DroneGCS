package mavlink.drone.variables;

import java.util.Vector;

import org.springframework.stereotype.Component;

import mavlink.drone.DroneVariable;
import mavlink.drone.DroneInterfaces.DroneEventsType;

@Component("messeges")
public class Messeges extends DroneVariable {

	private Vector<String> messeges;
	
	static int called;
	public void init() {
		if (called++ > 1)
			throw new RuntimeException("Not a Singletone");
		messeges = new Vector<String>();
	}
	
	public String pop() {
		if (messeges == null)
			return null;
		
		if (messeges.isEmpty())
			return null;
		
		String res = messeges.get(0);
		messeges.remove(0);
		
		return res;
	}
	
	public void push(String text) {
		messeges.addElement(text);
		drone.notifyDroneEvent(DroneEventsType.TEXT_MESSEGE);
	}
}