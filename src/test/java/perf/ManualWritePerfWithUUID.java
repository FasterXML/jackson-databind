package perf;

import java.util.UUID;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.StdScalarSerializer;

public class ManualWritePerfWithUUID
    extends ObjectWriterTestBase<UUIDFast, UUIDSlow>
{
    @Override
    protected int targetSizeMegs() { return 10; }
    
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
        new ManualWritePerfWithUUID().test(new JsonMapper(),
                "faster-UUID", new UUIDFast(uuids), UUIDFast.class,
                "JDK-UUID", new UUIDSlow(uuids), UUIDSlow.class);
    }
}

class UUIDFast
{
    public final UUID[] values;

    public UUIDFast(UUID[] v) { values = v; }
}

class UUIDSlow
{
    @JsonSerialize(contentUsing=SlowSer.class)
    public final UUID[] values;
    
    public UUIDSlow(UUID[] v) { values = v; }
}

class SlowSer extends StdScalarSerializer<UUID>
{
    public SlowSer() { super(UUID.class); }

    @Override
    public void serialize(UUID value, JsonGenerator jgen,
            SerializerProvider provider) {
        jgen.writeString(value.toString());
    }
}
