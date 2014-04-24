package perf;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

public class ManualReadPerfUntyped extends ObjectReaderBase
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java [input]");
            System.exit(1);
        }
        byte[] data = readAll(args[0]);

        JsonFactory f = new JsonFactory();
        boolean doIntern = true;

        f.configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, doIntern);
        f.configure(JsonFactory.Feature.INTERN_FIELD_NAMES, doIntern);
        
        ObjectMapper m = new ObjectMapper();
        Object input1 = m.readValue(data, Object.class);
        JsonNode input2 = m.readTree(data);

        new ManualReadPerfUntyped()
//            .testFromBytes(
            .testFromString(
                m, "JSON-as-Object", input1, Object.class
                ,m, "JSON-as-Object2", input2, Object.class
//               ,m, "JSON-as-Node", input2, JsonNode.class
                );
    }
}
