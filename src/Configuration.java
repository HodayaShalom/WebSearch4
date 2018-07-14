
import weka.core.stopwords.Null;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            }

            if (line.startsWith("trainFolderWeka")) {
                trainFolder = line.substring(line.indexOf("=") + 1);
            }

            if (line.startsWith("testFolderWeka")) {
                testFolder = line.substring(line.indexOf("=") + 1);
            }

            if (line.startsWith("recreateWekaDataFolders")) {
                recreateWekaDataFolders = Boolean.valueOf(line.substring(line.indexOf("=") + 1));
            }

        }

        if (train == null){
            System.out.println("ERROR -- missing parameter in parameters file - trainFile");
            System.exit(1);
        }
        if (test == null){
            System.out.println("ERROR -- missing parameter in parameters file - testFile");
            System.exit(1);
        }
        if (output == null){
            System.out.println("ERROR -- missing parameter in parameters file - outputFile");
            System.exit(1);
        }
        if (k < 1){
            System.out.println("ERROR -- wrong value for k. please make sure it is configured correctly");
            System.exit(1);
        }

        if (trainFolder == null){
            Path currentPath = Paths.get(System.getProperty("user.dir"));
            Path filePath = Paths.get(currentPath.toString(), "wekaFolders_temp", "train");


            trainFolder = filePath.toString();

            System.out.println("train weka folder not configured, using: " + trainFolder);
        }
        if (testFolder == null){
            Path currentPath = Paths.get(System.getProperty("user.dir"));
            Path filePath = Paths.get(currentPath.toString(), "wekaFolders_temp", "test");


            testFolder = filePath.toString();

            System.out.println("test weka folder not configured, using: " + testFolder);
        }

        bufferedReader.close();
    }

    String train = null;
    String test = null;
    String output = null;
    int k = 0;

    String trainFolder = null;
    String testFolder = null;
    boolean recreateWekaDataFolders = true;

}