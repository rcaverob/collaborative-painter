package cs10;

import java.util.TreeMap;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import cs10.EditorOne.Mode;

/**
 * Client-server graphical editor
 * @author Rodrigo Cavero Blades; based on code skeleton provided in the Dartmouth CS 10  Winter 2018 class
 */

public class Editor extends JFrame {	
	private static String serverIP = "localhost";			// IP address of sketch server
	// "localhost" for your own machine;
	// or ask a friend for their IP address

	private static final int width = 800, height = 800;		// canvas size

	// Current settings on GUI
	public enum Mode {
		DRAW, MOVE, RECOLOR, DELETE
	}
	private Mode mode = Mode.DRAW;				// drawing/moving/recoloring/deleting objects
	private String shapeType = "ellipse";		// type of object to add
	private Color color = Color.black;			// current drawing color

	// Drawing state
	// these are remnants of my implementation; take them as possible suggestions or ignore them
	private Shape curr = null;					// current shape (if any) being drawn
	private Boolean valShape = false;			// keeps track of valid shapes (more than a point)
	private Sketch sketch;						// holds and handles all the completed objects
	private int movingId = -1;					// current shape id (if any; else -1) being moved
	private Point drawFrom = null;				// where the drawing started
	private Point moveFrom = null;				// where object is as it's being dragged
	int curId = 0;

    // Other state
	private Shape curr_mov;						// current shape (if any) being moved

	// Communication
	private EditorCommunicator comm;			// communication with the sketch server

	public Editor() {
		super("Graphical Editor");

		sketch = new Sketch();

		// Connect to server
		comm = new EditorCommunicator(serverIP, this);
		comm.start();

		// Helpers to create the canvas and GUI (buttons, etc.)
		JComponent canvas = setupCanvas();
		JComponent gui = setupGUI();

		// Put the buttons and canvas together into the window
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(canvas, BorderLayout.CENTER);
		cp.add(gui, BorderLayout.NORTH);

		// Usual initialization
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}

	/**
	 * Creates a component to draw into
	 */
	private JComponent setupCanvas() {
		JComponent canvas = new JComponent() {
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawSketch(g);
			}
		};
		
		canvas.setPreferredSize(new Dimension(width, height));

		canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent event) {
				handlePress(event.getPoint());
			}

			public void mouseReleased(MouseEvent event) {
				handleRelease();
			}
		});		

		canvas.addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent event) {
				handleDrag(event.getPoint());
			}
		});
		
		return canvas;
	}

	/**
	 * Creates a panel with all the buttons
	 */
	private JComponent setupGUI() {
		// Select type of shape
		String[] shapes = {"ellipse", "freehand", "rectangle", "segment"};
		JComboBox<String> shapeB = new JComboBox<String>(shapes);
		shapeB.addActionListener(e -> shapeType = (String)((JComboBox<String>)e.getSource()).getSelectedItem());

		// Select drawing/recoloring color
		JButton chooseColorB = new JButton("choose color");
		JColorChooser colorChooser = new JColorChooser();
		JLabel colorL = new JLabel();
		colorL.setBackground(Color.black);
		colorL.setOpaque(true);
		colorL.setBorder(BorderFactory.createLineBorder(Color.black));
		colorL.setPreferredSize(new Dimension(25, 25));
		JDialog colorDialog = JColorChooser.createDialog(chooseColorB,
				"Pick a Color",
				true,  //modal
				colorChooser,
				e -> { color = colorChooser.getColor(); colorL.setBackground(color); },  // OK button
				null); // no CANCEL button handler
		chooseColorB.addActionListener(e -> colorDialog.setVisible(true));

		// Mode: draw, move, recolor, or delete
		JRadioButton drawB = new JRadioButton("draw");
		drawB.addActionListener(e -> mode = Mode.DRAW);
		drawB.setSelected(true);
		JRadioButton moveB = new JRadioButton("move");
		moveB.addActionListener(e -> mode = Mode.MOVE);
		JRadioButton recolorB = new JRadioButton("recolor");
		recolorB.addActionListener(e -> mode = Mode.RECOLOR);
		JRadioButton deleteB = new JRadioButton("delete");
		deleteB.addActionListener(e -> mode = Mode.DELETE);
		ButtonGroup modes = new ButtonGroup(); // make them act as radios -- only one selected
		modes.add(drawB);
		modes.add(moveB);
		modes.add(recolorB);
		modes.add(deleteB);
		JPanel modesP = new JPanel(new GridLayout(1, 0)); // group them on the GUI
		modesP.add(drawB);
		modesP.add(moveB);
		modesP.add(recolorB);
		modesP.add(deleteB);

		// Put all the stuff into a panel
		JComponent gui = new JPanel();
		gui.setLayout(new FlowLayout());
		gui.add(shapeB);
		gui.add(chooseColorB);
		gui.add(colorL);
		gui.add(modesP);
		return gui;
	}

	/**
	 * Getter for the sketch instance variable
	 */
	public Sketch getSketch() {
		return sketch;
	}

	/**
	 * Draws all the shapes in the sketch,
	 * along with the object currently being drawn in this editor (not yet part of the sketch)
	 */
	public void drawSketch(Graphics g) {
		// TODO: YOUR CODE HERE
		for (Shape s : sketch.getShapes().values()) {
			s.draw(g);
		}
		if (curr != null) curr.draw(g);
	}

	// Helpers for event handlers
	
	/**
	 * Helper method for press at point
	 * In drawing mode, start a new object;
	 * in moving mode, (request to) start dragging if clicked in a shape;
	 * in recoloring mode, (request to) change clicked shape's color
	 * in deleting mode, (request to) delete clicked shape
	 */
	private void handlePress(Point p) {
		// TODO: YOUR CODE HERE
		if (mode == Mode.DRAW) {
			switch (shapeType) {
			case "ellipse":
				curr = new Ellipse(p.x, p.y, color);
				drawFrom = p;
				break;
			case "rectangle":
				curr = new Rectangle(p.x, p.y, color);
				drawFrom = p;
				break;
			case "segment":
				curr = new Segment(p.x, p.y, color);
				drawFrom = p;
				break;
			case "freehand":
				curr = new Polyline(p.x, p.y, color);
				drawFrom = p;
				break;
			}
			
		} else if (mode == Mode.MOVE && !sketch.getShapes().isEmpty()) {
			curr_mov = clickedShape(p);
			movingId = clickedShapeID(p);
			if (curr_mov != null) moveFrom = p;
			
		} else if (mode == Mode.RECOLOR && !sketch.getShapes().isEmpty()) {
toRecolorId = clickedShapeID(p);
			if (toRecolorId != -1) sendShapeRecolor(toRecolorId, color.getRGB());

		} else if (mode == Mode.DELETE && !sketch.getShapes().isEmpty()) {
			int id = clickedShapeID(p);
			if (id!= -1) sendShapeDelete(id); 
		}
	}

	/**
	 Returns the front-most shape at a point, or null if there is none.
	 */
	private Shape clickedShape(Point p) {
		Shape clicked = null;
		for (int key : ((TreeMap<Integer, Shape>) sketch.getShapes()).descendingKeySet()) {
			clicked = sketch.getShapes().get(key);
			if (clicked.contains(p.x, p.y)) break;
			clicked = null;
		}
		return clicked;
	}
	
	/**
	 Returns the id of the front-most shape at a point, or -1 if there is none.
	 */
	private int clickedShapeID(Point p) {
		Shape clicked = null;
		for (int key : ((TreeMap<Integer, Shape>) sketch.getShapes()).descendingKeySet()) {
			clicked = sketch.getShapes().get(key);
			if (clicked.contains(p.x, p.y)) return key;
		}
		return -1;
	}

	/**
	 * Helper method for drag to new point
	 * In drawing mode, update the other corner of the object;
	 * in moving mode, (request to) drag the object
	 */
	private void handleDrag(Point p) {
		// TODO: YOUR CODE HERE
		if (mode == Mode.DRAW && drawFrom!= null) {
			valShape = true;
			switch (shapeType) {
			case "ellipse":
				((Ellipse) curr).setCorners(p.x, p.y, drawFrom.x, drawFrom.y);
				repaint();
				break;
			case "rectangle":
				((Rectangle) curr).setCorners(p.x, p.y, drawFrom.x, drawFrom.y);
				repaint();
				break;
			case "segment":
				((Segment) curr).setEnd(p.x, p.y);
				repaint();
				break;
			case "freehand":
				((Polyline) curr).addPoint(p.x, p.y);
				repaint();
				break;
			}
		} else if (mode == Mode.MOVE) {
			if (moveFrom != null) {
				sendShapeMove(movingId, p.x - moveFrom.x, p.y - moveFrom.y);
				moveFrom = p;
				
			}
		}
	}


	/**
	 * Helper method for release
	 * In drawing mode, pass the add new object request on to the server;
	 * in moving mode, release it		
	 */
	private void handleRelease() {
		// TODO: YOUR CODE HERE
		if (mode == Mode.DRAW) {
			if (curr != null && valShape) {
				curId++;
				sendShapeDraw(curr);
				curr = null;
				valShape = false;
			}
			drawFrom = null;
		}
		if (mode == Mode.MOVE) {
			moveFrom = null;
		}	
	}

	/**
	 * Sends a shape to be drawn by the server
	 */
	private void sendShapeDraw(Shape shape) {
		String message = "draw " + shape;
		comm.send(message);		
	}
	
	/**
	 * Sends a shape id to be moved by the server, as well as specifications for the movement
	 */
	private void sendShapeMove(int id, int dx, int dy) {
		String message = "move " + id + " " + dx + " " + dy; 
		comm.send(message);
		
	}

	/**
	 * Sends a shape id to be recolored by the server, as well as the color to recolor it.
	 */
	private void sendShapeRecolor(int id, int rgb) {
		comm.send("recolor "+ id + " " + rgb);
		
	}
	
	/**
	 * Sends a shape id to be deleted by the server
	 */
	private void sendShapeDelete(int id) {
		comm.send("delete" + " " + id);	
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Editor();
			}
		});	
	}

	/**
	 * Handles messages from the server and acts on the received commands.
	 */
	public synchronized void handleMessage(String line) {
		String[] parts = line.split(" ");
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
			break;
		case "move":
			int id = Integer.parseInt(parts[1]);
			Shape toMove = sketch.getShapes().get(id);
			toMove.moveBy(info[0], info[1]);
			repaint();
			break;
		case "recolor":
			int id_r = Integer.parseInt(parts[1]);
			Shape toRecolor = sketch.getShapes().get(id_r);
			Color c = new Color(info[0]);
			toRecolor.setColor(c);
			repaint();
			break;
		case "delete":
			int id_d = Integer.parseInt(parts[1]);
			sketch.getShapes().remove(id_d);
			repaint();
			break;
		}
	}

	/**
	 * Draws a shape, as specified by the parameters passed. (helper for handleMessage)
	 */
	private void drawShape(String type, int[] info) {
		switch (type) {
		case "ellipse":
			Color c = new Color(info[4]);
			Ellipse e = new Ellipse(info[0], info[1], info[2], info[3], c);
			sketch.addShape(e, info[5]);
			break;
		case "rectangle":
			Color cr = new Color(info[4]);
			Rectangle r = new Rectangle(info[0], info[1], info[2], info[3], cr);
			sketch.addShape(r, info[5]);
			break;
		case "segment":
			Color cs = new Color(info[4]);
			Segment seg = new Segment(info[0], info[1], info[2], info[3], cs);
			sketch.addShape(seg, info[5]);
			break;
		case "polyline":
			Color cp = new Color(info[info.length-2]);
			Polyline pol = new Polyline(info[0], info[1], cp);
			for (int i = 2; i < info.length -3; i+= 2) {
				pol.addPoint(info[i], info[i+1]);
			}
			sketch.addShape(pol, info[info.length-1]);
		}	
		repaint();
	}
}
