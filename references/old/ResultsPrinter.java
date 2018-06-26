package old;

import java.io.*;
import java.io.IOException;
import java.util.*;

public class ResultsPrinter {

    Map<String, String[]> results;
    String result_path;

    public ResultsPrinter(Map<String, String[]> results, String result_path) {
        this.results = results;
        this.result_path = result_path;
    }

    public void print() throws IOException  {
        print_to_file();
    }

    private void print_to_file() throws IOException {
        System.out.println("writing results to file. path=" + this.result_path);
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.result_path));
        for (Map.Entry<String, String[]> entry : results.entrySet()) {
            String[] hits_ids = entry.getValue();

            String str = entry.getKey() + " ";

            for (int i = 0; i < hits_ids.length; ++i) {
                str += hits_ids[i] + " ";
            }
            str += "\n";

            writer.write(str);
        }
        writer.close();
    }

    private void print_to_console() throws IOException {
        for (Map.Entry<String, String[]> entry : results.entrySet()) {
            String[] hits_ids = entry.getValue();

            System.out.print(entry.getKey() + " ");
            for (int i = 0; i < hits_ids.length; ++i) {
                System.out.print(hits_ids[i] + " ");
            }
            System.out.println();
        }
    }

}
