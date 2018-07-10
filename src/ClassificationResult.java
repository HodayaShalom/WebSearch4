

public class ClassificationResult {
    public ClassificationResult(String docId, int trueClass, int predictedClass){
        this.docId = docId;
        this.trueClass = trueClass;
        this.predictedClass = predictedClass;
    }

    public String getDocId(){
        return docId;
    }

    int trueClass;
    int predictedClass;
    String docId;
}
