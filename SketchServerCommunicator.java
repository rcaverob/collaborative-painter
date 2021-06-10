package cs10;
import java.io.*;
import java.net.Socket;
import java.util.Map.Entry;

/**
 * Handles communication between the server and one client, for SketchServer
 *
 * @author Rodrigo Cavero Blades, Dartmouth CS 10, Winter 2018;
 */
public class SketchServerCommunicator extends Thread {
	private Socket sock;					// to talk with client
	private BufferedReader in;				// from client
	private PrintWriter out;				// to client
	private SketchServer server;			// handling communication for

	public SketchServerCommunicator(Socket sock, SketchServer server) {
		this.sock = sock;
		this.server = server;
	}

	/**
	 * Sends a message to the client
	 * @param msg
	 */
	public void send(String msg) {
		out.println(msg);
	}
	
	/**
	 * Keeps listening for and handling (your code) messages from the client
	 */
	public void run() {
		try {
			System.out.println("someone connected");
			
			// Communication channel
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintWriter(sock.getOutputStream(), true);

			// Tell the client the current state of the world
			for ( Entry<Integer, Shape> entry : server.getSketch().getShapes().entrySet()) {
				out.println("draw "+ entry.getValue() + " " + entry.getKey());
			}

			// Keep getting and handling messages from the client
			String line;
			while ((line = in.readLine()) != null) {
				server.handleMessage(line);
			}

			// Clean up -- note that also remove self from server's list so it doesn't broadcast here
			server.removeCommunicator(this);
			out.close();
			in.close();
			sock.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
