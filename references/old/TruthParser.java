package old;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TruthParser {
    String path;
    Map<String, String[]> truth;

    public TruthParser(String path){
        this.path = path;
        this.truth = new HashMap<>();
    }

    public Map<String, String[]> getTruth(){
        return this.truth;
    }

    public void parse(){
        BufferedReader br = null;
        FileReader fr = null;

        try {

            fr = new FileReader(this.path);
            br = new BufferedReader(fr);

            String sCurrentLine;

            while (true) {
                sCurrentLine = br.readLine();
                if (sCurrentLine == null){
                    break;
                }

                String[] parts = sCurrentLine.split("\\s+");
                //System.out.println(sCurrentLine);
                if (!parts[0].equals("")){
                    String[] docs = new String[parts.length -1];

                    for(int i = 0; i < parts.length -1; i++){
                        docs[i] = parts[i+1];
                    }

                    truth.put(parts[0], docs);
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
