package perf;

/**
 * Simple test class 
 */
final class Record
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