package cs10;

import java.util.Map;
import java.util.TreeMap;

 /**
 * Handles the storing of the shapes for a Graphical Editor.
 * 
 * @author Rodrigo Cavero Blades, Dartmouth CS 10, Winter 2018
 */
public class Sketch {
	
	/**
	 * Holds the shapes, mapping the id numbers to each shape.
	 */
	private Map<Integer, Shape> shapes;

	public Sketch() {
		shapes = new TreeMap<Integer, Shape>();
	}
	public synchronized Map<Integer, Shape> getShapes() {
		return shapes;
	}
	
	/**
	 * Adds the given shape with the given id number
	 */
	public synchronized void addShape(Shape s, int id) {
		shapes.put(id, s);
	}	
}
