package perf;

import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Simple manually run micro-benchmark for checking effects of (de)serializer
 * pre-fetching
 */
public class ManualReadPerfWithRecord extends ObjectReaderTestBase
{
    @Override
    protected int targetSizeMegs() { return 8; }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        Record input = new Record(44, "BillyBob", "Bumbler", 'm', true);
        RecordAsArray input2 = new RecordAsArray(44, "BillyBob", "Bumbler", 'm', true);
        JsonMapper m = new JsonMapper();
        new ManualReadPerfWithRecord().testFromBytes(m, "JSON-as-Object", input, Record.class,
                m, "JSON-as-Array", input2, RecordAsArray.class);
    }
}
