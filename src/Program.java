//import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

public class Program {

    public static void main(String[] args) throws IOException , Exception{
        String configFile = args[0];
        Configuration config = new Configuration(configFile);
        Logic logic = new Logic(config);
        logic.run();
    }
}