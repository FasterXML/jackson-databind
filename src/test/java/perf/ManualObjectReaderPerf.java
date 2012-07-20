package perf;

import com.fasterxml.jackson.databind.*;

/**
 * Simple manually run micro-benchmark for checking effects of (de)serializer
 * pre-fetching
 */
public class ManualObjectReaderPerf
{
    protected int hash;
    
    private <T1, T2> void test(ObjectMapper mapper,
            T1 inputValue1, Class<T1> inputClass1,
            T2 inputValue2, Class<T2> inputClass2)
        throws Exception
    {
        final byte[] jsonInput = mapper.writeValueAsBytes(inputValue1);
        final byte[] arrayInput = mapper.writeValueAsBytes(inputValue2);
        
        // Let's try to guestimate suitable size... to get to N megs to process
        final int REPS = (int) ((double) (8 * 1000 * 1000) / (double) jsonInput.length);

        System.out.printf("Read %d bytes to bind (%d as array); will do %d repetitions\n",
                jsonInput.length, arrayInput.length, REPS);

        final ObjectReader reader0 = mapper.reader()
                .with(DeserializationFeature.EAGER_DESERIALIZER_FETCH);

        final ObjectReader jsonReader = reader0.withType(inputClass1);
        final ObjectReader arrayReader = reader0.withType(inputClass2);
        
        int i = 0;
        int roundsDone = 0;
        final int TYPES = 2;
        final int WARMUP_ROUNDS = 5;

        final long[] times = new long[TYPES];
        
        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % TYPES);

            String msg;
            boolean lf = (round == 0);

            long msecs;
            
            switch (round) {
            case 0:
                msg = "JSON-as-Object";
                msecs = testDeser(REPS, jsonInput, jsonReader);
                break;
            case 1:
                msg = "JSON-as-Array";
                msecs = testDeser(REPS, arrayInput, arrayReader);
                break;
            default:
                throw new Error();
            }
            // skip first 5 rounds to let results stabilize
            if (roundsDone >= WARMUP_ROUNDS) {
                times[round] += msecs;
            }
            
            System.out.printf("Test '%s' [hash: 0x%s] -> %d msecs\n", msg, this.hash, msecs);
            if (lf) {
                ++roundsDone;
                if ((roundsDone % 3) == 0 && roundsDone > WARMUP_ROUNDS) {
                    double den = (double) (roundsDone - WARMUP_ROUNDS);
                    System.out.printf("Averages after %d rounds (Object / Array): %.1f / %.1f msecs\n",
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

    private final long testDeser(int REPS, byte[] input, ObjectReader reader) throws Exception
    {
        long start = System.currentTimeMillis();
        Object result = null;
        while (--REPS >= 0) {
            result = reader.readValue(input);
        }
        hash = result.hashCode();
        return System.currentTimeMillis() - start;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        Record input = new Record(44, "BillyBob", "Bumbler", 'm', true);
        RecordAsArray input2 = new RecordAsArray(44, "BillyBob", "Bumbler", 'm', true);
        ObjectMapper m = new ObjectMapper();
        new ManualObjectReaderPerf().test(m,
                input, Record.class,
                input2, RecordAsArray.class);
    }
}
