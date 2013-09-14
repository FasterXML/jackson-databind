package perf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

abstract class ObjectWriterBase<T1,T2>
{
    protected int hash;
    
    protected void test(ObjectMapper mapper,
            String desc1, T1 inputValue1, Class<T1> inputClass1,
            String desc2, T2 inputValue2, Class<T2> inputClass2)
        throws Exception
    {
        final int REPS;
        {
            final byte[] input1 = mapper.writeValueAsBytes(inputValue1);
            final byte[] input2 = mapper.writeValueAsBytes(inputValue2);
            
            // Let's try to guestimate suitable size, N megs of output
            REPS = (int) ((double) (9 * 1000 * 1000) / (double) input1.length);
            System.out.printf("Read %d bytes to bind (%d as array); will do %d repetitions\n",
                    input1.length, input2.length, REPS);
        }

        final ObjectWriter writer0 = mapper.writer().with(SerializationFeature.EAGER_SERIALIZER_FETCH);
        final ObjectWriter jsonWriter = writer0.withType(inputClass1);
        final ObjectWriter arrayWriter = writer0.withType(inputClass2);
        
        int i = 0;
        int roundsDone = 0;
        final int TYPES = 2;
        final int WARMUP_ROUNDS = 5;

        final long[] times = new long[TYPES];

        while (true) {
            @SuppressWarnings("resource")
            final NopOutputStream out = new NopOutputStream();
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % TYPES);

            String msg;
            boolean lf = (round == 0);

            long msecs;
            ObjectWriter writer;
            Object value;
            switch (round) {
            case 0:
                msg = desc1;
                writer = jsonWriter;
                value = inputValue1;
                break;
            case 1:
                msg = desc2;
                writer = arrayWriter;
                value = inputValue2;
                break;
            default:
               out.close(); // silly eclipse juno
                throw new Error();
            }
            msecs = testSer(REPS, value, writer, out);

            // skip first 5 rounds to let results stabilize
            if (roundsDone >= WARMUP_ROUNDS) {
                times[round] += msecs;
            }
            
            System.out.printf("Test '%s' [hash: 0x%s] -> %d msecs\n", msg, this.hash, msecs);
            if (lf) {
                ++roundsDone;
                if ((roundsDone % 3) == 0 && roundsDone > WARMUP_ROUNDS) {
                    double den = (double) (roundsDone - WARMUP_ROUNDS);
                    System.out.printf("Averages after %d rounds (pre / no): %.1f / %.1f msecs\n",
                            (int) den,
                            times[0] / den, times[1] / den);
                            
                }
                System.out.println();
            }
            if ((i % 17) == 0) {
                System.out.println("[GC]");
                Thread.sleep(100L);
                System.gc();
                Thread.sleep(100L);
            }
        }
    }

    protected final long testSer(int REPS, Object value, ObjectWriter writer, NopOutputStream out) throws Exception
    {
        long start = System.currentTimeMillis();
        while (--REPS >= 0) {
            writer.writeValue(out, value);
        }
        hash = out.size();
        return System.currentTimeMillis() - start;
    }
}
