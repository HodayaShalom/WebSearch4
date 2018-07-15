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
    PrintWriter logwriter;

    Logic(Configuration config) {
        this.config = config;
    }

    public void run() throws IOException, Exception {

        try {

            System.out.format("Running with k = %d %tT %n", config.k, LocalDateTime.now());

            logwriter = new PrintWriter("out/log.txt");

            logwriter.printf("running %tT%n", LocalDateTime.now());

            logwriter.flush();
            if (config.recreateWekaDataFolders) {

                Vector<Entry> trainData = readData(config.train);

                Vector<Entry> testData = readData(config.test);

                writeDataInWekaFormat(trainData, testData);
            } else {
                System.out.format("Skipping reading data and creating weka data folders %n");
            }
            ArrayList<ClassificationResult> results = runTrainAndTest();

            writeResultsToFile(results);
        } finally {
            logwriter.close();
        }
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

            String text = tokens[2] + " " + tokens[3];

            entry.text = text;
            dataEntries.add(entry);
        }
        reader.close();

        System.out.format("Done reading data from %s %tT %n", dataFile, LocalDateTime.now());
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
    ArrayList<ClassificationResult> testClassifier(Classifier classifier, Instances trainData, Instances testData, int k) throws Exception {
        ArrayList<ClassificationResult> classificationResults = new ArrayList<>();

        System.out.format("building classifier %tT %n", LocalDateTime.now());
        classifier.buildClassifier(trainData);

        System.out.format("running classifier on test data%n");
        double accuracy = 0;
        int i = 0;
        int numOfClasses = 14;

        Evaluator evaluator = new Evaluator(numOfClasses);

        PrintWriter writer2 = new PrintWriter("out/out_intermediate.csv");

        try {
            for (Instance instance : testData) {
                double pred = classifier.classifyInstance(instance);

                evaluator.Eval(instance.classValue(), pred);

                if (instance.classValue() == pred) {
                    accuracy++;
                }

                i++;

                if (i % 100 == 0) {
                    double percent = i / testData.numInstances();
                    System.out.format("%d predicted out of %d %f%% %tT %n", i, testData.numInstances(), percent, LocalDateTime.now());
                    logwriter.printf("%d predicted out of %d %f%% %tT %n", i, testData.numInstances(), percent, LocalDateTime.now());
                    logwriter.flush();
                }

                String docId = FilenameUtils.getBaseName(instance.stringValue(1)).replaceFirst("^doc", "");

                Integer predClassInt = Integer.parseInt(testData.classAttribute().value((int) pred).replaceFirst("^class", ""));
                Integer trueClassInt = Integer.parseInt(testData.classAttribute().value((int) instance.classValue()).replaceFirst("^class", ""));
                ClassificationResult result = new ClassificationResult(docId, trueClassInt, predClassInt);
                classificationResults.add(result);


                writer2.printf("%s, %d, %d%n", result.docId, result.predictedClass, result.trueClass);
                writer2.flush();

            }

            double micro = evaluator.CalcMicroAvarage();
            double macro = evaluator.CalcMacroAvarage();
            System.out.format("k=%d:%n micro F-score=%f%n macro F-score=%f %tT %n", k, micro, macro, LocalDateTime.now());
            logwriter.printf("k=%d:%n micro F-score=%f%n macro F-score=%f %tT %n", k, micro, macro, LocalDateTime.now());

            accuracy = accuracy / (double) testData.numInstances();

            System.out.format("Classification accuracy is: %f%n", accuracy);
            logwriter.printf("Classification accuracy is: %f%n", accuracy);


            logwriter.close();
            writer2.close();
        } finally {
            writer2.close();
        }
        return classificationResults;
    }


    private void writeResultsToFile(ArrayList<ClassificationResult> results) throws IOException {

//        Your software must write the test-set classification results to the specified output file in the
//        following format:
//        DocID, PredictedClassNumber, TrueClassNumber
//        The list should be string-sorted by docID, and the fields are comma separated.

        System.out.println("writing results to file. path=" + config.output);
        PrintWriter writer = new PrintWriter(config.output);

        results.sort(Comparator.comparing(ClassificationResult::getDocId));
        for (ClassificationResult res : results) {
//            System.out.format("doc %d: pred=%f,true=%f%n",res.docId, res.predictedClass, res.trueClass);
            writer.printf("%s, %d, %d%n", res.docId, res.predictedClass, res.trueClass);
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
        logwriter.printf("Done loading train data to weka %tT %n", LocalDateTime.now());

        loader.setDirectory(new File(config.testFolder));
        Instances dataRawTest = loader.getDataSet();
        System.out.format("Done loading test data to weka %tT %n", LocalDateTime.now());
        logwriter.printf("Done loading test data to weka %tT %n", LocalDateTime.now());

        logwriter.flush();


        System.out.format("trainFiltered num attributes before filter %d%n", dataRawTrain.numAttributes());
        System.out.println("\n0: " + dataRawTrain.instance(0).attribute(0));
        System.out.println("\n1: " + dataRawTrain.instance(0).attribute(1));
        System.out.println("\n2: " + dataRawTrain.instance(0).attribute(2));

        StringToWordVector stringToWordVectorFilter = new StringToWordVector();

        // for TFIDF weighting function
        stringToWordVectorFilter.setTFTransform(true);
        stringToWordVectorFilter.setIDFTransform(true);
        stringToWordVectorFilter.setLowerCaseTokens(true);

        SnowballStemmer snowball = new SnowballStemmer();
        stringToWordVectorFilter.setStemmer(snowball);

        Reorder reorder = new Reorder();
        reorder.setOptions(weka.core.Utils.splitOptions("-R 2-last,first"));

        // filter to remove the attribute filename from, so that it won't affect the results
        Remove rm = new Remove();
        rm.setAttributeIndices("2"); // remove filename attribute

        MultiFilter multifilter = new MultiFilter();
        multifilter.setInputFormat(dataRawTrain);
        multifilter.setFilters(new Filter[]{rm, stringToWordVectorFilter, reorder});

        /// kNN - no normalization
        ArrayList<Integer> kList = getKList(true);

        ArrayList<ClassificationResult> results = null;

        for (Integer k : kList) {
            Classifier classifier = new IBk();
            String[] options = weka.core.Utils.splitOptions(
                    "-A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance\\\"\"");
            ((IBk) classifier).setOptions(options);
            ((IBk) classifier).setKNN(k);

            FilteredClassifier fc = new FilteredClassifier();
            fc.setFilter(multifilter);
            fc.setClassifier(classifier);

            results = testClassifier(fc, dataRawTrain, dataRawTest, k);
        }

        // return results of last k run
        return results;
    }

    ArrayList<Integer> getKList(boolean prod) {
        // PRODUCTION LIST
        ArrayList<Integer> kListProd = new ArrayList<Integer>() {{
            add(config.k);
        }};

        // TESTING LIST
        ArrayList<Integer> kListTest = new ArrayList<Integer>() {{
//                add(1);
//                add(5);
//                add(10);
            add(20);
//                add(30);
//                add(50);

        }};
        if (prod) {
            return kListProd;
        } else {
            return kListTest;
        }
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
        for (Entry entry : data) {

            counter++;
            if (counter % 100 == 0)
                System.out.println("" + counter + " files were created");

            System.out.format("writing to weka %s doc %s %tT %n", folder, entry.id, LocalDateTime.now());
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
