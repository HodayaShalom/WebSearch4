import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.IndexSearcher;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Utils {
//    public static String[] scoreDocArrayToStringArray(ScoreDoc[] scoreDocs, IndexSearcher searcher) throws IOException {
//
//        String[] docIds = new String[scoreDocs.length];
//
//        for (int i = 0; i < scoreDocs.length; ++i) {
//            int docId = scoreDocs[i].doc;
//            Document d = searcher.doc(docId);
//            docIds[i] = d.get("id");
//        }
//
//        return docIds;
//    }
//
//    public static Map<String, ScoreDoc[]> docsOverThresholdArray(Map<String, ScoreDoc[]> results, float T) {
//        Map<String, ScoreDoc[]> scoresOverT = new TreeMap<>();
//
//        for (Map.Entry<String, ScoreDoc[]> entry : results.entrySet()) {
//            scoresOverT.put(entry.getKey(), docsOverThreshold(entry.getValue(), T));
//        }
//
//        return scoresOverT;
//    }
//
//    public static ScoreDoc[] docsOverThreshold(ScoreDoc[] scoreDocs, float T) {
//        int count = 0;
//
//        for (int i = 0; i < scoreDocs.length; i++) {
//            if (scoreDocs[i].score >= T) {
//                count++;
//            }
//        }
//
//        ScoreDoc[] scoreDocsOverT = new ScoreDoc[count];
//
//        int j = 0;
//        for (int i = 0; i < scoreDocs.length; i++) {
//            if (scoreDocs[i].score >= T) {
//                scoreDocsOverT[j] = scoreDocs[i];
//                j++;
//            }
//        }
//
//        return scoreDocsOverT;
//    }

    public static String StripPunctuationsAndSigns(String str) {
        return str.replaceAll("[.,\"?;'-]", "");
    }


    public static Map sortByKey(Map unsortedMap) {
        Comparator<String> comparator = (String o1, String o2) -> {
            int v1 = Integer.parseInt(o1);
            int v2 = Integer.parseInt(o2);

            return v1 < v2 ? -1 : v1 > v2 ? +1 : 0;
        };
        Map sortedMap = new TreeMap(comparator);
        sortedMap.putAll(unsortedMap);
        return sortedMap;
    }

}
