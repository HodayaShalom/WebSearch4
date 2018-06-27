

public class ClassificationResult {
    public ClassificationResult(String docId, int trueClass, int predictedClass){
        this.docId = docId;
        this.trueClass = trueClass;
        this.predictedClass = predictedClass;
    }

    int trueClass;
    int predictedClass;
    String docId;
}
