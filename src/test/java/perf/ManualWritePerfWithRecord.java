package perf;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ManualWritePerfWithRecord
    extends ObjectWriterBase<Record, RecordAsArray>
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        Record input1 = new Record(44, "BillyBob", "Bumbler", 'm', true);
        RecordAsArray input2 = new RecordAsArray(44, "BillyBob", "Bumbler", 'm', true);
        ObjectMapper m = new ObjectMapper();
        new ManualWritePerfWithRecord().test(m,
                "JSON-as-Object", input1, Record.class,
                "JSON-as-Array", input2, RecordAsArray.class);
              
        
    }
}
