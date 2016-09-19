package desktop.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import mavlink.is.protocol.msg_metadata.MAVLinkPacket;
import mavlink.is.protocol.msgbuilder.MAVLinkStreams.MAVLinkOutputStream;
import mavlink.is.protocol.msgparser.Parser;

public class Connection implements MAVLinkOutputStream {
	public int localPort = 14550;
	public InetAddress hostAdd = null;
	public int hostPort;
	public byte[] receiveData = new byte[1024];
	public byte[] sendBuffer = new byte[1024];
	public Parser parser = new Parser();
	public DatagramSocket socket;
	public int length;

	public Connection(int port) {
		localPort = port;
	}

	public byte[] readDataBlock() throws IOException {
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		socket.receive(receivePacket);
		length = receivePacket.getLength();
		hostAdd = receivePacket.getAddress();
		hostPort = receivePacket.getPort();
		return receivePacket.getData();
	}

	public void getUdpStream() throws SocketException {
		socket = new DatagramSocket(localPort);
		socket.setBroadcast(true);
		socket.setReuseAddress(true);
	}

	public void sendBuffer(byte[] buffer) {
		try {
			if (hostAdd != null) {
				DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length, hostAdd,
						hostPort);
				socket.send(udpPacket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toggleConnectionState() {
		// TODO Auto-generated method stub
	}

	@Override
	public void sendMavPacket(MAVLinkPacket packet) {
		sendBuffer(packet.encodePacket());
	}

	@Override
	public boolean isConnected() {
		return hostAdd != null;
	}

	@Override
	public void openConnection() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeConnection() {
		// TODO Auto-generated method stub
		
	}
}