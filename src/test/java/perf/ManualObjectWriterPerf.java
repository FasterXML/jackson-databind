package perf;

import com.fasterxml.jackson.databind.*;

public class ManualObjectWriterPerf
{
    protected int hash;
    
    private <T> void test(ObjectMapper mapper, T inputValue, Class<T> inputClass)
        throws Exception
    {
        final int REPS;
        {
            final byte[] input = mapper.writeValueAsBytes(inputValue);
            
            // Let's try to guestimate suitable size, N megs of output
            REPS = (int) ((double) (9 * 1000 * 1000) / (double) input.length);
            System.out.println("Read "+input.length+" bytes to hash; will do "+REPS+" repetitions");
        }

        final ObjectWriter prefetching = mapper.writerWithType(inputClass);
        final ObjectWriter nonPrefetching = mapper.writer()
                .without(SerializationFeature.EAGER_SERIALIZER_FETCH)
                .withType(inputClass);
        
        int i = 0;
        int roundsDone = 0;
        final int TYPES = 2;
        final int WARMUP_ROUNDS = 5;

        final long[] times = new long[TYPES];

        while (true) {
            final NopOutputStream out = new NopOutputStream();
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % TYPES);

            String msg;
            boolean lf = (round == 0);

            long msecs;
            ObjectWriter writer;
            
            switch (round) {
            case 0:
                msg = "Pre-fetch";
                writer = prefetching;
                break;
            case 1:
                msg = "NO pre-fetch";
                writer = nonPrefetching;
                break;
            default:
                throw new Error();
            }
            msecs = testSer(REPS, inputValue, writer, out);

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

    private final long testSer(int REPS, Object value, ObjectWriter writer, NopOutputStream out) throws Exception
    {
        long start = System.currentTimeMillis();
        while (--REPS >= 0) {
            writer.writeValue(out, value);
        }
        hash = out.size();
        return System.currentTimeMillis() - start;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        Record input = new Record(44, "BillyBob", "Bumbler", 'm', true);
        ObjectMapper m = new ObjectMapper();
        new ManualObjectWriterPerf().test(m, input, Record.class);
    }
}
