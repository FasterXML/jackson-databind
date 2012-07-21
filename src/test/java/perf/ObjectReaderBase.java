package perf;

import com.fasterxml.jackson.databind.*;

abstract class ObjectReaderBase
{
    protected int hash;

    protected <T1, T2> void test(ObjectMapper mapper1, String desc1,
            T1 inputValue1, Class<T1> inputClass1,
            ObjectMapper mapper2, String desc2,
            T2 inputValue2, Class<T2> inputClass2)
        throws Exception
    {
        final byte[] byteInput1 = mapper1.writeValueAsBytes(inputValue1);
        final byte[] byteInput2 = mapper2.writeValueAsBytes(inputValue2);

        desc1 = String.format("%s (%d bytes)", desc1, byteInput1.length);
        desc2 = String.format("%s (%d bytes)", desc2, byteInput2.length);

        // sanity check:
        {
            /*T1 back1 =*/ mapper1.readValue(byteInput1, inputClass1);
            /*T2 back2 =*/ mapper2.readValue(byteInput2, inputClass2);
            System.out.println("Input successfully round-tripped for both styles...");
        }

        // Let's try to guestimate suitable size... to get to N megs to process
        final int REPS = (int) ((double) (8 * 1000 * 1000) / (double) byteInput1.length);

        System.out.printf("Read %d bytes to bind (%d as array); will do %d repetitions\n",
                byteInput1.length, byteInput2.length, REPS);

        final ObjectReader jsonReader = mapper1.reader()
                .with(DeserializationFeature.EAGER_DESERIALIZER_FETCH)
                .withType(inputClass1);
        final ObjectReader arrayReader = mapper2.reader()
                .with(DeserializationFeature.EAGER_DESERIALIZER_FETCH)
                .withType(inputClass2);
        
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
                msg = desc1;
                msecs = testDeser(REPS, byteInput1, jsonReader);
                break;
            case 1:
                msg = desc2;
                msecs = testDeser(REPS, byteInput2, arrayReader);
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
}
