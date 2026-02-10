package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

/**
 * Test for [databind#5395]: @JsonCreator on Enum supporting both
 * DELEGATING mode and PROPERTIES mode deserialization.
 *<p>
 * The enum has a single @JsonCreator factory method with @JsonProperty("code"),
 * and the user expects it to work with both:
 * <ul>
 *   <li>JSON Object: {"code": 1} (PROPERTIES mode)</li>
 *   <li>Scalar value: 1 (DELEGATING mode)</li>
 * </ul>
 */
public class EnumCreatorDualMode5395Test
{
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonIncludeProperties({"code", "desc"})
    enum ChannelEnum {
        ALIPAY(0, "Alipay"),
        WECHAT(1, "WeChat");

        private final int code;
        private final String desc;

        ChannelEnum(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @JsonProperty("code")
        public int getCode() {
            return code;
        }

        @JsonProperty("desc")
        public String getDesc() {
            return desc;
        }

        @JsonCreator
        private static ChannelEnum ofCode(@JsonProperty("code") String code) {
            if (code == null) {
                return null;
            }
            int codeInt = Integer.parseInt(code);
            for (ChannelEnum ch : values()) {
                if (ch.code == codeInt) {
                    return ch;
                }
            }
            return null;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Test PROPERTIES mode: deserialize from JSON object {"code": 1}
    @Test
    public void testPropertiesMode() throws Exception {
        String json = a2q("{'code': 1}");
        ChannelEnum channel = MAPPER.readValue(json, ChannelEnum.class);
        assertSame(ChannelEnum.WECHAT, channel);
    }

    // Test DELEGATING mode: deserialize from scalar value 1
    @Test
    public void testDelegatingMode() throws Exception {
        String json = "1";
        ChannelEnum channel = MAPPER.readValue(json, ChannelEnum.class);
        assertSame(ChannelEnum.WECHAT, channel);
    }

    // Test with missing "code" property: should return null
    @Test
    public void testNoCodeProperty() throws Exception {
        String json = a2q("{'desc': 'Alipay'}");
        ChannelEnum channel = MAPPER.readValue(json, ChannelEnum.class);
        assertNull(channel);
    }
}
