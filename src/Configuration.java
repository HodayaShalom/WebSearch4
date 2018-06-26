
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Configuration {

    @SuppressWarnings("resource")
    public Configuration(String configFile) throws IOException {
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));

        while ((line = bufferedReader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("trainFile")) {
                train = line.substring(line.indexOf("=") + 1);
            }

            if (line.startsWith("testFile")) {
                test = line.substring(line.indexOf("=") + 1);
            }

            if (line.startsWith("outputFile")) {
                output = line.substring(line.indexOf("=") + 1);
            }

            if (line.startsWith("k")) {
                String k_string = line.substring(line.indexOf("=") + 1);
                k = Integer.parseInt(k_string);
//                if (!mode.equals("basic") && !mode.equals("improved"))
//                    throw new IOException("Mode (Retrieval Algorithm) parameter must be basic/improved");
            }


            if (line.startsWith("trainFolderWeka")) {
                trainFolder = line.substring(line.indexOf("=") + 1);
            }

            if (line.startsWith("testFolderWeka")) {
                testFolder = line.substring(line.indexOf("=") + 1);
            }

//            if (line.startsWith("truthFile")) {
//                truth = line.substring(line.indexOf("=") + 1);
//            }
        }

        bufferedReader.close();
    }

    String train;
    String test;
    String output;
    int k;

    String trainFolder;
    String testFolder;
//    String truth;
}