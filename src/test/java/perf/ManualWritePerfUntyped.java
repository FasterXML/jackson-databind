package perf;

import java.io.File;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/* Test modified from json-parsers-benchmark, to be able to profile
 * Jackson implementation.
 */
public class ManualWritePerfUntyped
    extends ObjectWriterBase<Object,Object>
{
    @Override
    protected int targetSizeMegs() { return 15; }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java [input]");
            System.exit(1);
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<?,?> stuff = mapper.readValue(new File(args[0]), Map.class);
        new ManualWritePerfUntyped().test(mapper,
                "AllTypes/small-1", stuff, Object.class,
                "AllTypes/small-2", stuff, Object.class);
    }

    @Override
    protected long testSer(int REPS, Object value, ObjectWriter writer) throws Exception
    {
        final NopOutputStream out = new NopOutputStream();
        long start = System.nanoTime();
        byte[] output = null;

        while (--REPS >= 0) {
            output = writer.writeValueAsBytes(value);
        }
        hash = output.length;
        long nanos = System.nanoTime() - start;
        out.close();
        return nanos;
    }
}
