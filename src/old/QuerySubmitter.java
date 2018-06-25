package old;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;

import java.io.IOException;
import java.util.*;

public class QuerySubmitter {
    Map<String, Query> queries;
    IndexSearcher indexSearcher;
    String mode;
    double threshold;

    public QuerySubmitter(Map<String, Query> parsed_queries, IndexSearcher indexSearcher, String mode) {
        this.queries = parsed_queries;
        this.indexSearcher = indexSearcher;
        this.mode = mode;
        this.threshold = 0.4;
    }

    public Map<String, ScoreDoc[]> searchQueries() throws IOException {
        Map<String, ScoreDoc[]> queries_hits = new HashMap<>();
        for (Map.Entry<String, Query> entry : queries.entrySet()) {
            ScoreDoc[] hits = searchQuery(entry.getValue());
            queries_hits.put(entry.getKey(), hits);
        }
        Map<String, ScoreDoc[]> sortedMap = Utils.sortByKey(queries_hits);
        return sortedMap;
    }

    private ScoreDoc[] searchQuery(Query query) throws IOException{
        int hitsPerPage = 10;

        TopDocs docs = this.indexSearcher.search( query, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        if(this.mode.equals("improved")){
            for (ScoreDoc hit : hits){
                System.out.print(hit.score + " ");
            }

            System.out.println();

            ScoreDoc[] filteredHits = Arrays.stream(hits).filter(h -> h.score > this.threshold).toArray(ScoreDoc[]::new);
            return filteredHits;
        }

        return hits;
    }

}


