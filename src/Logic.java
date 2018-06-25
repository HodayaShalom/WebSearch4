import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import old.*;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class Logic {

    Logic(Configuration config) {
        this.config = config;
        this.similarity = new ClassicSimilarity();
//        this.similarity = new BM25Similarity();
    }

    public void run() throws IOException, ParseException, Exception {

        System.out.format("Running with k = %d", config.k);


        Vector<Entry> trainData = readData(config.train);
        

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


    private


    // members:
    Configuration config;
    StopwordAnalyzerBase analyzer;
    Directory indexDir = new RAMDirectory();
    IndexSearcher indexSearcher;
    IndexWriter writer;
    Similarity similarity;
    Evaluator evaluator = null;
}
