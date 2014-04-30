package perf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

abstract class ObjectWriterBase<T1,T2>
{
    protected int hash;

    protected abstract int targetSizeMegs();

    protected void test(ObjectMapper mapper,
            String desc1, T1 inputValue1, Class<? extends T1> inputClass1,
            String desc2, T2 inputValue2, Class<? extends T2> inputClass2)
        throws Exception
    {
        final int REPS;
        {
            final byte[] input1 = mapper.writeValueAsBytes(inputValue1);
            final byte[] input2 = mapper.writeValueAsBytes(inputValue2);
            
            // Let's try to guestimate suitable size, N megs of output
            REPS = (int) ((double) (targetSizeMegs() * 1000 * 1000) / (double) input1.length);
            System.out.printf("Read %d bytes to bind (%d as array); will do %d repetitions\n",
                    input1.length, input2.length, REPS);
        }

        final ObjectWriter writer0 = mapper.writer().with(SerializationFeature.EAGER_SERIALIZER_FETCH);
        final ObjectWriter writer1 = writer0.withType(inputClass1);
        final ObjectWriter writer2 = writer0.withType(inputClass2);
        
        int i = 0;
        int roundsDone = 0;
        final int TYPES = 2;

        // Skip first 5 seconds
        long startMeasure = System.currentTimeMillis() + 5000L;
        
        final long[] times = new long[TYPES];

        System.out.print("Warming up");
        
        while (true) {
            final int round = (i % TYPES);
            final boolean lf = (++i % TYPES) == 0;

            String msg;

            ObjectWriter writer;
            Object value;
            switch (round) {
            case 0:
                msg = desc1;
                writer = writer1;
                value = inputValue1;
                break;
            case 1:
                msg = desc2;
                writer = writer2;
                value = inputValue2;
                break;
            default:
                throw new Error();
            }

            long nanos = testSer(REPS, value, writer);
            long msecs = nanos >> 20; // close enough for comparisons

            // skip first N seconds to let results stabilize
            if (startMeasure > 0L) {
                if ((round != 0) || (System.currentTimeMillis() < startMeasure)) {
                    System.out.print(".");
                    continue;
                }
                startMeasure = 0L;
                System.out.println();
                System.out.println("Starting measurements...");
                Thread.sleep(250L);
                System.out.println();
            }

            times[round] += msecs;

            if ((i % 17) == 0) {
                System.out.println("[GC]");
                Thread.sleep(100L);
                System.gc();
                Thread.sleep(100L);
            }
            
            System.out.printf("Test '%s' [hash: 0x%s] -> %d msecs\n", msg, this.hash, msecs);
            Thread.sleep(50L);
            if (!lf) {
                continue;
            }
            
            if ((++roundsDone % 3) == 0) {
                double den = (double) roundsDone;
                System.out.printf("Averages after %d rounds (%s/%s): %.1f / %.1f msecs\n",
                        roundsDone, desc1, desc2,
                        times[0] / den, times[1] / den);
            }
            System.out.println();
        }
    }

    protected long testSer(int REPS, Object value, ObjectWriter writer) throws Exception
    {
        final NopOutputStream out = new NopOutputStream();
        long start = System.nanoTime();
        while (--REPS >= 0) {
            writer.writeValue(out, value);
        }
        hash = out.size();
        long nanos = System.nanoTime() - start;
        out.close();
        return nanos;
    }
}
