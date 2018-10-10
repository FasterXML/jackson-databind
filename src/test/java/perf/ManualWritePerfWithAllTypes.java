package perf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;

/* Test modified from json-parsers-benchmark, to be able to profile
 * Jackson implementation.
 */
public class ManualWritePerfWithAllTypes
    extends ObjectWriterTestBase<ManualWritePerfWithAllTypes.AllTypes, ManualWritePerfWithAllTypes.AllTypes>
{
    @Override
    protected int targetSizeMegs() { return 15; }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        JsonMapper m = new JsonMapper();
        AllTypes input1 = AllTypes.bigObject();
        AllTypes input2 = AllTypes.bigObject();
        new ManualWritePerfWithAllTypes().test(m,
                "AllTypes/small-1", input1, AllTypes.class,
                "AllTypes/small-2", input2, AllTypes.class);
    }

    @Override
    protected double testSer(int REPS, Object value, ObjectWriter writer) throws Exception
    {
        final NopOutputStream out = new NopOutputStream();
        long start = System.nanoTime();
        byte[] output = null;

        while (--REPS >= 0) {
            output = writer.writeValueAsBytes(value);
        }
        hash = output.length;
        long nanos = System.nanoTime() - start;
        out.close();
        return _msecsFromNanos(nanos);
    }

    // Value type for test
    public static class AllTypes
    {
        enum FooEnum {
            FOO, BAR;
        }

        protected String ignoreMe;

        public String ignoreMe2;
        public String ignoreMe3;

        public int myInt;
        public boolean myBoolean;
        public short myShort;
        public long myLong;
        public String string;
        public String string2;
        public BigDecimal bigDecimal;
        public BigInteger bigInteger;
        public Date date;

        public float myFloat;
        public double myDouble;
        public byte myByte;

        public FooEnum foo;
        public FooEnum bar;

        public long someDate = new Date().getTime ();

        public AllTypes allType;

        public List<AllTypes> allTypes = new ArrayList<AllTypes>();

        static AllTypes _small() {
            AllTypes small = new AllTypes();
            small.ignoreMe = "THIS WILL NOT PASS";
            small.ignoreMe2 = "THIS WILL NOT PASS EITHER";
            small.ignoreMe3 = "THIS WILL NOT PASS TOO";
            small.bigDecimal = new BigDecimal("1.235678900");
            small.date = new Date();
            small.bar = FooEnum.BAR;
            small.foo = FooEnum.FOO;
            small.string = "Hi Mom";
            small.myDouble = 1.2345d;
            small.myFloat = 1.0f;
            small.myShort = (short)1;
            small.myByte = (byte)1;
            return small;
        }

        public static AllTypes smallObject() {
            AllTypes small = _small();
            small.allType = _small();
            small.allType.string = "Hi Dad";
            small.allTypes = Arrays.asList(_small(), _small());
            return small;
        }

        public static AllTypes bigObject() {
            AllTypes big = new AllTypes();
            final List<AllTypes> list = new ArrayList<AllTypes>();
            for (int index = 0; index < 10000; index++) {
                AllTypes item = new AllTypes();

                item.ignoreMe = "THIS WILL NOT PASS";
                item.ignoreMe2 = "THIS WILL NOT PASS EITHER";
                item.ignoreMe3 = "THIS WILL NOT PASS TOO";
                item.bigDecimal = new BigDecimal("1.235678900");
                item.date = new Date ();
                item.bar = FooEnum.BAR;
                item.foo = FooEnum.FOO;
                item.string = "Hi Mom" + System.currentTimeMillis();
                item.myDouble = 1.2345d;
                item.myFloat = 1.0f;
                item.myShort = (short)1;
                item.myByte = (byte)1;

                list.add(item);
            }
            big.allTypes = list;
            return big;
        }
    }
}
