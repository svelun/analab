/**
 * 
 */
package world;

/**
 * @author svlu
 *
 */
public class Vector {
	Point start;
	Point end;
	private double length = Double.NaN;

	/**
	 * 
	 */
	public Vector(Point start, Point end) {
		if(start == null) throw new IllegalArgumentException("Need a start position, null is illegal");
		if(end == null) throw new IllegalArgumentException("Need an end position, null is illegal");
		this.start = start;
		this.end = end;
	}
	
	public Vector(Vector source)
	{
		try {
			start = source.start.clone();
			end = source.end.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}
	
	void set(Vector source)
	{
		start.set(source.start);
		end.set(source.end);
		length = Double.NaN;
	}
	
	double getLength() {
		if (length == Double.NaN) {
			double s = (end.x - start.x) * (end.x - start.x);
			s += (end.y - start.y) * (end.y - start.y);
			s += (end.z - start.z) * (end.z - start.z);
			length = Math.sqrt(s);
		}
		return length;
	}
	
	/**
	 * Sets a new length of the vector. The
	 * @param len is the new length of vector. 
	 */
	public void setLength(double len) {
		length = len;
		// TODO calculate new end point
		throw new Error("Not yet implemented");
	}
	
	Vector add(Vector s) {
		end.x += s.end.x - s.start.x;
		end.y += s.end.y - s.start.y;
		end.z += s.end.z - s.start.z;
		length = Double.NaN;
		return this;
	}

	/**
	 * @param d
	 */
	public void scale(double scale) {
		setLength(getLength() * scale);
	}
}
