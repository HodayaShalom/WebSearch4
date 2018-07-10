import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

//import opennlp.tools.stemmer.PorterStemmer;
import org.apache.commons.io.FileUtils;

import org.apache.commons.io.FilenameUtils;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.TextDirectoryLoader;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Reorder;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.core.stemmers.SnowballStemmer;

public class Logic {

    Logic(Configuration config) {
        this.config = config;
    }

    public void run() throws IOException, Exception {

        System.out.format("Running with k = %d %tT %n", config.k, LocalDateTime.now());

        if(config.recreateWekaDataFolders) {
            System.out.format("Skipping reading data and creating weka data folders %n");

            Vector<Entry> trainData = readData(config.train);
//        calculateFeatures(trainData);
            Vector<Entry> testData = readData(config.test);
//        calculateFeatures(testData);

            writeDataInWekaFormat(trainData, testData);
        }
        ArrayList<ClassificationResult> results = runTrainAndTest();

        writeResultsToFile(results);


    }

    private Vector<Entry> readData(String dataFile) throws Exception {
        Vector<Entry> dataEntries = new Vector<>();
        BufferedReader reader = new BufferedReader(new FileReader(dataFile));
        int counter = 0;
        String line;

        System.out.format("reading data from %s%n", dataFile);
        while ((line = reader.readLine()) != null) {
            counter++;
            if (counter % 100 == 0)
                System.out.println("" + counter + " lines were loaded");

            String[] tokens = line.split(",");
            Entry entry = new Entry();
            entry.id = tokens[0];
            entry.label = Integer.parseInt(tokens[1]);

            // TODO - lowercase all text, and clean text from non letter chars. (in a function?)

            String text = tokens[2] + " " + tokens[3];

            text = Utils.StripPunctuationsAndSigns(text);

            // TODO - lemmatize and stem

//            PorterStemmer ps = new PorterStemmer();
//
//            ps.
//            String[] words = text.split(" ");
//            Vector<Vector<String>> words = openNlp.processText(tokens[2] + " " + tokens[3], true, true);

//            entry.words = new Vector<>(Arrays.asList(words));
            entry.text = text;
            dataEntries.add(entry);
            // write here to weka instead of storing data to entry?
//            writeEntryToDataFile(entry, wekaFolder);
        }
        reader.close();

        System.out.format("Done reading data from %s %tT %n", dataFile , LocalDateTime.now());
        return dataEntries;
    }

    void writeDataInWekaFormat(Vector<Entry> train, Vector<Entry> test) throws Exception {

        // write to directory in weka "format"
        createDataFolder(train, config.trainFolder);
        System.out.format("Done creating data folder for train%n");

        createDataFolder(test, config.testFolder);
        System.out.format("Done creating data folder for test%n");
    }

    /*
     * build a model and test a specific classifier on a given testset.
     * returns the accuracy running on the test and train sets.
     */
    ArrayList<ClassificationResult> testClassifier(Classifier classifier, Instances trainData, Instances testData) throws Exception {

        ArrayList<ClassificationResult> classificationResults = new ArrayList<>();

        System.out.format("building classifier %tT %n", LocalDateTime.now());
        classifier.buildClassifier(trainData);

        System.out.format("running classifier on test data%n");
        double accuracy = 0;
        int i = 0;
        for (Instance instance: testData) {
            double pred = classifier.classifyInstance(instance);
            if (instance.classValue() == pred) {
                accuracy++;
            }
            System.out.format("num attributes %d%n",instance.numAttributes());


            System.out.println("\n0: "+instance.attribute(0) + "value: " + instance.stringValue(0)); // text
            System.out.println("\n1: "+instance.attribute(1) + "value: " + instance.stringValue(1)); // filename
            System.out.println("\n2: "+instance.attribute(2) + "value: " + instance.stringValue(2)); // class

            System.out.print("ID: " + testData.instance(i).value(0));
            System.out.print(", actual: " + testData.classAttribute().value((int) instance.classValue()));
            System.out.println(", predicted: " + testData.classAttribute().value((int) pred));

            System.out.format("doc %d: pred=%f,true=%f%n",i, pred, instance.classValue());
            i++;

            if (i % 100 == 0)
                System.out.println("" + i + " predicted out of " + testData.numInstances());

            // TODO - somehow retrieve the docId - DONE??

            String docId = FilenameUtils.getBaseName(instance.stringValue(1)).replaceFirst("^doc", "");
//            docId = Integer.toString(i);
            Integer predClassInt = Integer.parseInt(testData.classAttribute().value((int) pred).replaceFirst("^class", ""));
            Integer trueClassInt = Integer.parseInt(testData.classAttribute().value((int) instance.classValue()).replaceFirst("^class", ""));
            ClassificationResult result = new ClassificationResult(docId, trueClassInt, predClassInt);
            classificationResults.add(result);

        }
        accuracy = accuracy / (double) testData.numInstances();

        // TODO - print accuracy to console (micro & macro F-score??)

        System.out.format("Classification accuracy is: %f%n", accuracy);

        return classificationResults;
    }


    private void writeResultsToFile(ArrayList<ClassificationResult> results)  throws IOException {

//        Your software must write the test-set classification results to the specified output file in the
//        following format:
//        DocID, PredictedClassNumber, TrueClassNumber
//        The list should be string-sorted by docID, and the fields are comma separated.

        System.out.println("writing results to file. path=" + config.output);
        PrintWriter writer = new PrintWriter(config.output);

        results.sort(Comparator.comparing(ClassificationResult::getDocId));
        for(ClassificationResult res: results){
//            System.out.format("doc %d: pred=%f,true=%f%n",res.docId, res.predictedClass, res.trueClass);
            writer.printf("%s, %d, %d%n",res.docId, res.predictedClass, res.trueClass);
        }
        writer.close();
    }

    ArrayList<ClassificationResult> runTrainAndTest() throws Exception {

        // load from weka directory into Instances
        TextDirectoryLoader loader = new TextDirectoryLoader();
        loader.setOutputFilename(true);

        loader.setDirectory(new File(config.trainFolder));
        Instances dataRawTrain = loader.getDataSet();
        System.out.format("Done loading train data to weka %tT %n", LocalDateTime.now());

        loader.setDirectory(new File(config.testFolder));
        Instances dataRawTest = loader.getDataSet();
        System.out.format("Done loading test data to weka %tT %n", LocalDateTime.now());

        System.out.format("trainFiltered num attributes before filter %d%n",dataRawTrain.numAttributes());
        System.out.println("\n0: "+dataRawTrain.instance(0).attribute(0));
        System.out.println("\n1: "+dataRawTrain.instance(0).attribute(1));
        System.out.println("\n2: "+dataRawTrain.instance(0).attribute(2));
//        Attribute att = dataRawTrain.attribute("filename");

        // TODO - use filter to lowercase all words, stemming, etc.. check documentation
        StringToWordVector stringToWordVectorFilter = new StringToWordVector();
//        filter.setOptions(weka.core.Utils.splitOptions("-I -T"));
        // for TFIDF weighting function
        stringToWordVectorFilter.setTFTransform(true);
        stringToWordVectorFilter.setIDFTransform(true);
        stringToWordVectorFilter.setLowerCaseTokens(true);

        // TODO - use stemmer here? stop words?
        SnowballStemmer snowball=new SnowballStemmer();
//        snowball.setStemmer("porter");
        stringToWordVectorFilter.setStemmer(snowball);
//        stringToWordVectorFilter.setStopwordsHandler();
//        tfIdfFilter.setInputFormat(dataRaw);
//        Instances trainFiltered = Filter.useFilter(dataRaw, tfIdfFilter);

        Reorder reorder = new Reorder();
        reorder.setOptions(weka.core.Utils.splitOptions("-R 2-last,first"));
//        reorder.setInputFormat(trainFiltered);
//        trainFiltered = Filter.useFilter(trainFiltered, reorder);

//        System.out.format("trainFiltered num attributes after filter %d%n",trainFiltered.numAttributes());

        // TODO - check filters actually do what they are supposed to do (tfidf on the words, without the filename attribute)
        Remove rm = new Remove();
        rm.setAttributeIndices("2"); // ? remove filename attribute

        MultiFilter multifilter = new MultiFilter();
        multifilter.setInputFormat(dataRawTrain);
        multifilter.setFilters(new Filter[]{rm, stringToWordVectorFilter, reorder});

        //filter.setInputFormat(dataRawTest);
//        Instances testFiltered = Filter.useFilter(dataRawTest, tfIdfFilter);
//        reorder.setInputFormat(testFiltered);
//        testFiltered = Filter.useFilter(testFiltered, reorder);

//        Vector<Stat> stats = new Vector<Stat>();


        // TODO - maybe use normalization?? check if it improves results

        // TODO 2 - run here all possible k's, for performance (only for us, not for submission. otherwise it would take forever to run everything)
        /// kNN - no normalization
        Classifier classifier = new IBk();
        String[] options = weka.core.Utils.splitOptions(
                "-A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -D\\\"\"");
        ((IBk) classifier).setOptions(options);
        ((IBk) classifier).setKNN(config.k);
//        return testClassifier(classifier, trainFiltered, testFiltered);


        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(multifilter);
        fc.setClassifier(classifier);


        return testClassifier(fc, dataRawTrain, dataRawTest);

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
        int counter = 0;

        // this takes a really long time...
        for (Entry entry: data) {

            counter++;
            if (counter % 100 == 0)
                System.out.println("" + counter + " files were created");

            System.out.format("writing to weka %s doc %s %tT %n", folder ,entry.id, LocalDateTime.now());
            writeEntryToDataFile(entry, folder);
        }
    }

    void writeEntryToDataFile(Entry entry, String folder) throws FileNotFoundException {
        File file = new File(folder + "/class" + entry.label + "/doc" + entry.id + ".txt");
        file.getParentFile().mkdirs();
        PrintWriter pw = new PrintWriter(file);

        pw.print(entry.text);
        pw.close();
    }


    // members:
    Configuration config;
}
