package perf;

/**
 * Simple test class 
 */
final class Record extends RecordBase
{
    protected Record() { super(); }
    
    public Record(int a, String fn, String ln, char g, boolean ins)
    {
        super(a, fn, ln, g, ins);
    }
}