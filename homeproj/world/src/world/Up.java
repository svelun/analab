/**
 * 
 */
package world;

/**
 * @author svlu
 *
 */
public class Up extends Quark {

	/**
	 * @param position
	 * @param mass
	 */
	protected Up(Point position) {
		super(position, 2.3 * 1.777778E-30);
		charge = 1/3d;
		colorCharge = ColorCharge.RED;
	}

}
