package routing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import core.Settings;
import weka.classifiers.Classifier;
import weka.classifiers.functions.IWDCOClassifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Normalize;

import static core.SimScenario.GROUP_NS;
import static core.SimScenario.ROUTER_S;

public class MLTrainer{

	private static final String IWDCO_NS = "IWDCO";

	private static final String ITERATIONS = "iterations";

	private static final String NUM_IWDS = "numIWDs";

	private static final String MUTATIONS = "mutations";

	private static final String WEIGHT_RANGE = "weightRange";

	private static final String PRECISION = "precision";

	public static FilteredClassifier model;
	public static Instances data;
	
	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;

		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}

		return inputReader;
	}


	public static void train_data() throws Exception {
		BufferedReader datafile = readDataFile("C:\\Users\\smriti srivastava\\Desktop\\IWD\\Data_Final_10.arff");
		data = new Instances(datafile);
		data.setClassIndex(data.numAttributes() - 1);

		Settings iwdSettings = new Settings(IWDCO_NS);
		int iterations = iwdSettings.getInt(ITERATIONS, 10);
		int numIWDs = iwdSettings.getInt(NUM_IWDS, 40);
		int mutations = iwdSettings.getInt(MUTATIONS, 500);
		int precision = iwdSettings.getInt(PRECISION, 5);
		int weightRanges[] = iwdSettings.getCsvInts(WEIGHT_RANGE, 2);

		Normalize normalize = new Normalize();

		model = new FilteredClassifier();
		model.setFilter(normalize);
		model.setSeed(12345);

		if ((new Settings(GROUP_NS).getSetting(ROUTER_S)).equals("IWDMLRouter")) {
			IWDCOClassifier classifier = new IWDCOClassifier();//set the classifier
			classifier.setNumIWDs(numIWDs);
			classifier.setMutations(mutations);
			classifier.setNumIterations(iterations);
			classifier.setMinWeight(weightRanges[0]);
			classifier.setMaxWeight(weightRanges[1]);
			classifier.setPrecision(precision);
			classifier.setNumNodesInHiddenLayer(5);

			model.setClassifier(classifier);
		} else {
			MultilayerPerceptron classifier = new MultilayerPerceptron();
			classifier.setNormalizeAttributes(false);
			model.setClassifier(classifier);
		}

		Instances training_data = new Instances(data, 0, data.numInstances()-1);
		model.buildClassifier(training_data);


		System.out.println("Classifier has been built");

	}
	public static Double get_delivery_prob(Instance instance)
	{
		
		try {
			instance.setDataset(data);
			double[] class1=model.distributionForInstance(instance);
			return class1[1];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
	
}
