package perf;

import java.util.UUID;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.FromStringDeserializer;
import tools.jackson.databind.json.JsonMapper;

public class ManualReadPerfWithUUID extends ObjectReaderTestBase
{
    static class UUIDNative {
        public UUID[] ids;
        public UUIDNative() { }
        public UUIDNative(UUID[] ids) { this.ids = ids; }
    }

    @Override
    protected int targetSizeMegs() { return 8; }

    static class SlowDeser extends FromStringDeserializer<UUID>
    {
        public SlowDeser() { super(UUID.class); }

        @Override
        protected UUID _deserialize(String id, DeserializationContext ctxt)
        {
            return UUID.fromString(id);
        }
    }
    
    static class UUIDWithJdk {
        @JsonDeserialize(contentUsing=SlowDeser.class)
        public UUID[] ids;
        public UUIDWithJdk() { }
        public UUIDWithJdk(UUID[] ids) { this.ids = ids; }
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        UUID[] uuids = new UUID[8];
        for (int i = 0; i < uuids.length; ++i) {
            uuids[i] = UUID.randomUUID();
        }
        UUIDNative input1 = new UUIDNative(uuids);
        UUIDWithJdk input2 = new UUIDWithJdk(uuids);

        JsonMapper m = new JsonMapper();

        new ManualReadPerfWithRecord().testFromBytes(
                m, "JSON-as-Object", input1, UUIDNative.class,
                m, "JSON-as-Array", input2, UUIDWithJdk.class);
    }
}
