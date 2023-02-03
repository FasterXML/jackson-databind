package perf;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class ManualReadPerfUntypedStream extends ObjectReaderTestBase
{
    @Override
    protected int targetSizeMegs() { return 15; }

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
        Object input1 = m.readValue(data, Object.class);
        JsonNode input2 = m.readTree(data);

        new ManualReadPerfUntypedStream()
              .testFromBytes(
//            .testFromString(
                m, "JSON-as-Object", input1, Object.class
                ,m, "JSON-as-Object2", input2, Object.class
//               ,m, "JSON-as-Node", input2, JsonNode.class
                );
    }

    @Override
    protected double testDeser1(int reps, byte[] input, ObjectReader reader) throws IOException {
        return _testRawDeser(reps, input, reader);
    }

    @Override
    protected double testDeser2(int reps, byte[] input, ObjectReader reader) throws IOException {
        return _testRawDeser(reps, input, reader);
    }

    protected final double _testRawDeser(int reps, byte[] json, ObjectReader reader) throws IOException
    {
        long start = System.nanoTime();
        final JsonFactory f = reader.getFactory();
        while (--reps >= 0) {
            JsonParser p = f.createParser(new ByteArrayInputStream(json));
            JsonToken t;
            while ((t = p.nextToken()) != null) {
                if (t == JsonToken.VALUE_STRING) {
                    p.getText();
                } else if (t.isNumeric()) {
                    p.getNumberValue();
                }
                ;
            }
            p.close();
        }
        hash = f.hashCode();
        return _msecsFromNanos(System.nanoTime() - start);
    }
}
