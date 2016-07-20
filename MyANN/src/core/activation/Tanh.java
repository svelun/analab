/**
 * 
 */
package core.activation;

import core.ActivationFunction;

/**
 * @author svlu
 *
 */
public class Tanh implements ActivationFunction {

	/* (non-Javadoc)
	 * @see core.ActivationFunction#activation(double)
	 */
	@Override
	public double activation(double netSum) {
		return Math.tanh(netSum);
	}

	/* (non-Javadoc)
	 * @see core.ActivationFunction#derivate(double)
	 */
	@Override
	public double derivative(double value) {
		return 1 - (activation(value) * activation(value));
	}

}
