package old;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Logic2 {

    Logic2(Configuration config) {
        this.config = config;
        this.similarity = new ClassicSimilarity();
//        this.similarity = new BM25Similarity();
    }

    public void run() throws IOException, ParseException {

        System.out.format("Running in %s mode", config.mode);
        this.indexDir = new RAMDirectory();

        indexCollection(); // and find stop words list?
        Map<String, Query> queries = readAndParseQueries();
        Map<String, ScoreDoc[]> results = runQueries(queries);

        if (isImprovedMode()){ // filter by threshold
            float threshold;
            threshold = 1f;

//            threshold = 7f;
            results = Utils.docsOverThresholdArray(results, threshold);
        }

        Map<String, String[]> resultDocsIds = getResultsDocsIds(results, this.indexSearcher);
        Map<String, String[]> sortedResults = Utils.sortByKey(resultDocsIds);

        ResultsPrinter printer = new ResultsPrinter(sortedResults, config.output);

        TruthParser tp = new TruthParser(config.truth);
        tp.parse();
        Map<String, String[]> truth = tp.getTruth();

        this.evaluator = new Evaluator(truth, sortedResults);
        evaluator.printEvaluations();
        System.out.println("AVG. Precision: " + evaluator.calcAvaragePrecision() + "\t" +
                "AVG. Recall: " + evaluator.calcAvarageRecall() + "\t" +
                "AVG. F Measure: " + evaluator.calcAvarageFMeasure() + "\t");

        printer.print();
    }

    private void indexCollection() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(config.docs));

        List<ParsedDoc> parsedDocs = DocsParser.parse(bufferedReader, config.mode);
        CharArraySet stopWords = DocsParser.getMostFrequentWords(20);

        if(isImprovedMode()){
            analyzer = new EnglishAnalyzer(stopWords);
        }
        else {
            analyzer = new StandardAnalyzer(stopWords);
        }

        IndexWriterConfig luceneConfig = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(indexDir, luceneConfig);

        for (ParsedDoc parsedDoc : parsedDocs) {
            addDoc(writer, parsedDoc);
        }

        bufferedReader.close();
        writer.close();
        System.out.println("done with indexing");
    }

    private void addDoc(IndexWriter writer, ParsedDoc parsedDoc) throws IOException {
        Document doc = new Document();

        doc.add(new StringField("id", Integer.toString(parsedDoc.getId()), Store.YES));
        doc.add(new TextField("content", parsedDoc.getContent(), Store.YES));

        writer.addDocument(doc);
    }

    private Map<String, Query> readAndParseQueries() throws ParseException {
        QueryReader queryReader = new QueryReader(config.queries, isImprovedMode());
        queryReader.Read();
        Map<String, String> list = queryReader.getQueries();

        QueryParser2 queryParser = new QueryParser2(list, this.analyzer);
        Map<String, Query> queries = queryParser.Parse();

        return queries;
    }

    private Map<String, ScoreDoc[]>  runQueries(Map<String, Query> queries) throws IOException {
        IndexReader reader = DirectoryReader.open(indexDir);
        this.indexSearcher = new IndexSearcher(reader);
        this.indexSearcher.setSimilarity(this.similarity);

        QuerySubmitter querySubmitter = new QuerySubmitter(queries, this.indexSearcher, config.mode);
        Map<String, ScoreDoc[]> results = querySubmitter.searchQueries();

        return results;
    }

    private boolean isImprovedMode(){
        return config.mode.equals("improved");
    }

    public void setSimilarity(Similarity similarity) {
        this.similarity = similarity;
    }

    private Map<String, String[]> getResultsDocsIds(Map<String, ScoreDoc[]> results, IndexSearcher searcher) throws IOException{
        Map<String, String[]> docIdsResults = new HashMap<>();

        for (Map.Entry<String, ScoreDoc[]> entry : results.entrySet()) {
            docIdsResults.put(entry.getKey(), Utils.scoreDocArrayToStringArray(entry.getValue(), searcher));
        }

        return docIdsResults;
    }


    public double getAvarageFMeasure(){
        if (this.evaluator != null){
            return this.evaluator.calcAvarageFMeasure();
        }
        else
        {
            System.out.println("evaluator has not been initialized, returning 0");
            return 0.0;
        }
    }
    public double getAvarageRecall(){
        if (this.evaluator != null){
            return this.evaluator.calcAvarageRecall();
        }
        else
        {
            System.out.println("evaluator has not been initialized, returning 0");
            return 0.0;
        }
    }
    public double getAvaragePrecision(){
        if (this.evaluator != null){
            return this.evaluator.calcAvaragePrecision();
        }
        else
        {
            System.out.println("evaluator has not been initialized, returning 0");
            return 0.0;
        }
    }

    // members:
    Configuration config;
    StopwordAnalyzerBase analyzer;
    Directory indexDir = new RAMDirectory();
    IndexSearcher indexSearcher;
    IndexWriter writer;
    Similarity similarity;
    Evaluator evaluator = null;
}
