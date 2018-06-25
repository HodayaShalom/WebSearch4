import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.TextDirectoryLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Reorder;
import weka.filters.unsupervised.attribute.StringToWordVector;

/*
 * TwitterApplication implements the entire flow of the experiments
 * we performed for this project.
 */

public class TwitterApplication {
	
	// config params:
	String jwnlFilePath;
	String openNlpModelsPath;
	String trainFilePath;
	String testFilePath;
	String trainFolder;
	String testFolder;
	String dataMode;
	boolean includeSubjectNerAsFeature;
	String emoticonsFilePath;
	String imdbFolder;
	String outputFile;
	HashSet<String> emoticons = new HashSet<String>();
	
	String featuresSetStr;
	
	OpenNLPRunner openNlp;
	
	public TwitterApplication(String configFile) throws Exception {
		loadConfig(configFile);
	
		BufferedReader in = new BufferedReader(new FileReader(emoticonsFilePath));
		String line = in.readLine();
		while (line != null) {
			emoticons.add(line);
			line = in.readLine();
		}
		in.close();
		
		openNlp = new OpenNLPRunner(openNlpModelsPath, jwnlFilePath);
		
		Vector<Entry> data = null;
		if (dataMode.equals("IMDB")){
			data = readIMDBContent(imdbFolder);
		} else if (dataMode.equals("TWITTER")) {
			data = readTwitterContent(trainFilePath);
		}
		Vector<Integer> featuresSets = getFeaturesSets();
		Vector<CVPair> cvpairs = buildCV(data, 10);
		
		PrintWriter out = new PrintWriter(new FileWriter(outputFile));
		Vector<String> options = new Vector<String>();
		options.add("");	// for FP weighting function
		options.add("-T");	// for FF weighting function
		options.add("-I -T");	// for TFIDF weighting function
		for (int tt = 0; tt < options.size(); ++tt) {
			out.println("WEIGHTING FUNCTION: " + options.get(tt));
			out.println("+++++++++++++++++++++++++++++++++");
			for (int f = 0; f < featuresSets.size(); ++f) {
				calculateFeatures(data, featuresSets.get(f).intValue());
				
				Vector<Stat> averageStats = new Vector<Stat>();
				
				for (int i = 0; i < cvpairs.size(); ++i) {
					Vector<Stat> stats = runTrainAndTest(cvpairs.get(i).train, cvpairs.get(i).test, options.get(tt));
					for (int j = 0; j < stats.size(); ++j) {
						if (i == 0)
							averageStats.add(stats.get(j));
						else
							averageStats.get(j).applyToAverage(stats.get(j), i + 1);
					}
				}
				out.println("Feature set " + featuresSets.get(f).intValue());
				out.println("=======================");
				for (int j = 0; j < averageStats.size(); ++j) {
					out.println(averageStats.get(j).toString());
				}
				out.println("");
				out.flush();
			}
		}
		
		out.close();
	}
	
	Vector<Integer> getFeaturesSets() {
		Vector<Integer> ret = new Vector<Integer>();
		String []tokens = featuresSetStr.split("[,]");
		for (int i = 0; i < tokens.length; ++i) {
			int t = Integer.parseInt(tokens[i].trim());
			ret.add(new Integer(t));
		}
		return ret;
	}
	
	/*
	 * running and testing all the classifiers on a specific split from the 
	 * cross validation set.
	 * stringToWordOptions determines the weighting function to use.
	 */
	Vector<Stat> runTrainAndTest(Vector<Entry> train, Vector<Entry> test, String stringToWordOptions) throws Exception {
		createDataFolder(train, trainFolder);
		
		TextDirectoryLoader loader = new TextDirectoryLoader();
	    loader.setDirectory(new File(trainFolder));
	    Instances dataRaw = loader.getDataSet();
	    StringToWordVector filter = new StringToWordVector();
	    filter.setOptions(weka.core.Utils.splitOptions(stringToWordOptions));
	    filter.setInputFormat(dataRaw);
	    Instances trainFiltered = Filter.useFilter(dataRaw, filter);
	    Reorder reorder = new Reorder();
		reorder.setOptions(weka.core.Utils.splitOptions("-R 2-last,first"));
		reorder.setInputFormat(trainFiltered);
		trainFiltered = Filter.useFilter(trainFiltered, reorder);
		
		createDataFolder(test, testFolder);
		loader.setDirectory(new File(testFolder));
	    dataRaw = loader.getDataSet();
	    //filter.setInputFormat(dataRaw);
	    Instances testFiltered = Filter.useFilter(dataRaw, filter);
	    reorder.setInputFormat(testFiltered);
		testFiltered = Filter.useFilter(testFiltered, reorder);
		
		Vector<Stat> stats = new Vector<Stat>();
		
		/// NAIVE BAYES - multinomial
		Classifier classifier = new NaiveBayesMultinomial();
		String []options = weka.core.Utils.splitOptions("");
		//classifier.setOptions(options);
		Stat stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "NaiveBayes";
		stats.add(stat);
		
		/// SVM with polynimial degree 2 kernel
		classifier = new LibSVM();
		options = weka.core.Utils.splitOptions("-K 1 -D 2");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "LibSVM-POLY2";
		stats.add(stat);
		
		/// SVM with linear kernel
		classifier = new LibSVM();
		options = weka.core.Utils.splitOptions("-K 0");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "LibSVM-LIN";
		stats.add(stat);
		
		/// kNN - no normalization (k = 1)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 1 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(1)";
		stats.add(stat);
		
		/// kNN - no normalization (k = 3)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 3 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(3)";
		stats.add(stat);
		
		/// kNN - no normalization (k = 5)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 5 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(5)";
		stats.add(stat);
		
		/// kNN - no normalization (k = 10)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 10 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(10)";
		stats.add(stat);
		
		/// kNN - no normalization (k = 20)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 20 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(20)";
		stats.add(stat);
		
		/// kNN - no normalization (k = 30)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 30 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(30)";
		stats.add(stat);
		
		/// kNN - no normalization (k = 50)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 50 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(50)";
		stats.add(stat);
		
		/// kNN - with normalization (k = 1)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 1 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(1)Norm";
		stats.add(stat);
		
		/// kNN - with normalization (k = 3)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 3 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(3)Norm";
		stats.add(stat);
		
		/// kNN - with normalization (k = 5)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 5 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(5)Norm";
		stats.add(stat);
		
		/// kNN - with normalization (k = 10)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 10 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(10)Norm";
		stats.add(stat);
		
		/// kNN - with normalization (k = 20)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 20 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(20)Norm";
		stats.add(stat);
		
		/// kNN - with normalization (k = 30)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 30 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(30)Norm";
		stats.add(stat);
		
		/// kNN - with normalization (k = 50)
		classifier = new IBk();
		options = weka.core.Utils.splitOptions("-K 50 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
		classifier.setOptions(options);
		stat = testClassifier(classifier, trainFiltered, testFiltered);
		stat.classifierName = "KNN(50)Norm";
		stats.add(stat);
		
		return stats;
	}
	
	/*
	 * build a model and test a specific classifier on a given testset.
	 * returns the accuracy running on the test and train sets.
	 */
	Stat testClassifier(Classifier classifier, Instances trainData, Instances testData) throws Exception {
		Stat stat = new Stat();
		classifier.buildClassifier(trainData);
		double accuracy = 0;
		for (int i = 0; i < testData.numInstances(); ++i) {
			Instance inst = testData.instance(i);
			double pred = classifier.classifyInstance(inst);
			if (inst.classValue() == pred) {
				accuracy++;
			} 
		}
		accuracy = accuracy / (double)testData.numInstances();
		stat.accuracyTest = accuracy;
		
		accuracy = 0;
		for (int i = 0; i < trainData.numInstances(); ++i) {
			Instance inst = trainData.instance(i);
			double pred = classifier.classifyInstance(inst);
			if (inst.classValue() == pred)
				accuracy++;
		}
		accuracy = accuracy / (double)trainData.numInstances();
		stat.accuracyTrain = accuracy;
		
		return stat;
	}
	
	/*
	 * creating a cross-validation set
	 */
	Vector<CVPair> buildCV(Vector<Entry> data, int fold) throws Exception {
		Vector<CVPair> pairs = new Vector<CVPair>();
		int amountInSet =  data.size() / fold;
		Vector<Integer> indices = new Vector<Integer>();
		for (int i = 0; i < data.size(); ++i)
			indices.add(new Integer(i));
		for (int i = 0; i < fold; ++i) {
			CVPair cvpair = new CVPair();
			pairs.add(cvpair);
			while (cvpair.test.size() < amountInSet) {
				int rand = (int)(Math.random() * indices.size());
				Entry entry = data.get(indices.get(rand).intValue());
				cvpair.test.add(entry);
				indices.remove(rand);
			}
		}
		
		for (int i = 0; i < pairs.size(); ++i) {
			CVPair p = pairs.get(i);
			for (int j = 0; j < pairs.size(); ++j) {
				if (j == i)
					continue;
				p.train.addAll(pairs.get(j).test);
			}
		}
		return pairs;
	}
	
	/*
	 * preparing a data folder to be be read by the StringToWords filter of WEKA
	 */
	void createDataFolder(Vector<Entry> data, String folder) throws IOException {
		File mainFolder = new File(folder);
		if (mainFolder.exists()) {
			removeDirectory(mainFolder);
			mainFolder.mkdir();
		}
		// create the train folder
		for (int i = 0; i < data.size(); ++i) {
			File classFolder = new File(folder + "/class" + data.get(i).label);
			if (!classFolder.exists())
				classFolder.mkdir();
			PrintWriter pw = new PrintWriter(folder + "/class" + data.get(i).label + "/twit" + i + ".txt");
			
			Iterator<String> iter = data.get(i).features.keySet().iterator();
			while (iter.hasNext()) {
				String word = iter.next();
				Integer num = data.get(i).features.get(word);
				for (int j = 0; j < num.intValue(); ++j)
					pw.print(word + " ");
			}
			pw.close();
		}
	}
	
	/*
	 * loading configuration parameters from the configuration file
	 */
	private void loadConfig(String configFile) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(configFile));
		String line = in.readLine();
		
		while (line != null) {
			// param=value
			
			String []tokens = line.split("[=]");
			if (tokens[0].equals("JWNL_FILE_PATH")) {
				jwnlFilePath = tokens[1];
			} else if (tokens[0].equals("OPEN_NLP_MODELS_FOLDER")) {
				openNlpModelsPath = tokens[1];
			} else if (tokens[0].equals("TRAIN_FILE_PATH")) {
				trainFilePath = tokens[1];
			} else if (tokens[0].equals("TEST_FILE_PATH")) {
				testFilePath = tokens[1];
			} else if (tokens[0].equals("FEATURES_SET")) {
				featuresSetStr = tokens[1];
			} else if (tokens[0].equals("TRAIN_FOLDER")) {
				trainFolder = tokens[1];
			} else if (tokens[0].equals("TEST_FOLDER")) {
				testFolder = tokens[1];
			} else if (tokens[0].equals("INCLUDE_SUBJECT_NER_AS_FEATURE")) {
				includeSubjectNerAsFeature = Boolean.parseBoolean(tokens[1]);
			} else if (tokens[0].equals("EMOTICONS_FILE_PATH")) {
				emoticonsFilePath = tokens[1];
			} else if (tokens[0].equals("DATA_MODE")) {
				dataMode = tokens[1];
			} else if (tokens[0].equals("IMDB_FOLDER")) {
				imdbFolder = tokens[1];
			} else if (tokens[0].equals("OUTPUT_FILE")) {
				outputFile = tokens[1];
			}
			line = in.readLine();
		}
		in.close();
	}
	
	/*
	 * reading IMDB data
	 */
	private Vector<Entry> readIMDBContent(String folderPath) throws Exception {
		Vector<Entry> out = new Vector<Entry>();
		
		File posFolder = new File(folderPath + "/pos");
		File negFolder = new File(folderPath + "/neg");
		File []posFiles = posFolder.listFiles();
		for (int i = 0; i < posFiles.length; ++i) {
			if (i % 10 == 0)
				System.out.println("" + i + " files out of " + posFiles.length + " positive files have been loaded");
			BufferedReader in = new BufferedReader(new FileReader(posFiles[i]));
			char []buf = new char[(int)posFiles[i].length()];
			in.read(buf);
			in.close();
			String text = new String(buf);
			Vector<Vector<String>> words = openNlp.processText(text, false, false);
			Entry entry = new Entry();
			entry.words = words;
			entry.label = 1;
			out.add(entry);
		}
		File []negFiles = negFolder.listFiles();
		for (int i = 0; i < negFiles.length; ++i) {
			if (i % 10 == 0)
				System.out.println("" + i + " files out of " + negFiles.length + " negative files have been loaded");
			BufferedReader in = new BufferedReader(new FileReader(negFiles[i]));
			char []buf = new char[(int)negFiles[i].length()];
			in.read(buf);
			in.close();
			String text = new String(buf);
			Vector<Vector<String>> words = openNlp.processText(text, false, false);
			Entry entry = new Entry();
			entry.words = words;
			entry.label = -1;
			out.add(entry);
		}
		return out;
	}
	
	/*
	 * reading Twitter data.
	 */
	private Vector<Entry> readTwitterContent(String dataFile) throws Exception {
		Vector<Entry> out = new Vector<Entry>();
		BufferedReader in = new BufferedReader(new FileReader(dataFile));
		int counter = 0;
		String line = in.readLine();
		int posCounter = 0;
		int negCounter = 0;
		while (line != null) {
			counter++;
			if (counter % 100 == 0)
				System.out.println("" + counter + " lines were loaded");
			String []tokens = line.split("[\t]");
			if (tokens[4].equals("Not Available")) {
				line = in.readLine();
				continue;
			}
			if (tokens[3].equals("\"objective\"") || tokens[3].equals("\"objective-OR-neutral\"") || tokens[3].equals("\"neutral\"")) {
				line = in.readLine();
				continue;
			}
			Entry entry = new Entry();
			if (tokens[3].equals("\"positive\""))
				entry.label = 1;
			else if (tokens[3].equals("\"negative\""))
				entry.label = -1;
			entry.id = tokens[0];
			if ((posCounter >= 730 && entry.label == 1) ||
					(negCounter >= 730 && entry.label == -1)) {
				line = in.readLine();
				continue;
			}
			if (entry.label == 1)
				posCounter++;
			else if (entry.label == -1)
				negCounter++;
			
			Vector<Vector<String>> words = openNlp.processText(tokens[4], true, true);
			Vector<Vector<String>> subjectWords = openNlp.processText(tokens[2], true, true);
			entry.words = words;
			int subjectIndex = 0;
			String subjectNerTag = "";
			for (int i = 0; i < words.size(); ++i) {
				String lemma = words.get(i).get(1);
				if (lemma.equals(subjectWords.get(subjectIndex).get(1))) {
					subjectIndex++;
					if (subjectIndex == subjectWords.size()) {
						// found
						subjectIndex = 0;
						int start = i - subjectWords.size() + 1;
						int end = i;
						entry.addSubjectIndices(start, end);
						
						// handling ner
						String ner = words.get(start).get(4);
						if (ner.startsWith("B-")) {
							if (subjectNerTag.length() == 0)
								subjectNerTag = ner;
						}
					}
				}
			}
			entry.subjectNerTag = subjectNerTag;
			out.add(entry);
			line = in.readLine();
		}
		in.close();
		return out;
	}
	
	/*
	 * adding feature to the "bag" of feature, with it's frequency
	 */
	void addFeature(HashMap<String, Integer> features, String feature) {
		Integer num = features.get(feature);
		if (num == null)
			features.put(feature, new Integer(1));
		else {
			features.put(feature, new Integer(num.intValue() + 1));
		}
	}
	
	/*
	 * claculates features for the dataset. 
	 * featuresSet is the setting we want to use this time.
	 */
	void calculateFeatures(Vector<Entry> entries, int featuresSet) throws Exception {
		for (int e = 0; e < entries.size(); ++e) {
			Entry entry = entries.get(e);
			HashMap<String, Integer> features = new HashMap<String, Integer>();
			
			if (featuresSet == 1) {
				// all lemmas (except the subject)
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i))
						addFeature(features, cleanWord(entry.words.get(i).get(1), ""));
				}
				
			} else if (featuresSet == 2) {
				// all lemmas (except the subject) with POS
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i))
						addFeature(features, cleanWord(entry.words.get(i).get(1), "__" + entry.words.get(i).get(2)));
				}
			} else if (featuresSet == 3) {
				// only VERB, ADJ, ADV lemmas (except the subject) with POS
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i) && 
							(entry.words.get(i).get(2).startsWith("JJ") || entry.words.get(i).get(2).startsWith("RB") || entry.words.get(i).get(2).startsWith("VB")))
						addFeature(features, cleanWord(entry.words.get(i).get(1), "__" + entry.words.get(i).get(2)));
				}
			} else if (featuresSet == 4) {
				// only VERB, ADJ, ADV lemmas (except the subject)
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i) && 
							(entry.words.get(i).get(2).startsWith("JJ") || entry.words.get(i).get(2).startsWith("RB") || entry.words.get(i).get(2).startsWith("VB")))
						addFeature(features, cleanWord(entry.words.get(i).get(1), ""));
				}
			} else if (featuresSet == 5) {
				// lemmas (except the subject) with location
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i)) {
						int window = entry.getWindowOfWordIndex(i);
						addFeature(features, cleanWord(entry.words.get(i).get(1), "__" + window));
					}
				}
			} else if (featuresSet == 6) {
				// only VERB, ADJ, ADV lemmas (except the subject) with location
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i) && 
							(entry.words.get(i).get(2).startsWith("JJ") || entry.words.get(i).get(2).startsWith("RB") || entry.words.get(i).get(2).startsWith("VB"))) {
						int window = entry.getWindowOfWordIndex(i);
						addFeature(features, cleanWord(entry.words.get(i).get(1), "__" + window));
					}
				}
			} else if (featuresSet == 7) {
				// only AD, ADV lemmas (except the subject) with location
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i) && 
							(entry.words.get(i).get(2).startsWith("JJ") || entry.words.get(i).get(2).startsWith("RB"))) {
						int window = entry.getWindowOfWordIndex(i);
						addFeature(features, cleanWord(entry.words.get(i).get(1), "__" + window));
					}
				}
			} else if (featuresSet == 8) {
				// lemmas (except subject) and POS
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i)) {
						addFeature(features, cleanWord(entry.words.get(i).get(1), ""));
						addFeature(features, cleanWord(entry.words.get(i).get(2), ""));
					}
				}
			} else if (featuresSet == 9) {
				// words (except the subject)
				for (int i = 0; i < entry.words.size(); ++i) {
					if (!entry.isSubjectWord(i)) {
						addFeature(features, cleanWord(entry.words.get(i).get(0), ""));
					}
				}
			} 
			entry.features = features;
			if (includeSubjectNerAsFeature && entry.subjectNerTag.length() > 0) {
				addFeature(entry.features, entry.subjectNerTag.toUpperCase());
			}
		}
	}
	
	/* 
	 * helper function to clean a word
	 */
	String cleanWord(String word, String suffix) {
		if (emoticons.contains(word))
			return word;
		StringBuffer newWord = new StringBuffer();
		for (int i = 0; i < word.length(); ++i) {
			if (Character.isLetter(word.charAt(i)))
				newWord.append(word.charAt(i));
		}
		if (newWord.toString().length() > 0)
			return newWord.toString() + suffix;
		return "";
	}
	
	static public boolean removeDirectory(File directory) {
		if (directory == null)
			return false;
		if (!directory.exists())
			return true;
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();

		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				File entry = new File(directory, list[i]);

				if (entry.isDirectory())
				{
					if (!removeDirectory(entry))
						return false;
				}
				else
				{
					if (!entry.delete())
						return false;
				}
			}
		}
		return directory.delete();
	}
	
	class Entry {
		HashMap<String, Integer> features = new HashMap<String, Integer>();
		int label;
		HashSet<Integer> subjectIndices = new HashSet<Integer>();
		String subjectNerTag;
		Vector<Vector<String>> words;
		String id;
		
		void addSubjectIndices(int start, int end) {
			for (int i = start; i <= end; ++i) {
				subjectIndices.add(new Integer(i));
			}
		}
		
		boolean isSubjectWord(int index) {
			return subjectIndices.contains(new Integer(index));			
		}
		
		int getWindowOfWordIndex(int index) {
			int min = 10000;
			Iterator<Integer> iter = subjectIndices.iterator();
			while (iter.hasNext()) {
				Integer i = iter.next();
				int val = Math.abs(i.intValue() - index);
				if (val < min) {
					min = val;
				}
			}
			if (min <= 5) {
				return 1;
			} else if (min <= 7) {
				return 2;
			} 
			return 3;
		}
	}
	
	class CVPair{
		Vector<Entry> train = new Vector<Entry>();
		Vector<Entry> test = new Vector<Entry>();
	}
	
	class Stat{
		String classifierName;
		double accuracyTest;
		double accuracyTrain;
		
		
		void applyToAverage(Stat t, int amount) {
			accuracyTest = (accuracyTest * (amount - 1)) + t.accuracyTest;
			accuracyTest /= (double)amount;
			
			accuracyTrain = (accuracyTrain * (amount - 1)) + t.accuracyTrain;
			accuracyTrain /= (double)amount;
		}
		
		public String toString() {
			return classifierName + ": [ACC TEST: " + accuracyTest + ", ACC TRAIN: " + accuracyTrain + "]";
		}
	}
	
	public static void main(String []args) throws Exception {
		new TwitterApplication(System.getProperty("config"));
	}
	
}
