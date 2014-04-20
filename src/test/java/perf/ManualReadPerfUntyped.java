package perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ManualReadPerfUntyped extends ObjectReaderBase
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java [input]");
            System.exit(1);
        }
        byte[] data = readAll(args[0]);

        ObjectMapper m = new ObjectMapper();
        Object input1 = m.readValue(data, Object.class);
        JsonNode input2 = m.readTree(data);

        new ManualReadPerfUntyped().test(
                m, "JSON-as-Object", input1, Object.class,
                m, "JSON-as-Array", input2, JsonNode.class);
    }

}
