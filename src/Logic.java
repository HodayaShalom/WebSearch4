import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.io.FileUtils;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.TextDirectoryLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Reorder;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class Logic {

    Logic(Configuration config) {
        this.config = config;
    }

    public void run() throws IOException, Exception {

        System.out.format("Running with k = %d", config.k);


        Vector<Entry> trainData = readData(config.train);
//        calculateFeatures(trainData);
        Vector<Entry> testData = readData(config.test);
//        calculateFeatures(testData);

        ArrayList<ClassificationResult> results = runTrainAndTest(trainData, testData);

        writeResultsToFile(results);


    }

    private void writeResultsToFile(ArrayList<ClassificationResult> results) {
        // TODO - write results to config.outputFile
//        Your software must write the test-set classification results to the specified output file in the
//        following format:
//        DocID, PredictedClassNumber, TrueClassNumber
//        The list should be string-sorted by docID, and the fields are comma separated.
    }

    private Vector<Entry> readData(String dataFile) throws Exception {
        Vector<Entry> dataEntries = new Vector<>();
        BufferedReader reader = new BufferedReader(new FileReader(dataFile));
        int counter = 0;
        String line;

        System.out.format("reading data from %s", dataFile);
        while ((line = reader.readLine()) != null) {
            counter++;
            if (counter % 100 == 0)
                System.out.println("" + counter + " lines were loaded");

            String[] tokens = line.split(",");
            Entry entry = new Entry();
            entry.id = tokens[0];
            entry.label = Integer.parseInt(tokens[1]);

            // TODO - lowercase all text, and clean text from non letter chars. (in a function?)
            // TODO - lemmatize and stem
            String[] words_lemmatized = (tokens[2] + " " + tokens[3]).split(" ");
//            Vector<Vector<String>> words = openNlp.processText(tokens[2] + " " + tokens[3], true, true);

            entry.words = new Vector<>(Arrays.asList(words_lemmatized));
            dataEntries.add(entry);
        }
        reader.close();
        return dataEntries;
    }

    /*
     * build a model and test a specific classifier on a given testset.
     * returns the accuracy running on the test and train sets.
     */
    ArrayList<ClassificationResult> testClassifier(Classifier classifier, Instances trainData, Instances testData) throws Exception {

        ArrayList<ClassificationResult> classificationResults = new ArrayList<>();

        classifier.buildClassifier(trainData);
        double accuracy = 0;
        for (Instance instance: testData) {
            double pred = classifier.classifyInstance(instance);
            if (instance.classValue() == pred) {
                accuracy++;
            }

            // TODO - somehow retrieve the docId
            String docId = "";
            ClassificationResult result = new ClassificationResult(docId, (int)instance.classValue(), (int)pred);
            classificationResults.add(result);

        }
        accuracy = accuracy / (double) testData.numInstances();

        // TODO - print accuracy to console (micro & macro F-score??)

        System.out.format("Classification accuracy is: %f", accuracy);

        return classificationResults;
//        Your software must write the test-set classification results to the specified output file in the
//        following format:
//        DocID, PredictedClassNumber, TrueClassNumber
//        The list should be string-sorted by docID, and the fields are comma separated.


    }

    ArrayList<ClassificationResult> runTrainAndTest(Vector<Entry> train, Vector<Entry> test) throws Exception {
        // write to directory in weka "format"
        createDataFolder(train, config.trainFolder);

        // load from weka directory into Instances
        TextDirectoryLoader loader = new TextDirectoryLoader();
        loader.setDirectory(new File(config.trainFolder));
        Instances dataRaw = loader.getDataSet();

        StringToWordVector filter = new StringToWordVector();
//        filter.setOptions(weka.core.Utils.splitOptions("-I -T"));
        // for TFIDF weighting function
        filter.setTFTransform(true);
        filter.setIDFTransform(true);
        // use stemmer here? stop words?
        filter.setInputFormat(dataRaw);

        Instances trainFiltered = Filter.useFilter(dataRaw, filter);

        Reorder reorder = new Reorder();
        reorder.setOptions(weka.core.Utils.splitOptions("-R 2-last,first"));
        reorder.setInputFormat(trainFiltered);
        trainFiltered = Filter.useFilter(trainFiltered, reorder);


        createDataFolder(test, config.testFolder);
        loader.setDirectory(new File(config.testFolder));
        dataRaw = loader.getDataSet();
        //filter.setInputFormat(dataRaw);
        Instances testFiltered = Filter.useFilter(dataRaw, filter);
        reorder.setInputFormat(testFiltered);
        testFiltered = Filter.useFilter(testFiltered, reorder);

//        Vector<Stat> stats = new Vector<Stat>();


        /// kNN - no normalization
        Classifier classifier = new IBk();
        String[] options = weka.core.Utils.splitOptions(
                "-A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
        ((IBk) classifier).setOptions(options);
        ((IBk) classifier).setKNN(config.k);
        return testClassifier(classifier, trainFiltered, testFiltered);

//        /// kNN - no normalization (k = 3)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 3 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(3)";
//        stats.add(stat);
//
//        /// kNN - no normalization (k = 5)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 5 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(5)";
//        stats.add(stat);
//
//        /// kNN - no normalization (k = 10)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 10 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(10)";
//        stats.add(stat);
//
//        /// kNN - no normalization (k = 20)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 20 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(20)";
//        stats.add(stat);
//
//        /// kNN - no normalization (k = 30)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 30 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(30)";
//        stats.add(stat);
//
//        /// kNN - no normalization (k = 50)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 50 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(50)";
//        stats.add(stat);
//
//        /// kNN - with normalization (k = 1)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 1 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(1)Norm";
//        stats.add(stat);
//
//        /// kNN - with normalization (k = 3)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 3 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(3)Norm";
//        stats.add(stat);
//
//        /// kNN - with normalization (k = 5)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 5 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(5)Norm";
//        stats.add(stat);
//
//        /// kNN - with normalization (k = 10)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 10 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(10)Norm";
//        stats.add(stat);
//
//        /// kNN - with normalization (k = 20)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 20 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(20)Norm";
//        stats.add(stat);
//
//        /// kNN - with normalization (k = 30)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 30 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(30)Norm";
//        stats.add(stat);
//
//        /// kNN - with normalization (k = 50)
//        classifier = new IBk();
//        options = weka.core.Utils.splitOptions("-K 50 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
//        classifier.setOptions(options);
//        stat = testClassifier(classifier, trainFiltered, testFiltered);
//        stat.classifierName = "KNN(50)Norm";
//        stats.add(stat);
//
//        return stats;
    }


    /*
     * preparing a data folder to be be read by the StringToWords filter of WEKA
     */
    void createDataFolder(Vector<Entry> data, String folder) throws IOException {
        File mainFolder = new File(folder);
        if (mainFolder.exists()) {
            FileUtils.deleteDirectory(mainFolder);
        }

        // create the train/test folder
        for (Entry entry: data) {
            String classFolderPath = folder + "/class" + entry.label;
            File classFolder = new File(classFolderPath);
            if (!classFolder.exists())
                Files.createDirectories(Paths.get(classFolderPath));
            PrintWriter pw = new PrintWriter(folder + "/class" + entry.label + "/doc" + entry.id + ".txt");

            for (String word: entry.words) {
                if (!word.isEmpty())
                    pw.print(word + " ");
            }

//            // using features:
//            for (String word: entry.features.keySet()){
//                // write each word to files according to its frequency
//                Integer num = entry.features.get(word);
//                for (int j = 0; j < num.intValue(); ++j)
//                    pw.print(word + " ");
//            }

            pw.close();
        }
    }


    void calculateFeatures(Vector<Entry> entries) throws Exception {
        for(Entry entry: entries){
            // calculate frequency for all lemmas, using a dictionary for each entry, where (key = lemma, value = frequency in doc)
            for (int i = 0; i < entry.words.size(); ++i) {
                addFeature(entry.features, Utils.cleanWord(entry.words.get(i)));
            }
        }
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


    // members:
    Configuration config;
//    StopwordAnalyzerBase analyzer;
//    Directory indexDir = new RAMDirectory();
//    IndexSearcher indexSearcher;
//    IndexWriter writer;
//    Similarity similarity;
//    Evaluator evaluator = null;
}
