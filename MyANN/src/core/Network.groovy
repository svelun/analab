package core

import core.activation.Tanh
import java.awt.image.BufferedImage
import java.awt.image.ImagingOpException
import java.lang.ProcessEnvironment.Value;
import java.nio.channels.NetworkChannel
import java.util.zip.ZStreamRef
import javax.imageio.ImageIO
import javax.swing.Spring.WidthSpring;

import java.awt.RenderingHints

/**
 * 
 */

/**
 * @author svlu
 *
 */
class ANN {
	int levelPrecision = 1024
	int xSize = 3 * 160
	int ySize = 100
	int zSize = 3
	int inputNeuronFactor = 4
	double learningRate = 0.5
	float weightLimit = 5.0


	private Neuron[][][] neurons
	private Neuron[][] inputNeurons
	private outputNeurons = []
	private NeuronExecuter[] layers
	private int inputCount = 0
	private long maxRunningTime = 10

	private long t1 = System.currentTimeMillis()

	boolean isInitialized = false

	private void init() {
		// Create the Neuron network

		layers = new NeuronExecuter[zSize + 1]
		ActivationFunction func = new Tanh();
		
		// Create the input layer
		int inXSize = xSize * inputNeuronFactor
		int inYSize = ySize * inputNeuronFactor
		println("inXSize " + inXSize + " inYSize " + inYSize + " xSize " + xSize + " ySize " + ySize + " factor " + inputNeuronFactor)
		inputNeurons = new Neuron[inXSize][inYSize]
		inXSize.times ({ x ->
			inYSize.times ({ y -> inputNeurons[x][y] = new InputNeuron(this, func, x, y)})
		})

		neurons = new Neuron[xSize][ySize][zSize]
		xSize.times({ x ->
			ySize.times({ y ->
				zSize.times ({ z ->
					Neuron n
					if (z == 0) {
						n = new ConvolutionalNeuron(this, func, x, y, z)
					}
					else if (z == (zSize - 1)) {
						n = new OutputNeuron(this, func, x, y)
						outputNeurons.add(n)
					}
					else {
						n = new MiddleNeuron(this, func, x, y, z)
					}
					neurons[x][y][z] = n
				})
			})
		})
		layers[0] = new NeuronExecuter(this, -1) // For the input layer (z index == -1)
		zSize.times { z -> layers[z + 1] = new NeuronExecuter(this, z) }
		println ("Used time for network creation: " + (System.currentTimeMillis() - t1))
		Runtime rt = Runtime.getRuntime()
		long totalMemory = rt.totalMemory()
		println ("Memory total: ${totalMemory}  used: " + (totalMemory - rt.freeMemory()))
		synchronized (this) {
			isInitialized = true
			notifyAll()
		}
	}

	public ANN(int width, int height, int depth) {
		xSize = width
		ySize = height
		zSize = depth
		init()
	}

	ANN() {
		init()
	}

	Neuron get(int x, y, z) {
		if (z >= 0) return neurons[x][y][z]
		else if (z == -1) return inputNeurons[x][y]
		throw new Error("Illegal neuron requested: x == ${z} x == ${z} z == ${z}")
	}

	int getLayerWidth(int z) {
		if( z >= 0) return xSize
		else return xSize * inputNeuronFactor
	}
	int getLayerHeight(int z) {
		if( z >= 0) return ySize
		else return ySize * inputNeuronFactor
	}
	int getInputLayerWidth() { return xSize * inputNeuronFactor }
	int getInputLayerHeight() { return ySize * inputNeuronFactor }

	void setLearningRate(double value) { learningRate = value }

	void waitForLayer(int layer, int runCount) {
		if (layer < -1) {
			synchronized (this) {
				println("network runCount " + runCount + " inputCount " + inputCount)
				while(runCount > inputCount) {
					wait()
					println (" new inputCount: " + inputCount)
				}
			}
		}
		else layers[layer + 1].waitForRun(runCount);
	}

	synchronized void setMaxRunningTime(long value) {
		if (value > maxRunningTime) maxRunningTime = value
	}

	long getMaxRunningTime() { return maxRunningTime }

	void setInput(int [][] input) {
		waitForLayer(-1, inputCount)
		(xSize * inputNeuronFactor).times { x ->
			(ySize * inputNeuronFactor).times { y ->
				inputNeurons[x][y].setInput(input[x][y])
			}
		}
		synchronized (this) {
			inputCount++
			println("new inputCount == " + inputCount)
			this.notifyAll()
		}
		Thread.yield()
		synchronized (this) {
			this.notifyAll()
		}
	}

	void feedback(int[] target) {
		double Etotal = 0
		int i = 0
		outputNeurons.each {
			Etotal += ((target[i] - it.getOutput())^2)/2
			it.feedback(target[i])
			i++
		}
		println ("Total error: " + Etotal)
	}
}

class Layer
{
	private int dimensions = 2
	public Neuron getAt(List<Integer> coordinates) {
		// FIXME implent this
		return null
	}
}

class NeuronExecuter extends Thread
{
	int runCount = 0
	private int zIdx
	private ANN network

	NeuronExecuter(ANN network, int zRow) {
		super("Neuron Executer z index: " + zRow)
		this.network = network
		zIdx = zRow
		start()
	}

	synchronized void waitForRun(int runCount) {
		println("layer runCount " + runCount + " this.runCount " + this.runCount)
		while(this.runCount < runCount) {
			println("in waitForRun zIdx = " + zIdx + " this.runCount " + this.runCount)
			wait()
		}
	}

	synchronized int getRunCount() {
		return runCount()
	}

	public void run() {
		network.waitForLayer(zIdx - 1, runCount + 1)

		synchronized (network) {
			try {
				while(!network.isInitialized) { network.wait() }
			} catch (Exception e) {
				e.printStackTrace()
			}
		}

		long ta = 0
		int rc = 0
		while (true) {
			rc++
			long t1 = System.currentTimeMillis()
			for(int x = 0; x < network.getLayerWidth(zIdx); x++) {
				for(int y = 0; y < network.getLayerHeight(zIdx); y++) {
					network.get(x, y, zIdx).run()
				}
			}
			long runningTime = System.currentTimeMillis() - t1
			ta += runningTime
			ta /= 2
			if (ta > network.getMaxRunningTime()) {
				println("${zIdx} running time " + runningTime + " avarage " + ta)
				network.setMaxRunningTime(ta)
			}
			synchronized (this) {
				this.runCount++
				println("new runCount z == " + zIdx + " runCount == " + runCount)
				notifyAll()
			}
		}
	}
}

abstract class Neuron implements Runnable {
	static Random rand = new Random()
	int level = 0
	float internalWeight;
	int x, y, z
	ANN network
	private ActivationFunction activationFunction;
	
	Neuron(ANN network, ActivationFunction func, int x, y, z) {
		this.network = network
		activationFunction = func;
		this.x = x
		this.y = y
		this.z = z
		internalWeight = getStartWeight()
	}

	protected float getStartWeight() { return (float)(rand.nextDouble() * network.weightLimit / 3) }

	int getLevel() { return level }

	void internal_feedback(double error) {
		throw new Error("This method should be overridden")
	}

	public String toString() { return "level = ${level}" }

}

class MiddleNeuron extends Neuron
{
	float[] weight = new float[25]
	//int[] historyInput = new int[9]
	int actualConnections = -1

	MiddleNeuron(ANN network, ActivationFunction func, int x, y, z) {
		super(network, func, x, y, z)
		25.times({ weight[it] = getStartWeight() })
	}

	public void run() {
		double newLevel = 0
		int count = 25
		int z1 = z - 1
		int xSize = network.xSize
		int ySize = network.ySize
		5.times( { itx ->
			int x1 = (x + itx - 2) % xSize
			5.times( { ity ->
				int y1 = (y + ity - 2) % ySize
				int idx = itx * 5 + ity
				newLevel += network.get(x1, y1, z1).level * weight[idx]
			})
		})
		actualConnections = count + 1
		newLevel += getLevel() * internalWeight
		level = activationFunction.activation(newLevel)
		//if (x == 15 && y == 12) println ("############ z == ${z} New Level! " + this)
	}

	void internal_feedback(double error) {
		double ourError = error /* / network.zSize */
		double connectionAdjustment = activationFunction.derivative(ourError) /* / actualConnections) */ * network.learningRate
		float weightLimit = network.weightLimit
		//if (x == 15 && y == 12) println ("z == ${z} Error == ${error} Our error == ${ourError} connectionAdjustment == ${connectionAdjustment}" + this)
		int z1 = z - 1
		int xSize = network.xSize
		int ySize = network.ySize
		5.times( { itx ->
			int x1 = (x + itx - 2) % xSize
			5.times( { ity ->
				int y1 = (y + ity - 2) % ySize
				int idx = itx * 5 + ity
				weight[idx] +=  connectionAdjustment
				if (weight[idx] > weightLimit) weight[idx] = 2.0
				else if (weight[idx] < 0.0) weight[idx] = 0.0
			})
		})

		internalWeight +=  connectionAdjustment
		if (internalWeight > weightLimit) internalWeight = network.levelPrecision
		else if (internalWeight < 0.0) internalWeight = 0.0

		network.get(x, y, z - 1).internal_feedback(error)
	}

	public String toString() { return super.toString() + " weight = ${weight}" }
}

class ConvolutionalNeuron extends Neuron
{
	float weight;

	ConvolutionalNeuron(ANN network, ActivationFunction func, int x, y, z) {
		super(network, x, y, z)
		weight = getStartWeight()
	}

	public void run() {
		double newLevel = 0;
		int z1 = z - 1;
		int xSize = network.xSize;
		int ySize = network.ySize;
		int xOffset, yOffset;
		if (z1 < 0) {
			xOffset = x * network.inputNeuronFactor
			yOffset = y * network.inputNeuronFactor
		}
		else {
			xOffset = yOffset = 0
		}
		int xLimit = network.getLayerWidth(z1)
		int yLimit = network.getLayerHeight(z1)
		xSize.times( { itx ->
			ySize.times( { ity ->
				newLevel += network.get((itx + xOffset) % xLimit, (ity + yOffset) % yLimit, z1).level * weight
			})
		})
		newLevel += getLevel() * internalWeight
		level = activationFunction.activation(newLevel);
		if (x == 15 && y == 12) println ("************* CNN new level == ${level} " + this)
	}

	void internal_feedback(double error) {
		double ourError = error / network.zSize
		weight += activationFunction.derivative(ourError) * network.learningRate
		//if (x == 15 && y == 12) println ("z == ${z} Our error == ${ourError} " + this)

		internalWeight += ourError * network.learningRate
		if (internalWeight > 2.0) internalWeight = network.levelPrecision
		else if (internalWeight < 0.0) internalWeight = 0.0

		network.get(x, y, z - 1).internal_feedback(error)
	}

	public String toString() { return super.toString() + " weight = ${weight}" }
}


class OutputNeuron extends MiddleNeuron
{
	OutputNeuron(ANN network, ActivationFunction func, int x, y) {
		super(network, func, x, y, network.zSize - 1)
	}

	double feedback(int target) {
		//if (x == 15 && y == 12) println (" ----- Target ${target} error diff " + (target - getOutput()))
		double ediff = (target - getOutput()) * 2.0 / (double)network.levelPrecision
		double error = ediff //(ediff * ediff)/(double)2
		// if ((target - getOutput()) < 0) error = -error

		this.internal_feedback(error)
		return error
	}

	int getOutput() { return getLevel() * network.levelPrecision }
}

class InputNeuron extends Neuron
{
	private int inputValue
	private float weight = getStartWeight()

	InputNeuron(ANN network, ActivationFunction func, int x, y) {
		super(network, func, x, y, -1)
	}

	void setInput(int value) { inputValue = value }

	public void run() {
		double newLevel = inputValue * weight
		level = activationFunction.activation(newLevel);
		if (x == (15 * 3) && y == 12) println (" ----- Input ${inputValue} weight ${weight} level ${level} ")
		if (x == (15 * 3 + 1) && y == 12) println (" ----- Input ${inputValue} weight ${weight} level ${level} ")
		if (x == (15 * 3 + 2) && y == 12) println (" ----- Input ${inputValue} weight ${weight} level ${level} ")
	}

	void internal_feedback(double error) {
		double ourError = error / network.zSize
		weight += activationFunction.derivative(ourError) * network.learningRate
	}

	public String toString() { return super.toString() + " input = ${inputValue}" }
}

class ImageClassification
{
	private inputImages = []
	private int width = 160
	private int height = 100
	private int midWidth
	private int midHeight

	private BufferedImage getScaledInstance(image) {
		int type = ( image.getTransparency() == BufferedImage.OPAQUE ) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB
		int w = image.width
		int h = image.height
		int nw = width
		int nh = height

		while( true ) {
			if( w > nw ) {
				w /= 2
				if( w < nw ) {
					w = nw
				}
			} else if( w < nw ) {
				w *= 2
				if( w > nw ) {
					w = nw
				}
			}

			if( h > nh ) {
				h /= 2
				if( h < nh ) {
					h = nh
				}
			} else if( h < nh ) {
				h *= 2
				if( h > nh ) {
					h = nh
				}
			}

			image = new BufferedImage( w, h, type ).with { ni ->
				ni.createGraphics().with { g ->
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC )
					g.drawImage( image, 0, 0, w, h, null )
					g.dispose()
					ni
				}
			}
			if( w == nw || h == nh ) {
				return image
			}
		}
	}

	private void readImage(String fileName) throws IOException {
		BufferedImage img = null
		img = getScaledInstance(ImageIO.read(new File(fileName)))
		inputImages.add(img)
	}

	int[][] getPixelData(BufferedImage image) {
		int w = image.getWidth()
		int h = image.getHeight()
		int[][] data = new int[w * 3][h]
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int rgb = image.getRGB(x, y)
				data[x * 3][y] = rgb & 0xff
				data[x * 3 + 1][y] = (rgb & 0xff00) >> 8
				data[x * 3 + 2][y] = (rgb & 0xff0000) >> 16
			}
		}
		return data
	}

	ImageClassification() {
		midWidth = width / 4
		midHeight = height / 4
		teach()
	}

	void teach() {
		ANN network = new ANN(midWidth, midHeight, 3)
		readImage("/home/svlu/Pictures/speed_60_blue_sky_rural.jpg")
		int output_60_x = (int)(midWidth * 3 / 4)
		int output_60_y = (int)(midHeight / 2)
		int output_60 =  output_60_y * midWidth + output_60_x
		readImage("/home/svlu/Pictures/speed_90_trafic_blue_sky_rural.jpg")
		int output_90_x = (int)(midWidth * 3 / 4 + 5)
		int output_90_y = (int)(midHeight / 2)
		int output_90 = output_90_y * midWidth + output_90_x
		sleep(3000)
		int[] target = new int[3 * width / 4 * height / 4]
		int runCount = 1
		double lr = 1.0
		double lrDelta = 0.6/500
		target.length.times { target[it] = 0 }
		50.times {
			network.setLearningRate(lr)
			lr -= lrDelta
			target[output_60] = 60
			target[output_90] = 0
			10.times {
				network.setInput(getPixelData(inputImages[0]))
				network.waitForLayer(2, runCount)
				network.feedback(target)
				runCount++
			}

			target[output_60] = 0
			target[output_90] = 90
			10.times {
				network.setInput(getPixelData(inputImages[1]))
				network.waitForLayer(2, runCount)
				network.feedback(target)
				runCount++
			}
			println("90 level: " + network.get(output_90_x, output_90_y, 2).getLevel())
			println("60 level: " + network.get(output_60_x, output_60_y, 2).getLevel())
			println("Learning rate: " + lr + " run count " + runCount)
		}
	}
}

new ImageClassification()
