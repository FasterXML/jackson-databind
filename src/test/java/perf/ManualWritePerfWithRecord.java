package perf;

import com.fasterxml.jackson.databind.json.JsonMapper;

public class ManualWritePerfWithRecord
    extends ObjectWriterTestBase<Record, RecordAsArray>
{
    @Override
    protected int targetSizeMegs() { return 10; }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        Record input1 = new Record(44, "BillyBob", "Bumbler", 'm', true);
        RecordAsArray input2 = new RecordAsArray(44, "BillyBob", "Bumbler", 'm', true);
        JsonMapper m = new JsonMapper();
        new ManualWritePerfWithRecord().test(m,
                "JSON-as-Object", input1, Record.class,
                "JSON-as-Array", input2, RecordAsArray.class);
    }
}
