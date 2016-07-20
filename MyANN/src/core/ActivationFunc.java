package core;
/**
 * 
 */

/**
 * @author svlu
 *
 */
public interface ActivationFunction {
	public double activation(double netSum);
	public double derivative(double value);
}
