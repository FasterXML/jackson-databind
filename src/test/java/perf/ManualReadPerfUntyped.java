package perf;

import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

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
                .configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, doIntern)
                .configure(JsonFactory.Feature.INTERN_FIELD_NAMES, doIntern)
                .build();

        JsonMapper m = new JsonMapper(f);

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
