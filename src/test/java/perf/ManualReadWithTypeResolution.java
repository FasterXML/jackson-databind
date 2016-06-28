package perf;

import java.io.*;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

/**
 * Variant that uses hard-coded input but compares cost of generic type
 * resolution to that of passing raw (type-erased) Class.
 *
 * @since 2.8
 */
public class ManualReadWithTypeResolution
{
    protected int hash;
    // wait for 3 seconds
    protected long startMeasure = System.currentTimeMillis() + 3000L;
    protected int roundsDone = 0;
    protected int REPS;
    private double[] timeMsecs;

    protected String _desc1, _desc2;
    
    public static void main(String[] args) throws Exception
    {
//        byte[] data = "{\"id\":124}".getBytes("UTF-8");
        byte[] data = "{\"key\":\"value\",\"id\":124}".getBytes("UTF-8");
        ObjectMapper m = new ObjectMapper();
        new ManualReadWithTypeResolution().doTest(m, data);
    }

    // When comparing to simple streaming parsing, uncomment:

    private void doTest(ObjectMapper mapper, byte[] byteInput)
        throws Exception
    {
        REPS = (int) ((double) (15 * 1000 * 1000) / (double) byteInput.length);
        _desc1 = "Raw type";
        _desc2 = "Generic type";

        System.out.printf("Read %d bytes to bind; will do %d repetitions\n",
                byteInput.length, REPS);
        System.out.print("Warming up");

        int i = 0;
        final int TYPES = 2;


        timeMsecs = new double[TYPES];
        final TypeReference<?> typeRef = new TypeReference<Map<String,Object>>() { };
        final JavaType typeResolved = mapper.getTypeFactory().constructType(typeRef);

        while (true) {
            Thread.sleep(100L);
            final int type = (i++ % TYPES);

            String msg;
            double msesc;
            
            switch (type) {
            case 0:
                msesc = testDeser(REPS, byteInput, mapper, Map.class);
                msg = _desc1;
                break;
            case 1:
                msesc = testDeser(REPS, byteInput, mapper, typeRef);
                msg = _desc2;
                break;
            default:
                throw new Error();
            }
            updateStats(type, (i % 17) == 0, msg, msesc);
        }
    }

    protected final double testDeser(int reps, byte[] json, ObjectMapper mapper, Class<?> type)
            throws IOException
    {
        long start = System.nanoTime();
        Object result = null;
        while (--reps >= 0) {
            result = mapper.readValue(json, type);
        }
        hash = result.hashCode();
        return _msecsFromNanos(System.nanoTime() - start);
    }

    protected final double testDeser(int reps, byte[] json, ObjectMapper mapper, TypeReference<?> type)
            throws IOException
    {
        long start = System.nanoTime();
        Object result = null;
        while (--reps >= 0) {
            result = mapper.readValue(json, type);
        }
        hash = result.hashCode();
        return _msecsFromNanos(System.nanoTime() - start);
    }
    
    private void updateStats(int type, boolean doGc, String msg, double msecs)
        throws Exception
    {
        final boolean lf = (type == (timeMsecs.length - 1));

        if (startMeasure == 0L) { // skip first N seconds
            timeMsecs[type] += msecs;
        } else {
            if (lf) {
                if (System.currentTimeMillis() >= startMeasure) {
                    startMeasure = 0L;
                    System.out.println(" complete!");
                } else {
                    System.out.print(".");
                }
            }
            return;
        }

        System.out.printf("Test '%s' [hash: 0x%s] -> %.1f msecs\n", msg, Integer.toHexString(hash), msecs);
        if (lf) {
            ++roundsDone;
            if ((roundsDone % 3) == 0 ) {
                double den = (double) roundsDone;
                System.out.printf("Averages after %d rounds (%s/%s): %.1f / %.1f msecs\n",
                        (int) den, _desc1, _desc2,
                        timeMsecs[0] / den, timeMsecs[1] / den);
            }
            System.out.println();
        }
        if (doGc) {
            System.out.println("[GC]");
            Thread.sleep(100L);
            System.gc();
            Thread.sleep(100L);
        }
    }

    protected final double _msecsFromNanos(long nanos) {
        return (nanos / 1000000.0);
    }
}
