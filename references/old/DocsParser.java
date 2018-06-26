package old;

import org.apache.lucene.analysis.CharArraySet;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Iterator;

public class DocsParser {

    private static final String delimiter = "\\s+";
    private static Map<String, Integer> wordCounts = new HashMap<String, Integer>(); // for the stop words

    public static List<ParsedDoc> parse(BufferedReader reader, String mode) throws IOException {

        String line;
        List<ParsedDoc> parsedDocs = new ArrayList<>();
        String[] splitLine = null;
        String content = "";
        ParsedDoc parsedDoc;

        // Read first lines until reaching first doc (header of first doc)
        while ((line = reader.readLine()) != null) {
            splitLine = line.trim().split(delimiter);
            if (isDocHeaderLine(splitLine)) { // New doc
                break;
            }
        }

        int id = Integer.parseInt(splitLine[1]);
        String date = splitLine[2];

        // Read content until next doc header
        while ((line = reader.readLine()) != null) {
            splitLine = line.trim().split(delimiter);
            if (isDocHeaderLine(splitLine)) { // New doc

                parsedDoc = new ParsedDoc(id, content, date);
                parsedDocs.add(parsedDoc);
                content = "";

                id = Integer.parseInt(splitLine[1]);
                date = splitLine[2];
                continue;
            }
            line = Utils.StripPunctuationsAndSigns(line);
            content += " " + line.toLowerCase();

            // word count
            countWords(line);
        }
        // Add last doc (the one that does not have a header afterwards)
        parsedDoc = new ParsedDoc(id, content, date);
        parsedDocs.add(parsedDoc);
        return parsedDocs;
    }

    private static void countWords(String line){
        String[] words = line.toLowerCase().split(delimiter);
        for (String word : words) {
            if (word.trim().isEmpty() || word.trim() == "")
                continue;

            Integer count = wordCounts.get(word);
            if (count == null) {
                count = 0;
            }
            wordCounts.put(word, count + 1);
        }
    }


    
    private static boolean isDocHeaderLine(String[] splitLine){
        return splitLine.length > 0 && splitLine[0].equals("*TEXT");
    }

        public static CharArraySet getMostFrequentWords(int numberOfWords) {
            CharArraySet stopWords = new CharArraySet(20, true);

            Map sortedMap = sortByValue(wordCounts);

            Iterator iterator = sortedMap.entrySet().iterator();
            int ii = 0;
            while (iterator.hasNext()) {
                Map.Entry mapEntry = (Map.Entry) iterator.next();
                if (ii < numberOfWords) {
                    stopWords.add(mapEntry.getKey().toString());
                    ii++;
                }
            }
            return stopWords;
        }

    private static Map sortByValue(Map unsortedMap) {
        Map sortedMap = new TreeMap(new ValueComparator(unsortedMap));
        sortedMap.putAll(unsortedMap);
        return sortedMap;
    }
}

class ValueComparator implements Comparator {

    Map map;

    public ValueComparator(Map map) {
        this.map = map;
    }

    public int compare(Object keyA, Object keyB) {
        Comparable valueA = (Comparable) map.get(keyA);
        Comparable valueB = (Comparable) map.get(keyB);
        return valueB.compareTo(valueA);
    }
}
