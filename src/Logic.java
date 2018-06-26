import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;


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

    public void run() throws IOException, ParseException, Exception {

        System.out.format("Running with k = %d", config.k);


        Vector<Entry> trainData = readData(config.train);
        Vector<Entry> testData = readData(config.test);

        runTrainAndTest(trainData, testData);


    }


//    private ArrayList<String> readClasses(String classesFile)
//    {
//
//    }


    private Vector<Entry> readData(String dataFile) throws Exception {
        Vector<Entry> dataEntries = new Vector<Entry>();
        BufferedReader reader = new BufferedReader(new FileReader(dataFile));
        int counter = 0;
        String line;

        while ((line = reader.readLine()) != null) {
            counter++;
            if (counter % 100 == 0)
                System.out.println("" + counter + " lines were loaded");

            String []tokens = line.split(",");
            Entry entry = new Entry();
            entry.id = tokens[0];
            entry.label = Integer.parseInt(tokens[1]);
            // TODO - lemmatize and stem
            String[] words_lemmatized = (tokens[2] + " " + tokens[3]).split(" ");
//            Vector<Vector<String>> words = openNlp.processText(tokens[2] + " " + tokens[3], true, true);
            entry.words = words_lemmatized;
            dataEntries.add(entry);
        }
        reader.close();
        return dataEntries;
    }


    /*
     * build a model and test a specific classifier on a given testset.
     * returns the accuracy running on the test and train sets.
     */
    void testClassifier(Classifier classifier, Instances trainData, Instances testData) throws Exception {

        classifier.buildClassifier(trainData);
        double accuracy = 0;
        for (int i = 0; i < testData.numInstances(); ++i) {
            Instance inst = testData.instance(i);
            double pred = classifier.classifyInstance(inst);
            if (inst.classValue() == pred) {
                accuracy++;
            }
            // TODO - write to file
        }
        accuracy = accuracy / (double)testData.numInstances();
        double accuracyTest = accuracy;

//        accuracy = 0;
//        for (int i = 0; i < trainData.numInstances(); ++i) {
//            Instance inst = trainData.instance(i);
//            double pred = classifier.classifyInstance(inst);
//            if (inst.classValue() == pred)
//                accuracy++;
//        }
//        accuracy = accuracy / (double)trainData.numInstances();
//        double accuracyTrain = accuracy;

    }

    void runTrainAndTest(Vector<Entry> train, Vector<Entry> test) throws Exception {
        createDataFolder(train, trainFolder);

        TextDirectoryLoader loader = new TextDirectoryLoader();
        loader.setDirectory(new File(trainFolder));
        Instances dataRaw = loader.getDataSet();
        StringToWordVector filter = new StringToWordVector();
        filter.setOptions(weka.core.Utils.splitOptions("-I -T")); // for TFIDF weighting function
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

//        Vector<Stat> stats = new Vector<Stat>();


        /// kNN - no normalization (k = 1)
        Classifier classifier = new IBk();
        String[] options = weka.core.Utils.splitOptions("-K 1 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
        ((IBk) classifier).setOptions(options);
        testClassifier(classifier, trainFiltered, testFiltered);

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

    // members:
    String trainFolder;
    String testFolder;
    Configuration config;
    StopwordAnalyzerBase analyzer;
    Directory indexDir = new RAMDirectory();
    IndexSearcher indexSearcher;
    IndexWriter writer;
    Similarity similarity;
//    Evaluator evaluator = null;
}
