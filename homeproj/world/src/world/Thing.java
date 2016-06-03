/**
 * 
 */
package world;

/**
 * @author svlu
 *
 */
public abstract class Thing extends Thread {
	Point position;
	protected double mass;
	protected double charge;
	protected double rotationMomentum;
	protected double rotationAngelXY, rotationAngelXZ;
	protected Box limitingBox;
	private Vector netForce;
	private Vector acceleration;
	private long lastSample;
	private Vector velocity;
	
	Thing(Point position, double mass) {
		super("A thing, with mass: " + mass + "kg");
		if(position == null) throw new IllegalArgumentException("A thing needs a position");
		if(!Double.isFinite(mass)) throw new IllegalArgumentException("A thing needs a finite mass");
		this.position = position;
		this.mass = mass;
		limitingBox = new Box();
		netForce = new Vector(getCenterOfGravity(), getCenterOfGravity());
		start();
	}

	public Point getCenterOfGravity() { return position; }
	
	public boolean isInCollisionRange(Thing b)
	{
		if ((limitingBox.cornerA.isBigger(b.limitingBox.cornerA)) &&
				(limitingBox.cornerA.isSmaller(b.limitingBox.cornerB)))
			return true;
		return false;
	}

	abstract boolean isIntesecting(Thing t);
	Box getLimitBox() {
		return limitingBox;
	}

	void forces(Thing b) {
		try {
			netForce.start = getCenterOfGravity();
			netForce.end.set(netForce.start);

			Vector f = new Vector(position.clone(), b.position.clone());
			double r = f.getLength();
			double force = mass * b.mass / (r * r) * 6.673E+11;
			f.setLength(force);
			netForce.add(f);
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	protected void calculatePosition(long time) {
		long deltaTime = time - lastSample;
		if (deltaTime < 0) throw new Error("Time seams to go backwards: now: " + time + " old time: " + lastSample);
		acceleration = new Vector(netForce);
		acceleration.scale(1/mass);
		acceleration.scale(deltaTime/1000d);
		velocity.add(acceleration);
		position.x += velocity.end.x * 1000d / deltaTime;
		position.y += velocity.end.y * 1000d / deltaTime;
		position.z += velocity.end.z * 1000d / deltaTime;
	}
	
	public void run() {
		lastSample = System.currentTimeMillis();
		velocity = new Vector(new Point(), new Point()); // Velocity is zero
		try {
			for(;;) {
				sleep(10);
				calculatePosition(System.currentTimeMillis());
			}
		}
		catch(InterruptedException ie) {}
	}
}
