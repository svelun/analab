/**
 * 
 */
package world;

enum ColorCharge { RED, GREEN, BLUE, ANTI_RED, ANTI_GREEN, ANTI_BLUE };

/**
 * @author svlu
 *
 */
abstract class Quark extends Thing {
	
	protected ColorCharge colorCharge;
	
	/**
	 * @param position
	 * @param mass
	 */
	protected Quark(Point position, double massMev) {
		super(position, massMev * 1.777778E-30);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see world.Thing#isIntesecting(world.Thing)
	 */
	@Override
	boolean isIntesecting(Thing t) {
		// TODO Auto-generated method stub
		return false;
	}

}