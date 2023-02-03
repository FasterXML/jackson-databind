package perf;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape=JsonFormat.Shape.ARRAY)
final class RecordAsArray extends RecordBase
{
    protected RecordAsArray() { super(); }

    public RecordAsArray(int a, String fn, String ln, char g, boolean ins)
    {
        super(a, fn, ln, g, ins);
    }
}
