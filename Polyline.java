package cs10;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * A multi-segment Shape, with straight lines connecting "joint" points -- (x1,y1) to (x2,y2) to (x3,y3) ...
 * 
 * @author Rodrigo Cavero Blades, Dartmouth CS 10, Winter 2018
 */
public class Polyline implements Shape {
	private Color color;
	private List<Segment> segments;
	private List<Point> points;
	private int xf,yf;	// endpoint
	
	/**
	 * Initial 0-length shape at a point
	 */
	public Polyline(int x1, int y1, Color color) {
		this.color = color;
		segments = new ArrayList<Segment>();
		xf = x1;
		yf = y1;
	}
	
	@Override
	public void moveBy(int dx, int dy) {
		for (Segment seg : segments) {
			seg.moveBy(dx, dy);
		}
	}
	
	public void addPoint(int x, int y) {
		Segment seg = new Segment(xf, yf, x, y, color);
		segments.add(seg);
		xf = x;
		yf = y;
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public void setColor(Color color) {
		this.color = color;
		for (Segment seg : segments) {
			seg.setColor(color);
		}
	}
	
	@Override
	public boolean contains(int x, int y) {
		boolean result = false;
		for (Segment seg : segments) {
			result = result || seg.contains(x, y);
		}
		return result;
	}

	@Override
	public void draw(Graphics g) {
		for (Segment seg : segments) {
			seg.draw(g);
		}
	}

	@Override
	public String toString() {
		String result = "polyline ";
		for (Segment seg : segments) {
			result += seg.endPointString();
		}
		result += color.getRGB();
		return result;
	}
}
