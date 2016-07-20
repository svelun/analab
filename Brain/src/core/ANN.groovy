/**
 * 
 */
package core

import java.awt.image.BufferedImage;
import java.io.IOException
import java.util.Random;
import java.awt.image.ImagingOpException
import java.lang.ProcessEnvironment.Value;
import java.nio.channels.NetworkChannel
import java.util.zip.ZStreamRef
import javax.imageio.ImageIO
import javax.swing.Spring.WidthSpring
import javax.swing.text.DefaultEditorKit.PreviousWordAction

import java.awt.Adjustable;
import java.awt.RenderingHints


class NeuronLayer
{
	private ANNetwork network;
	private NeuronLayer previousLevel;
	private NeuronLayer nextLevel;
	private int zIdx;
	private int xSize;
	private int ySize;
	private Neuron[][] neurons;
	private NeuronExecuter executor;

	/**
	 * @param network
	 * @param previousLevel
	 * @param zIdx
	 * @param xSize
	 * @param ySize
	 */
	public NeuronLayer(ANNetwork network, NeuronLayer previousLevel, int zIdx, int xSize, int ySize) {
		super();
		this.network = network;
		this.previousLevel = previousLevel;
		this.zIdx = zIdx;
		this.xSize = xSize;
		this.ySize = ySize;
		if(previousLevel != null) previousLevel.setNextLevel(this);
		neurons = new Neuron[xSize][ySize];
		executor = new NeuronExecuter(this, zIdx);
	}

	void addNeuron(Neuron n, int x, int y) {
		neurons[x][y] = n;
		n.setLayer(this);
	}

	public Neuron getNeuron(int x, int y) {
		return neurons[x][y];
	}

	/**
	 * @return the network
	 */
	public ANNetwork getNetwork() {
		return network;
	}
	/**
	 * @return the nextLevel
	 */
	public NeuronLayer getNextLevel() {
		return nextLevel;
	}
	/**
	 * @param nextLevel the nextLevel to set
	 */
	private void setNextLevel(NeuronLayer nextLevel) {
		this.nextLevel = nextLevel;
	}
	/**
	 * @return the previousLevel
	 */
	public NeuronLayer getPreviousLevel() {
		return previousLevel;
	}
	/**
	 * @return the zIdx
	 */
	public int getzIdx() {
		return zIdx;
	}
	/**
	 * @return the xSize
	 */
	public int getxSize() {
		return xSize;
	}
	/**
	 * @return the ySize
	 */
	public int getySize() {
		return ySize;
	}
	
	public void waitForLayer(int runCount) {
		executor.waitForRun(runCount);
	}
	public int getRunCount() {
		executor.getRunCount();
	}
	
	public void newInput() { executor.newInput(); }
}

class OutputLayer extends NeuronLayer
{
	public OutputLayer(ANNetwork network, NeuronLayer previousLayer, int xSize, int ySize) {
		super(network, previousLayer, previousLayer.getzIdx(), xSize, ySize);
		ActivationFunc func = new Tanh();
		ySize.times { yIdx ->
			xSize.times { xIdx ->
				OutputNeuron n = new OutputNeuron(this, func, xIdx, yIdx);
				addNeuron(n, xIdx, yIdx);
			}
		}
	}
}

/**
 * @author svlu
 *
 */
class ANNetwork {
	int levelPrecision = 255
	int xSize = 3 * 160
	int ySize = 100
	int zSize = 3
	int inputNeuronFactor = 8
	double learningRate = 0.005
	float weightLimit = 3.0


	private NeuronLayer inputLayer;
	private NeuronLayer lastLayer;
	private NeuronLayer outputLayer;
	private int inputCount = 0
	private long maxRunningTime = 10

	private long t1 = System.currentTimeMillis()

	boolean isInitialized = false

	private void init() {
		// Create the Neuron network

		ActivationFunc func = new Tanh();

		// Create the input layer
		int inXSize = xSize * inputNeuronFactor
		int inYSize = ySize * inputNeuronFactor
		println("inXSize " + inXSize + " inYSize " + inYSize + " xSize " + xSize + " ySize " + ySize + " factor " + inputNeuronFactor);
		inputLayer = new NeuronLayer(this, (NeuronLayer)null, -1, inXSize, inYSize);
		inXSize.times ({ x ->
			inYSize.times ({ y -> inputLayer.addNeuron(new InputNeuron(inputLayer, func, x, y), x, y); } )
		})

		NeuronLayer layer = inputLayer;
		zSize.times ({ z ->
			int xs;
			int ys;
			if (z > 0) {
				xs = xSize;
				ys = ySize;
			} else {
				xs = inXSize / 2;
				ys = inYSize / 2;
			}
			layer = new NeuronLayer(this, layer, z, xs, ys);
			xs.times({ x ->
				ys.times({ y ->
					Neuron n
					if (z == 0) {
						if( x == 0 && y == 0)
							n = new ConvolutionalNeuron(layer, func, 15, 5);
						else
							n = new ConvolutionalNeuron(layer, func, x, y, 15, 5);
						
					}
					else {
						n = new MiddleNeuron(layer, func, x, y, 15, 5)
					}
					layer.addNeuron(n, x, y);
				})
				lastLayer = layer;
			})
		})
		println ("Used time for network creation: " + (System.currentTimeMillis() - t1))
		Runtime rt = Runtime.getRuntime()
		long totalMemory = rt.totalMemory()
		println ("Memory total: ${totalMemory}  used: " + (totalMemory - rt.freeMemory()))
		synchronized (this) {
			isInitialized = true
			notifyAll()
		}
	}

	public ANNetwork(int width, int height, int depth) {
		xSize = width
		ySize = height
		zSize = depth
		init()
	}

	public ANNetwork() {
		init()
	}
	
	public OutputLayer createOutputLayer(int outXSize, int outYSize) {
		outputLayer = new OutputLayer(this, lastLayer, outXSize, outYSize);
		lastLayer = outputLayer;
		return outputLayer;
	}

	public NeuronLayer getOutputLayer() { return outputLayer; }
	
	void setLearningRate(double value) { learningRate = value }

	synchronized void setMaxRunningTime(long value) {
		if (value > maxRunningTime) maxRunningTime = value
	}

	long getMaxRunningTime() { return maxRunningTime }

	int setInput(int [][] input) {
		int rc;
		println("input runCount " + inputCount)
		inputLayer.waitForLayer(inputCount);
		inputLayer.getxSize().times { x ->
			inputLayer.getySize().times { y ->
				inputLayer.getNeuron(x, y).setInput(input[x][y])
			}
		}
		synchronized (this) {
			inputCount++
			println("new inputCount == " + inputCount)
			this.notifyAll()
			rc = inputCount;
		}
		inputLayer.newInput();
		Thread.yield()
		return rc
	}

	void feedback(int[] target) {
		double Etotal = 0
		int i = 0
		NeuronLayer layer = getOutputLayer();
		layer.getySize().times { yIdx ->
			layer.getxSize().times { xIdx ->
				OutputNeuron n = (OutputNeuron)layer.getNeuron(xIdx, yIdx);
				Etotal += ((target[i] - n.getOutput())^2)/2
				n.feedback(target[i])
				i++
			}
		}
		println ("Total error: " + Etotal)
	}
}

class NeuronExecuter extends Thread
{
	private int runCount = 0;
	private int fromY;
	private int toY;
	private NeuronLayer layer;
	private NeuronExecuter next;
	private boolean hasNewInput;
	
	NeuronExecuter(NeuronLayer layer, int zRow) {
		super("Neuron Executer z index: " + zRow);
		this.layer = layer;
		fromY = 0;
		if (zRow >= 0 && layer.getySize() > 16) {
			toY = layer.getySize() / 8;
			next = new NeuronExecuter(layer, toY, 2 * toY, zRow);
		}
		else {
			toY = layer.getySize();
			next = null;
		}
		start();
	}

	private NeuronExecuter(NeuronLayer layer, int fromY, int toY, int zRow) {
		super("Neuron Executer z index: " + zRow + " range ${fromY}:${toY}")
		this.layer = layer;
		this.fromY = fromY
		this.toY = toY
		if (toY < layer.getySize()) {
			next = new NeuronExecuter(layer, toY, toY + toY - fromY, zRow);
		} else {
			next = null
			if (toY > layer.getySize()) this.toY = layer.getySize();
		}
		start()
	}

	void waitForRun(int runCount) {
		synchronized (this) {
			while(this.runCount != runCount) {
				wait()
			}
		}
	}

	synchronized int getRunCount() {
		return runCount
	}

	synchronized void newInput() {
		hasNewInput = true;
		notifyAll();
	}
	
	public void run() {

		synchronized (layer.getNetwork()) {
			try {
				while(!layer.getNetwork().isInitialized) { layer.getNetwork().wait() }
			} catch (Exception e) {
				e.printStackTrace()
			}
		}

		long ta = 0
		int rc = 0
		NeuronLayer prevLayer = layer.getPreviousLevel();
		while (true) {
			rc++
			if(prevLayer != null) prevLayer.waitForLayer(runCount + 1)
			else {
				synchronized (this) {
					while(!hasNewInput) wait();
					hasNewInput = false;
				}
			}
			long t1 = System.currentTimeMillis()
			for(int x = 0; x < layer.getxSize(); x++) {
				for(int y = fromY; y < toY; y++) {
					layer.getNeuron(x, y).run();
				}
			}
			long runningTime = System.currentTimeMillis() - t1
			ta += runningTime
			ta /= 2
			if (ta > layer.getNetwork().getMaxRunningTime()) {
				println("${layer.getzIdx()} running time " + runningTime + " avarage " + ta)
				layer.getNetwork().setMaxRunningTime(ta)
			}
			if(next != null) next.waitForRun(runCount + 1);
			synchronized (this) {
				this.runCount++
				notifyAll()
			}
		}
	}
}

/**
 * @author svlu
 *
 */
abstract class Neuron implements Runnable {
	protected NeuronLayer parentLayer;

	static Random rand = new Random()
	protected float level = 0;
	protected float bias = 1.0;
	protected float beta1 = 0.0;

	protected float inputLevel = 0;
	int x, y
	ANNetwork network
	protected ActivationFunc activationFunction;

	Neuron(NeuronLayer layer, ActivationFunc func, int x, y) {
		this.network = layer.getNetwork();
		activationFunction = func;
		this.x = x
		this.y = y
	}

	protected float getStartWeight() { return (float)(rand.nextDouble() * network.weightLimit / 3) }

	void setLayer(NeuronLayer layer) {
		parentLayer = layer;
	}

	float getLevel() { return level }

	void internal_feedback(double error) {
		throw new Error("This method should be overridden")
	}

	public String toString() { return "level = ${level}" }
}

class MiddleNeuron extends Neuron
{
	protected float[] weight;
	//int[] historyInput = new int[9]
	int actualConnections = -1
	protected int connectionWidth;
	protected int connectionHeight;
	
	MiddleNeuron(NeuronLayer layer, ActivationFunc func, int x, y, int connectionWidth, connectionHeight) {
		super(layer, func, x, y);
		this.connectionWidth = connectionWidth;
		this.connectionHeight = connectionHeight;
        weight = new float[connectionWidth * connectionHeight];
 		weight.size().times({ weight[it] = getStartWeight() })
		bias = 10.0/weight.size();
	}

	MiddleNeuron(NeuronLayer layer, float[] weights, ActivationFunc func, int x, y, int connectionWidth, connectionHeight) {
		super(layer, func, x, y);
		this.connectionWidth = connectionWidth;
		this.connectionHeight = connectionHeight;
        weight = weights;
		bias = 10.0/weights.size();
	}

	public void run() {
		inputLevel = 0
		int count = connectionWidth * connectionHeight;
		NeuronLayer pl = parentLayer.getPreviousLevel();
		int xSize = pl.getxSize();
		int ySize = pl.getySize();
		int xStep;
		int yStep;
		if(xSize > parentLayer.getxSize()) xStep = x * xSize / parentLayer.getxSize();
		else xStep = 0
		if(ySize > parentLayer.getySize()) yStep = y * ySize / parentLayer.getxSize();
		else yStep = 0
		connectionWidth.times( { itx ->
			int x1 = ((int)(xStep + (x + itx - connectionWidth/2))) % xSize
			connectionHeight.times( { ity ->
				int y1 = ((int)(yStep + (y + ity - connectionHeight/2))) % ySize
				int idx = itx * connectionHeight + ity
				inputLevel += pl.getNeuron(x1, y1).level * weight[idx]
			})
		})
		inputLevel *= bias;
		actualConnections = count + 1
		level = activationFunction.activation(inputLevel)
		//if (x == 15 && y == 12) println ("############ z == ${z} New Level! " + this)
	}

	void internal_feedback(double error) {
		double ourError = error  / network.zSize
		double connectionAdjustment = ourError * activationFunction.derivative(inputLevel) * network.learningRate
		float weightLimit = network.weightLimit
		//if (x == 15 && y == 12) println ("z == ${z} Error == ${error} Our error == ${ourError} connectionAdjustment == ${connectionAdjustment}" + this)
		int xSize = network.xSize
		int ySize = network.ySize
		connectionWidth.times( { itx ->
			// int x1 = ((int)(x + itx - connectionWidth/2)) % xSize
			connectionHeight.times( { ity ->
				// int y1 = ((int)(y + ity - connectionHeight/2)) % ySize
				int idx = itx * connectionHeight + ity
				weight[idx] +=  connectionAdjustment
				if (weight[idx] > weightLimit) weight[idx] = weightLimit
				else if (weight[idx] < 0.0) weight[idx] = 0.0
			})
		})

		parentLayer.getPreviousLevel().getNeuron(x, y).internal_feedback(error)
	}

	public String toString() { return super.toString() + " weight = ${weight}" }
}

class ConvolutionalNeuron extends MiddleNeuron
{
	ConvolutionalNeuron(NeuronLayer layer, ActivationFunc func, int x, y, int connectionWidth, connectionHeight) {
		super(layer, ((ConvolutionalNeuron)layer.getNeuron(0, 0)).weight, func, x, y, connectionWidth, connectionHeight);
	}
	
	ConvolutionalNeuron(NeuronLayer layer, ActivationFunc func, int connectionWidth, connectionHeight) {
		super(layer, func, 0, 0, connectionWidth, connectionHeight);
	}
}

class OutputNeuron extends MiddleNeuron
{
	OutputNeuron(NeuronLayer layer, ActivationFunc func, int x, y) {
		super(layer, func, x, y, layer.getPreviousLevel().getxSize(), layer.getPreviousLevel().getySize())
	}

	double feedback(int target) {
		//if (x == 15 && y == 12) println (" ----- Target ${target} error diff " + (target - getOutput()))
		double ediff = (target - getOutput()) / (double)network.levelPrecision
		double error = (ediff * ediff)/(double)2
		if (ediff < 0) error = -error

		this.internal_feedback(error)
		return error
	}

	int getOutput() { return getLevel() * network.levelPrecision }
}

class InputNeuron extends Neuron
{
	private float inputValue
	private float weight = getStartWeight()

	InputNeuron(NeuronLayer layer, ActivationFunc func, int x, y) {
		super(layer, func, x, y)
	}

	void setInput(int value) { inputValue = value / (float)255; }

	public void run() {
		inputLevel = inputValue * weight
		//if (x == (15 * 3) && y == 12) println("input " + inputLevel + " tanh " + activationFunction.activation(inputLevel))
		level = activationFunction.activation(inputLevel);
		if (x == (15 * 3) && y == 12) println (" ----- Input ${inputValue} weight ${weight} level ${level} ")
		if (x == (15 * 3 + 1) && y == 12) println (" ----- Input ${inputValue} weight ${weight} level ${level} ")
		if (x == (15 * 3 + 2) && y == 12) println (" ----- Input ${inputValue} weight ${weight} level ${level} ")
	}

	void internal_feedback(double error) {
		double ourError = error / network.zSize
		weight += ourError * activationFunction.derivative(inputLevel) * network.learningRate
	}

	public String toString() { return super.toString() + " input = ${inputValue}" }
}

/**
 * @author svlu
 *
 */
public interface ActivationFunc {
	public double activation(double netSum);
	public double derivative(double value);
}

/**
 * @author svlu
 *
 */
class Tanh implements ActivationFunc {
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
		double f = activation(value);
		return 1 - (f * f);
	}

}

class ImageClassification
{
	private inputImages = []
	private int width = 320
	private int height = 200
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
		midWidth = width * 3 / 8
		midHeight = height / 8
		teach()
	}

	void teach() {
		ANNetwork network = new ANNetwork(midWidth, midHeight, 4);
		OutputLayer outLayer = network.createOutputLayer(2, 1);
		
		int[][] justRed = new int[width * 3][height];
		int[][] justGreen = new int[width * 3][height];
		int[][] justBlue = new int[width * 3][height];
		for(int x = 0; x < width * 3; x += 3) {
			for(int y = 0; y < height; y++) {
				justRed[x][y] = 250;
				justGreen[x][y] = justBlue[x][y] = 0
				justGreen[x + 1][y] = 250;
				justRed[x + 1][y] = justBlue[x + 1][y] = 0
				justBlue[x + 2][y] = 250;
				justRed[x + 2][y] = justGreen[x + 2][y] = 0
			}
		}
		readImage("/home/svlu/Pictures/speed_60_blue_sky_rural.jpg")
		int output_60_x = 0
		int output_60_y = 0
		int output_60 =  0
		readImage("/home/svlu/Pictures/speed_90_trafic_blue_sky_rural.jpg")
		int output_90_x = 1
		int output_90_y = 0
		int output_90 = 1
		sleep(1000)
		int[] target = new int[2]
		int runCount
		double lr = 0.008
		double lrDelta = 0.006/500
		target.length.times { target[it] = 0 }
		500.times {
			network.setLearningRate(lr)
			lr -= lrDelta
			target[output_60] = 0;
			target[output_90] = 0;
			3.times {
				runCount = network.setInput(justRed);
				println(" waiting for runCount " + runCount);
				outLayer.waitForLayer(runCount);
				network.feedback(target)
				println("90 goal 0, level: " + network.getOutputLayer().getNeuron(output_90_x, output_90_y).getLevel())
				println("60 goal 0, level: " + network.getOutputLayer().getNeuron(output_60_x, output_60_y).getLevel())
			}
			target[output_60] = 255
			target[output_90] = 255
			10.times {
				runCount = network.setInput(getPixelData(inputImages[0]))
				outLayer.waitForLayer(runCount);
				network.feedback(target)
				println("90 goal 1, level: " + network.getOutputLayer().getNeuron(output_90_x, output_90_y).getLevel())
				println("60 goal 1, level: " + network.getOutputLayer().getNeuron(output_60_x, output_60_y).getLevel())
			}
			target[output_60] = 0
			target[output_90] = 0
			3.times {
				runCount = network.setInput(justGreen)
				outLayer.waitForLayer(runCount);
				network.feedback(target)
				println("90 goal 0, level: " + network.getOutputLayer().getNeuron(output_90_x, output_90_y).getLevel())
				println("60 goal 0, level: " + network.getOutputLayer().getNeuron(output_60_x, output_60_y).getLevel())
			}

			target[output_60] = 255
			target[output_90] = 255
			10.times {
				runCount = network.setInput(getPixelData(inputImages[1]))
				outLayer.waitForLayer(runCount);
				network.feedback(target)
				println("90 goal 1, level: " + network.getOutputLayer().getNeuron(output_90_x, output_90_y).getLevel())
				println("60 goal 1, level: " + network.getOutputLayer().getNeuron(output_60_x, output_60_y).getLevel())
			}
			target[output_60] = 0
			target[output_90] = 0
			3.times {
				runCount = network.setInput(justBlue)
				outLayer.waitForLayer(runCount);
				network.feedback(target)
				println("90 goal 0, level: " + network.getOutputLayer().getNeuron(output_90_x, output_90_y).getLevel())
				println("60 goal 0, level: " + network.getOutputLayer().getNeuron(output_60_x, output_60_y).getLevel())
			}
			println("Learning rate: " + lr + " run count " + runCount)
		}
	}
}

new ImageClassification()
