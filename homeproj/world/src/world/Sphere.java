/**
 * 
 */
package world;

/**
 * @author svlu
 *
 */
public class Sphere extends Thing {
	double radie;

	/**
	 * 
	 */
	public Sphere(Point position, double mass, double radie) {
		super(position, mass);
		if(!Double.isFinite(radie)) throw new IllegalArgumentException("A sphere needs a finite radie");
		this.radie = radie;
		limitingBox.cornerA.x = position.x - radie;
		limitingBox.cornerA.y = position.y - radie;
		limitingBox.cornerA.z = position.z - radie;
		limitingBox.cornerB.x = position.x + radie;
		limitingBox.cornerB.y = position.y + radie;
		limitingBox.cornerB.z = position.z + radie;
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
