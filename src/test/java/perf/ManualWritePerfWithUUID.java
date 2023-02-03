package perf;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

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

@SuppressWarnings("serial")
class SlowSer extends StdScalarSerializer<UUID>
{
    public SlowSer() { super(UUID.class); }

    @Override
    public void serialize(UUID value, JsonGenerator jgen,
            SerializerProvider provider) throws IOException {
        jgen.writeString(value.toString());
    }
}
