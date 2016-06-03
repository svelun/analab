/**
 * 
 */
package world;

/**
 * @author svlu
 *
 */
public class Point implements Cloneable {
	double x, y, z;

	Point(double x, double y, double z) {
		setX(x);
		setY(y);
		setZ(z);
	}
	
	/**
	 * 
	 */
	public Point() {
		// x, y, and z is zero
	}

	public Point clone() throws CloneNotSupportedException
	{
		return (Point)super.clone();
	}
	
	public boolean isBigger(Point b)
	{
		if(x >= b.x && y >= b.y && z >= b.z) return true;
		else return false;
	}
	
	public boolean isSmaller(Point b)
	{
		if(x <= b.x && y <= b.y && z <= b.z) return true;
		else return false;
	}
	
	/**
	 * @return the x
	 */
	public double getX() {
		return x;
	}

	/**
	 * @param x the x to set
	 */
	public void setX(double x) {
		if (!Double.isFinite(x)) throw new IllegalArgumentException("Point value x must be finite");
		this.x = x;
	}

	/**
	 * @return the y
	 */
	public double getY() {
		return y;
	}

	/**
	 * @param y the y to set
	 */
	public void setY(double y) {
		if (!Double.isFinite(y)) throw new IllegalArgumentException("Point value y must be finite");
		this.y = y;
	}

	/**
	 * @return the z
	 */
	public double getZ() {
		return z;
	}

	/**
	 * @param z the z to set
	 */
	public void setZ(double z) {
		if (!Double.isFinite(z)) throw new IllegalArgumentException("Point value z must be finite");
		this.z = z;
	}

	/**
	 * @param source
	 */
	public void set(Point source) {
		x = source.x;
		y = source.y;
		z = source.z;
	}	
}
