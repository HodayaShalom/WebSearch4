package old;

import java.io.IOException;
import java.util.*;

import old.QueryReader;
import old.TruthParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;


public class Tester {
    public static void main(String[] args) throws IOException, ParseException {
        improvedVsReqular();
    }

    private static void improvedVsReqular() throws IOException, ParseException {
        String parameters = "parameters_improved.txt";
        Configuration config = new Configuration(parameters);

        Logic logic1 = new Logic(config);
        logic1.run();

        String parameters_basic = "parameters.txt";
        Configuration config_basic = new Configuration(parameters_basic);

        Logic logic2 = new Logic(config_basic);
        logic2.run();

        System.out.println("Improved mode Results:");
        System.out.println("F-Measure=" + logic1.getAvarageFMeasure() + " Precision=" + logic1.getAvaragePrecision() + " Recall=" + logic1.getAvarageRecall());

        System.out.println("Basic mode Results:");
        System.out.println("F-Measure=" + logic2.getAvarageFMeasure() + " Precision=" + logic2.getAvaragePrecision() + " Recall=" + logic2.getAvarageRecall());

    }
    private static void smilarityTester() throws IOException, ParseException {
        String configFile = "parameters.txt";
        Configuration config = new Configuration(configFile);

        // Test ClassicSimilarity
        Logic logic1 = new Logic(config);
        logic1.setSimilarity(new ClassicSimilarity());
        logic1.run();


        // test BM25Similarity
        Logic logic2 = new Logic(config);
        logic2.setSimilarity(new BM25Similarity());
        logic2.run();

        System.out.println("ClassicSimilarity Results:");
        System.out.println("F-Measure=" + logic1.getAvarageFMeasure() + " Precision=" + logic1.getAvaragePrecision() + " Recall=" + logic1.getAvarageRecall());

        System.out.println("BM25Similarity Results:");
        System.out.println("F-Measure=" + logic2.getAvarageFMeasure() + " Precision=" + logic2.getAvaragePrecision() + " Recall=" + logic2.getAvarageRecall());
    }
    private static void truthParserTer(){
        TruthParser tp = new TruthParser("data\\truth.txt");
        tp.parse();
        Map<String, String[]> truth = tp.getTruth();

        System.out.println("truth parser done");
    }

    private static void test1(){
        QueryReader reader = new QueryReader("data/queries.txt", false);
        reader.Read();
        Map<String, String> queries = reader.getQueries();

        for (Map.Entry<String, String> entry : queries.entrySet()) {
            System.out.println("Query id=" + entry.getKey() + " content=" + entry.getValue());
        }
    }
    private static void printResults(Map<Integer, ScoreDoc[]> hitsRes, IndexSearcher searcher) throws IOException {
        for (Map.Entry<Integer, ScoreDoc[]> entry : hitsRes.entrySet()) {
            ScoreDoc[] hits = entry.getValue();
            System.out.println("entry: " + entry.getKey() + " Found " + hits.length + " hits.");


            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                System.out.println((i + 1) + ". " + d.get("id") + "\t" + d.get("content"));
            }

        }
    }

    private static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("content", title, Field.Store.YES));

        // use a string field for isbn because we don't want it tokenized
        doc.add(new StringField("id", isbn, Field.Store.YES));
        w.addDocument(doc);
    }

//    private static IndexWriter indexDocs(Directory index, IndexWriterConfig config) throws IOException{
//        IndexWriter w = new IndexWriter(index, config);
//        addDoc(w, "Lucene in Action", "193398817");
//        addDoc(w, "Lucene for Dummies", "55320055Z");
//        addDoc(w, "Managing Gigabytes", "55063554A");
//        addDoc(w, "The Art of Computer Science", "9900333X");
//        w.close();
//        return w;
//    }

    private static Map<Integer, Query> parseQueries(Map<Integer, String> map, StandardAnalyzer analyzer) throws ParseException {
        Map<Integer, Query> queries = new HashMap<Integer, Query>();

        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            Query q = new QueryParser("content", analyzer).parse(entry.getValue());
            queries.put(entry.getKey(), q);
        }

        return queries;
    }

    private static Map<String, String> getDocsExample() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("1", "CHANCELLOR TO PRESIDENT KENNEDY SUGGESTED THAT GERMANY, WHICH ALREADY\n" +
                "\n" +
                "SUPPLIES ALMOST 50 PER CENT OF NATO GROUND STRENGTH, DOES NOT INTEND TO\n" +
                "\n" +
                "RAISE ANY MORE DIVISIONS FOR CONVENTIONAL WARFARE . YET U.S . STRATEGIC");
        map.put("2", "KHRUSHCHEV LAUNCHED HIS GRANDIOSE \" VIRGIN LANDS \" GAMBLE . PART OF THE\n" +
                "\n" +
                "PLAN WAS TO PLOW UP 32 MILLION ACRES OF MARGINAL LAND IN KAZAKHSTAN,\n" +
                "\n" +
                "AND SETTLE IT WITH COMMUNIST \" PIONEERS, \" WHO WERE TO PLANT AND\n" +
                "\n" +
                "PRODUCE HUGE QUANTITIES OF DESPERATELY NEEDED GRAIN WITHIN TWO YEARS .");
        map.put("3", "BERLIN ONE LAST RUN HANS WEIDNER HAD BEEN HOPING FOR MONTHS TO\n" +
                "\n" +
                "ESCAPE DRAB EAST GERMANY AND MAKE HIS WAY TO THE WEST . THE ODDS WERE\n" +
                "\n" +
                "AGAINST HIM, FOR WEIDNER, 40, WAS A CRIPPLE ON CRUTCHES WHO LIVED IN");
        map.put("4", "THE ROAD TO JAIL IS PAVED WITH NONOBJECTIVE ART SINCE THE\n" +
                "\n" +
                "KREMLIN'S SHARPEST BARBS THESE DAYS ARE AIMED AT MODERN ART AND \"\n" +
                "\n" +
                "WESTERN ESPIONAGE, \" IT WAS JUST A MATTER OF TIME BEFORE THE KGB'S COPS\n" +
                "\n" +
                "WOULD TURN UP A VICTIM WHOSE WRONGDOINGS COMBINED BOTH EVILS . HE\n" +
                "\n" +
                "TURNED OUT TO BE A LENINGRAD PHYSICS TEACHER WHOSE TASTE FOR ABSTRACT\n" +
                "\n" +
                "PAINTING ALLEGEDLY LED HIM TO JOIN THE U.S . SPY SERVICE . POLICE SAID");
        map.put("5", "THE CONGO ROUND 3 ? THE SOUND OF\n" +
                "\n" +
                "CHRISTMAS IN KATANGA PROVINCE WAS THE THUNK OF MORTAR SHELLS AND THE\n" +
                "\n" +
                "RATTLE OF MACHINE GUNS . AFTER AN UNEASY TWELVE-MONTH TRUCE BETWEEN U.N\n" +
                "\n" +
                ". FORCES AND THE TROOPS OF KATANGA'S SECESSIONIST MOISE TSHOMBE, A FEW\n" +
                "\n" +
                "MINOR INCIDENTS GOT OUT OF HAND, AND FOR THE THIRD TIME SINCE SEPTEMBER");

//        map.put("193398817", "Lucene in Action");
//        map.put("55320055Z", "Lucene for Dummies");
//        map.put("55063554A", "Managing Gigabytes");
//        map.put("9900333X", "The Art of Computer Science");

        return map;
    }

    private static  Map<Integer, String> getQueriesExample() {
        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(1, "WOULD LENINGRAD SERVICE");
        map.put(2, "INCIDENTS HAND TIME");
        map.put(3, "DIVISIONS AND SERVICE");
        map.put(4, "MORTAR TOWARD");

          //map.put(1, "lucene");
        return map;
    }

    private static void indexDocsFromDocsList(Map<String, String> docs, Directory index, IndexWriterConfig config) throws IOException {
        IndexWriter w = new IndexWriter(index, config);

        for (Map.Entry<String, String> entry : docs.entrySet()) {
            addDoc(w, entry.getValue(), entry.getKey());
        }
        w.close();

    }
}
