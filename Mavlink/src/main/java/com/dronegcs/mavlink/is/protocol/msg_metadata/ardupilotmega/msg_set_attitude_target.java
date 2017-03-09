// MESSAGE SET_ATTITUDE_TARGET PACKING
package com.dronegcs.mavlink.is.protocol.msg_metadata.ardupilotmega;

import com.dronegcs.mavlink.is.protocol.msg_metadata.MAVLinkMessage;
import com.dronegcs.mavlink.is.protocol.msg_metadata.MAVLinkPacket;
import com.dronegcs.mavlink.is.protocol.msg_metadata.MAVLinkPayload;

/**
* Set the vehicle attitude and body angular rates.
*/
public class msg_set_attitude_target extends MAVLinkMessage{

	public static final int MAVLINK_MSG_ID_SET_ATTITUDE_TARGET = 82;
	public static final int MAVLINK_MSG_LENGTH = 39;
	private static final long serialVersionUID = MAVLINK_MSG_ID_SET_ATTITUDE_TARGET;
	

 	/**
	* Timestamp in milliseconds since system boot
	*/
	public int time_boot_ms; 
 	/**
	* Attitude quaternion (w, x, y, z order, zero-rotation is 1, 0, 0, 0)
	*/
	public float q[] = new float[4]; 
 	/**
	* Body roll rate in radians per second
	*/
	public float body_roll_rate; 
 	/**
	* Body roll rate in radians per second
	*/
	public float body_pitch_rate; 
 	/**
	* Body roll rate in radians per second
	*/
	public float body_yaw_rate; 
 	/**
	* Collective thrust, normalized to 0 .. 1 (-1 .. 1 for vehicles capable of reverse trust)
	*/
	public float thrust; 
 	/**
	* System ID
	*/
	public byte target_system; 
 	/**
	* Component ID
	*/
	public byte target_component; 
 	/**
	* Mappings: If any of these bits are set, the corresponding input should be ignored: bit 1: body roll rate, bit 2: body pitch rate, bit 3: body yaw rate. bit 4-bit 6: reserved, bit 7: throttle, bit 8: attitude
	*/
	public byte type_mask; 

	/**
	 * Generates the payload for a com.dronegcs.mavlink.is.mavlink message for a message of this type
	 * @return
	 */
	public MAVLinkPacket pack(){
		MAVLinkPacket packet = new MAVLinkPacket();
		packet.len = MAVLINK_MSG_LENGTH;
		packet.sysid = 255;
		packet.compid = 190;
		packet.msgid = MAVLINK_MSG_ID_SET_ATTITUDE_TARGET;
		packet.payload.putInt(time_boot_ms);
		 for (int i = 0; i < q.length; i++) {
                        packet.payload.putFloat(q[i]);
            }
		packet.payload.putFloat(body_roll_rate);
		packet.payload.putFloat(body_pitch_rate);
		packet.payload.putFloat(body_yaw_rate);
		packet.payload.putFloat(thrust);
		packet.payload.putByte(target_system);
		packet.payload.putByte(target_component);
		packet.payload.putByte(type_mask);
		return packet;		
	}

    /**
     * Decode a set_attitude_target message into this class fields
     *
     * @param payload The message to decode
     */
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
	    time_boot_ms = payload.getInt();
	     for (int i = 0; i < q.length; i++) {
			q[i] = payload.getFloat();
		}
	    body_roll_rate = payload.getFloat();
	    body_pitch_rate = payload.getFloat();
	    body_yaw_rate = payload.getFloat();
	    thrust = payload.getFloat();
	    target_system = payload.getByte();
	    target_component = payload.getByte();
	    type_mask = payload.getByte();    
    }

     /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_set_attitude_target(){
    	msgid = MAVLINK_MSG_ID_SET_ATTITUDE_TARGET;
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a com.dronegcs.mavlink.is.mavlink packet
     * 
     */
    public msg_set_attitude_target(MAVLinkPacket mavLinkPacket){
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.msgid = MAVLINK_MSG_ID_SET_ATTITUDE_TARGET;
        unpack(mavLinkPacket.payload);
        //Log.d("MAVLink", "SET_ATTITUDE_TARGET");
        //Log.d("MAVLINK_MSG_ID_SET_ATTITUDE_TARGET", toString());
    }
    
                  
    /**
     * Returns a string with the MSG name and data
     */
    public String toString(){
    	return "MAVLINK_MSG_ID_SET_ATTITUDE_TARGET -"+" time_boot_ms:"+time_boot_ms+" q:"+q+" body_roll_rate:"+body_roll_rate+" body_pitch_rate:"+body_pitch_rate+" body_yaw_rate:"+body_yaw_rate+" thrust:"+thrust+" target_system:"+target_system+" target_component:"+target_component+" type_mask:"+type_mask+"";
    }
}
