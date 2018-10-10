package perf;

import java.io.File;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;

/* Test modified from json-parsers-benchmark, to be able to profile
 * Jackson implementation.
 */
public class ManualWritePerfUntyped
    extends ObjectWriterTestBase<Object,Object>
{
    @Override
    protected int targetSizeMegs() { return 15; }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java [input]");
            System.exit(1);
        }
        JsonMapper mapper = new JsonMapper();
        Map<?,?> stuff = mapper.readValue(new File(args[0]), Map.class);
        new ManualWritePerfUntyped().test(mapper,
                "Untyped-1", stuff, Object.class,
                "Untyped-2", stuff, Object.class);
    }

    @SuppressWarnings("resource")
    @Override
    protected double testSer(int REPS, Object value, ObjectWriter writer) throws Exception
    {
        long start = System.nanoTime();

        // As Bytes
        /*
//        byte[] output = null;
        NopOutputStream out = new NopOutputStream();
        while (--REPS >= 0) {
//            output = writer.writeValueAsBytes(value);
            writer.writeValue(out, value);
        }
        long nanos = System.nanoTime() - start;
        hash = out.size;
        out.close();
        */

        // As String

//        String output = null;
        NopWriter w = new NopWriter();
        while (--REPS >= 0) {
//            output = writer.writeValueAsString(value);
            writer.writeValue(w, value);
        }
        long nanos = System.nanoTime() - start;
//        hash = output.length();
        hash = w.size();

        return _msecsFromNanos(nanos);
    }
}
