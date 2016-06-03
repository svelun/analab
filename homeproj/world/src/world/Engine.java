/**
 * 
 */
package world;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author svlu
 *
 */
public class Engine {
	static private ArrayList<Thing> things = new ArrayList<Thing>(1000);
	
	static Collection<Thing> getThings() { return things; }
	
}
