package old;

import opennlp.tools.formats.EvalitaNameSampleStream;
import org.apache.lucene.search.ScoreDoc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Evaluator {
    Map<String, String[]> truth;
    Map<String, String[]> results;

    Map<String, Double> precisions;
    Map<String, Double> recalls;
    Map<String, Double> fmeasures;

    public enum ParameterType {
        PRECISION,
        RECALL,
        FMEASURE;
    }

    public Evaluator(Map<String, String[]> truth, Map<String, String[]> results)
    {
        this.truth = truth;
        this.results = results;

        this.precisions = calcMeasureByParameter(ParameterType.PRECISION);
        this.recalls = calcMeasureByParameter(ParameterType.RECALL);
        this.fmeasures = calcMeasureByParameter(ParameterType.FMEASURE);
    }

    public void printEvaluations() throws IOException {
        // print to console and to file
        String evalPath = "out\\evaluations.txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(evalPath));

        System.out.println("printing evaluation to file: " + evalPath);
        System.out.println("Query\tPrecision\tRecall\tF-Measure");
        for (Map.Entry<String, String[]> entry : this.results.entrySet()) {
            String queryId = entry.getKey();
            String str = queryId + "\t" + this.precisions.get(queryId) + "\t" + this.recalls.get(queryId) + "\t" + this.fmeasures.get(queryId);
            System.out.println(str);
            writer.write(str+ "\n");

        }
        writer.close();
    }

    public HashMap<String, Double> calcMeasureByParameter(ParameterType parameter){
        HashMap<String, Double> measure = new HashMap<>();

        for (Map.Entry<String, String[]> entry : this.results.entrySet()) {
            String queryId = entry.getKey();

            String[] queryTruth = this.truth.get(queryId);
            String[] queryResults = entry.getValue();

            switch (parameter) {
                case PRECISION:
                    measure.put(queryId, calcPrecision(queryResults, queryTruth));
                    break;
                case RECALL:
                    measure.put(queryId, calcRecall(queryResults, queryTruth));
                    break;
                case FMEASURE:
                    measure.put(queryId, calcFMeasure(queryResults, queryTruth));
                    break;
            }
        }

        return measure;
    }

    public double calcAvarageByParameter(ParameterType parameter){
        int totalQueries = this.results.size();
        double totalMeasure = 0;

        for (Map.Entry<String, String[]> entry : this.results.entrySet()) {
            String queryId = entry.getKey();

            String[] queryTruth = this.truth.get(queryId);
            String[] queryResults = entry.getValue();

            switch (parameter) {
                case PRECISION:
                    totalMeasure += calcPrecision(queryResults, queryTruth);
                    break;
                case RECALL:
                    totalMeasure += calcRecall(queryResults, queryTruth);
                    break;
                case FMEASURE:
                    totalMeasure += calcFMeasure(queryResults, queryTruth);
                    break;
            }
        }

        if (totalQueries == 0){
            return 0;
        }

        return totalMeasure / totalQueries;
    }

    public double calcAvaragePrecision(){
        return calcAvarageByParameter(ParameterType.PRECISION);
    }

    public double calcAvarageRecall() {
        return calcAvarageByParameter(ParameterType.RECALL);
    }

    public double calcAvarageFMeasure(){
        return calcAvarageByParameter(ParameterType.FMEASURE);
    }

    public double arraysIntersection(String[] a, String[] b){
        int count = 0;

        for (int i =0; i < a.length; i++){
            for (int j=0; j < b.length; j++){
                if(a[i].equals(b[j])){
                    count++;
                    break;
                }
            }
        }

        return (double)count;
    }

    public double calcPrecision(String[] results, String[] truth){
        if (results.length == 0){
            return 0;
        }

        double precision = arraysIntersection(results, truth) / results.length;
        return precision;
    }

    public double calcRecall(String[] results, String[] truth){
        if (truth.length == 0){
            return 0;
        }
        double recall = arraysIntersection(results, truth) / truth.length;
        return recall;
    }

    public double calcFMeasure(String[] results, String[] truth){
        double precision = calcPrecision(results, truth);
        double recall = calcRecall(results, truth);

        if ((precision + recall) == 0){
            return 0;
        }

        double fMeasure = 2 * ((precision*recall) / (precision + recall));
        return fMeasure;
    }
}
