package perf;

import java.util.Map;

import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.databind.*;

public class ManualReadPerfUntyped extends ObjectReaderTestBase
{
    @Override
    protected int targetSizeMegs() { return 10; }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java [input]");
            System.exit(1);
        }
        byte[] data = readAll(args[0]);

        boolean doIntern = true;

        JsonFactory f = JsonFactory.builder()
            .set(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, doIntern)
            .set(JsonFactory.Feature.INTERN_FIELD_NAMES, doIntern)
            .build();
        
        ObjectMapper m = new ObjectMapper(f);
        
        // Either Object or Map
        final Class<?> UNTYPED = Map.class;
        
        Object input1 = m.readValue(data, UNTYPED);
        JsonNode input2 = m.readTree(data);

        new ManualReadPerfUntyped()
            .testFromBytes(
//            .testFromString(
                m, "JSON-as-Object", input1, UNTYPED
                ,m, "JSON-as-Object2", input2, UNTYPED
//               ,m, "JSON-as-Node", input2, JsonNode.class
                );
    }
}
