package perf;

public class RecordBase
{
    public int age;
    public String firstName, lastName;
    public char gender;
    public boolean insured;

    protected RecordBase() { }
    protected RecordBase(int a, String fn, String ln, char g, boolean ins)
    {
        age = a;
        firstName = fn;
        lastName = ln;
        gender = g;
        insured = ins;
    }
}
