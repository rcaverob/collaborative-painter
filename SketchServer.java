package cs10;
import java.net.*;
import java.util.*;
import java.awt.Color;
import java.io.*;

/**
 * A server to handle sketches: getting requests from the clients,
 * updating the overall state, and passing them on to the clients
 *
 * @author Rodrigo Cavero Blades, Dartmouth CS 10, Winter 2018;
 */
 
public class SketchServer {
	private ServerSocket listen;						// for accepting connections
	private ArrayList<SketchServerCommunicator> comms;	// all the connections with clients
	private Sketch sketch;								// the state of the world
	int curId = 0;										// unique ID number keep track of the shapes	
	
	public SketchServer(ServerSocket listen) {
		this.listen = listen;
		sketch = new Sketch();
		comms = new ArrayList<SketchServerCommunicator>();
	}

	public Sketch getSketch() {
		return sketch;
	}
	
	/**
	 * The usual loop of accepting connections and firing off new threads to handle them
	 */
	public void getConnections() throws IOException {
		System.out.println("server ready for connections");
		while (true) {
			SketchServerCommunicator comm = new SketchServerCommunicator(listen.accept(), this);
			comm.setDaemon(true);
			comm.start();
			addCommunicator(comm);
		}
	}

	/**
	 * Adds the communicator to the list of current communicators
	 */
	public synchronized void addCommunicator(SketchServerCommunicator comm) {
		comms.add(comm);
	}

	/**
	 * Removes the communicator from the list of current communicators
	 */
	public synchronized void removeCommunicator(SketchServerCommunicator comm) {
		comms.remove(comm);
	}

	/**
	 * Sends the message from the one communicator to all (including the originator)
	 */
	public synchronized void broadcast(String msg) {
		for (SketchServerCommunicator comm : comms) {
			comm.send(msg);
		}
	}
	
	
	/**
	 * Handles messages from the client and acts on the received commands. This differs from the handleMessage
	 * in the Editor class in that it assigns an id number to shapes and broadcasts the message
	 * to all other clients (Editors).
	 */
	public synchronized void handleMessage(String msg) {
		String[] parts = msg.split(" ");
		int numParts = parts.length;
		String command = parts[0];
		
		int[] info = new int[numParts - 2];
		for (int i = 2; i < numParts; i ++) {
			info[i - 2] = Integer.parseInt(parts[i]);
		}
		
		switch (command) {
		case "draw":
			String type = parts[1];
			drawShape(type, info);
			msg += " " + curId;
			curId ++;
			break;
		case "move":
			int id = Integer.parseInt(parts[1]);
			Shape toMove = sketch.getShapes().get(id);
			toMove.moveBy(info[0], info[1]);
			break;
		case "recolor":
			int id_r = Integer.parseInt(parts[1]);
			Shape toRecolor = sketch.getShapes().get(id_r);
			Color c = new Color(info[0]);
			toRecolor.setColor(c);
			break;
		case "delete":
			int id_d = Integer.parseInt(parts[1]);
			sketch.getShapes().remove(id_d);
			break;
		}
		broadcast(msg);
	}
	
	/**
	 * Creates a shape as specified by the parameters passed (helper for handleMessage) 
	 * and adds it to the Sketch kept by the server
	 * also assigns a unique id to each shape
	 */
	private void drawShape(String type, int[] info) {
		switch (type) {
		case "ellipse":
			Color c = new Color(info[4]);
			Ellipse e = new Ellipse(info[0], info[1], info[2], info[3], c);
			sketch.addShape(e, curId);
			break;
		case "rectangle":
			Color cr = new Color(info[4]);
			Rectangle r = new Rectangle(info[0], info[1], info[2], info[3], cr);
			sketch.addShape(r, curId);
			break;
		case "segment":
			Color cs = new Color(info[4]);
			Segment seg = new Segment(info[0], info[1], info[2], info[3], cs);
			sketch.addShape(seg, curId);
			break;
		case "polyline":
			Color cp = new Color(info[info.length-1]);
			Polyline pol = new Polyline(info[0], info[1], cp);
			for (int i = 2; i < info.length -2; i+= 2) {
				pol.addPoint(info[i], info[i+1]);
			}
			sketch.addShape(pol, curId);
		}
	}
	
	public static void main(String[] args) throws Exception {
		new SketchServer(new ServerSocket(4242)).getConnections();
	}
}
