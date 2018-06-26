package old;

import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import java.util.HashMap;
import java.util.*;

public class QueryParser2 {
    Map<String, String> list;
    StopwordAnalyzerBase analyzer;

    public QueryParser2(Map<String, String> list, StopwordAnalyzerBase analyzer){
        this.list = list;
        this.analyzer = analyzer;

    }

    public Map<String, Query> Parse() throws ParseException {
        Map<String, Query> queries = new HashMap<String, Query>();
        for (Map.Entry<String, String> entry : list.entrySet()) {
            Query q = new org.apache.lucene.queryparser.classic.QueryParser( "content", analyzer).parse(entry.getValue());
            queries.put(entry.getKey(), q);
        }

        return queries;
    }
}
