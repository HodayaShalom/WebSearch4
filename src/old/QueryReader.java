package old;

import opennlp.tools.stemmer.PorterStemmer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class QueryReader {
    String path;
    boolean stem_queries;
    private Map<String, String> queries = new HashMap<>();

    public QueryReader(String path, boolean stem_queries) {
        this.path = path;
        this.stem_queries = stem_queries;
    }

    public Map<String, String> getQueries(){
        return queries;
    }

    public void Read()
    {
        BufferedReader br = null;
        FileReader fr = null;

        try {
            fr = new FileReader(path);
            br = new BufferedReader(fr);

            String sCurrentLine;
            String curQuery = null;
            String content = "";

            while (true) {
                sCurrentLine = br.readLine();
                if (sCurrentLine == null){
                    queries.put(curQuery, content);
                    break;
                }

                sCurrentLine = Utils.StripPunctuationsAndSigns(sCurrentLine);

                String[] parts = sCurrentLine.split("\\s+");

                if (parts[0].equals("*FIND")){
                    if (curQuery != null)
                    {
                        queries.put(curQuery, content);
                    }
                    curQuery = parts[1];
                    content = "";
                }
                else
                {
                    sCurrentLine = sCurrentLine.toLowerCase();
                    if(stem_queries){
                        PorterStemmer stemmer = new PorterStemmer();
                        content += stemmer.stem(sCurrentLine);
                    }
                    else {
                        content += " " + sCurrentLine;
                    }
                }
            }
        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
    }
}
