package perf;

import com.fasterxml.jackson.databind.*;

/**
 * Simple manually run micro-benchmark for checking effects of (de)serializer
 * pre-fetching
 */
public class ManualObjectReaderPerf
{
    protected int hash;
    
    private <T> void test(ObjectMapper mapper, T inputValue, Class<T> inputClass)
        throws Exception
    {
        final byte[] input = mapper.writeValueAsBytes(inputValue);
        
        // Let's try to guestimate suitable size... to get to 6 megs to process
        final int REPS = (int) ((double) (6 * 1000 * 1000) / (double) input.length);

        System.out.println("Read "+input.length+" bytes to hash; will do "+REPS+" repetitions");

        final ObjectReader cachingReader = mapper.reader(inputClass);
        final ObjectReader nonCachingReader = mapper.reader()
                .without(DeserializationFeature.EAGER_DESERIALIZER_FETCH)
                .withType(inputClass);
        
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
                msg = "Pre-fetch";
                msecs = testDeser(REPS, input, cachingReader);
                break;
            case 1:
                msg = "NO pre-fetch";
                msecs = testDeser(REPS, input, nonCachingReader);
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
        ObjectMapper m = new ObjectMapper();
        new ManualObjectReaderPerf().test(m, input, Record.class);
    }
    
    /**
     * Simple test class 
     */
    protected final static class Record
    {
        public int age;
        public String firstName, lastName;
        public char gender;
        public boolean insured;

        public Record() { }
        public Record(int a, String fn, String ln, char g, boolean ins)
        {
            age = a;
            firstName = fn;
            lastName = ln;
            gender = g;
            insured = ins;
        }
    }
}
