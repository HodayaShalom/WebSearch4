import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class offlineScoreCalc {
    public static void main(String[] args){
        String csv_file = args[0];
        int numOfclasses = 14;
        Evaluator evaluator = new Evaluator(numOfclasses);
        read_csv(csv_file, evaluator);

        double micro = evaluator.CalcMicroAvarage();
        double macro = evaluator.CalcMacroAvarage();

        System.out.println("micro: " + micro + " macro: " + macro);
    }

    public static void read_csv(String csvFile, Evaluator evaluator){
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] res = line.split(cvsSplitBy);
                int pred = Integer.parseInt(res[1].trim());
                int truth = Integer.parseInt(res[2].trim());
                evaluator.Eval(truth, pred);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
