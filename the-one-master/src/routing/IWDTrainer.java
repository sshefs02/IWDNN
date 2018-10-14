/**
 * 
 */
package routing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import weka.classifiers.functions.IWDClassifier;
import weka.core.Instance;
import weka.core.Instances;

public class IWDTrainer {

	public static IWDClassifier model;
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
		BufferedReader datafile = readDataFile("C:\\Users\\smriti srivastava\\Documents\\data25.arff");
		data = new Instances(datafile);
		data.setClassIndex(data.numAttributes() - 1);
		
		model=new IWDClassifier();//set the classifier
		model.setNumEpochs(1);
		model.setNumIterations(1);
		model.setNumIWDs(801);
		Instances training_data = new Instances(data, 0, data.numInstances()-1);
		model.buildClassifier(training_data);

		System.out.println("Classifier has been built YAY!!!!");

	}
	
	public static Double get_delivery_prob(Instance instance)
	{
		
		try {
			instance.setDataset(data);
			double[] class1=model.distributionForInstance(instance);
			System.out.println("dp--"+class1[0]+"---"+class1[1]);
			return class1[1];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
}
