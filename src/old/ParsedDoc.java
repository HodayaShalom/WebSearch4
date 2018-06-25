package old;

public class ParsedDoc {
    private int id;
    private String content;
    private String date;

    public ParsedDoc(int id, String content, String date) {
        this.id = id;
        this.content = content;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }
}
