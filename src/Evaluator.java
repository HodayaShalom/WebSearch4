import java.util.Map;
import java.util.HashMap;

public class Evaluator {
    class Evals {
        private int truePositive = 0;
        private int falsePositive = 0;
        private int falseNegative = 0;
        private int trueNegative = 0;

        public void addTP(){
            truePositive++;
        }

        public void addFP(){
            falsePositive++;
        }

        public void addFN(){
            falseNegative++;
        }

        public void addTN(){
            trueNegative++;
        }

        public int getTruePositive(){
            return truePositive;
        }
        public int getFalsePositive(){
            return falsePositive;
        }

        public int getTrueNegative(){
            return trueNegative;
        }

        public int getFalseNegative(){
            return falseNegative;
        }

    }


    Map<Integer, Evals> classesEvals = new HashMap<Integer, Evals>();
    int numOfClasses;

    public Evaluator(int numOfClasses){
        this.numOfClasses = numOfClasses;

        for(int i = 1; i < numOfClasses+1; i++) {
            classesEvals.put(i, new Evals());
        }
    }

    private double calcRecall(double TP, double FN){
        if (TP == (double)0 && FN == (double)0){
            return 0;
        }

        return TP / (TP + FN);
    }
    private double calcRecall(int classVal){
        Evals classEvals = classesEvals.get(classVal);
        double TP = (double)classEvals.getTruePositive();
        double FN = (double)classEvals.getFalseNegative();

        return calcRecall(TP, FN);
    }

    private double calcPrecision(double TP, double FP){
        if (TP == (double)0 && FP == (double)0){
            return 0;
        }

        return TP / (TP + FP);
    }

    private double calcPrecision(int classVal){
        Evals classEvals = classesEvals.get(classVal);

        double TP = (double)classEvals.getTruePositive();
        double FP = (double)classEvals.getFalsePositive();

        return calcPrecision(TP, FP);
    }

    private double calcFScore(double recall, double precision){
        // F = 2PR / P + R
        if (recall == (double)0 && precision == (double)0)
        {
            return 0;
        }

        return 2*recall*precision / (recall + precision);
    }

    private double CalcFscore(int classVal){
        double precision = calcPrecision(classVal);
        double recall = calcRecall(classVal);

        return calcFScore(recall, precision);
    }

    public double CalcMicroAvarage(){
        double tpSum = 0;
        double fpSum =0;
        double fnSum = 0;

        for (Map.Entry<Integer, Evals> entry : classesEvals.entrySet()) {
            tpSum += entry.getValue().getTruePositive();
            fpSum += entry.getValue().getFalsePositive();
            fnSum += entry.getValue().getFalseNegative();
        }

        double precision = calcPrecision(tpSum, fpSum);
        double recall = calcRecall(tpSum, fnSum);

        return calcFScore(recall, precision);
    }
    public double CalcMacroAvarage(){
        double sum = 0;

        for(int i = 1; i < numOfClasses + 1; i++){
            sum += CalcFscore(i);
        }
        return sum/(double)numOfClasses;
    }

    public void Eval(double trueValue, double prediction){
        for (Map.Entry<Integer, Evals> entry : classesEvals.entrySet()) {
            if (entry.getKey() == (int)prediction){
                if(prediction == trueValue){
                    // for predicted class - true positive
                    entry.getValue().addTP();
                }
                else{
                    entry.getValue().addFP();
                }
            }
            else{
                if (entry.getKey() == (int)trueValue){
                    // for all non predicted classes - true negative
                    entry.getValue().addFN();
                }
                else {
                    // for all non predicted classes - true negative
                    entry.getValue().addTN();
                }
            }
        }
    }
}
