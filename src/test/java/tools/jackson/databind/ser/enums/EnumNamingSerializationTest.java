package tools.jackson.databind.ser.enums;

import java.util.EnumMap;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.EnumNaming;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class EnumNamingSerializationTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    enum EnumFlavorA {
        CHOCOLATE_CHIPS,
        HOT_CHEETOS;

        @Override
        public String toString() {
            return "HOT_CHOCOLATE_CHEETOS_AND_CHIPS";
        }
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    enum EnumSauceB {
        KETCH_UP,
        MAYO_NEZZ;
    }

    @EnumNaming(EnumNamingStrategy.class)
    enum EnumSauceC {
        BARBEQ_UE,
        SRIRACHA_MAYO;
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    enum EnumFlavorD {
        _PEANUT_BUTTER,
        PEANUT__BUTTER,
        PEANUT_BUTTER
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    enum EnumFlavorE {
        PEANUT_BUTTER,
        @JsonProperty("almond")
        ALMOND_BUTTER
    }

    static class EnumFlavorWrapperBean {
        public EnumSauceB sauce;

        @JsonCreator
        public EnumFlavorWrapperBean(@JsonProperty("sce") EnumSauceB sce) {
            this.sauce = sce;
        }
    }

    /*
    /**********************************************************
    /* Test
    /**********************************************************
    */

    @Test
    public void enumNamingShouldOverrideToStringFeature() throws Exception {
        String resultStr = MAPPER.writer()
            .with(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
            .writeValueAsString(EnumFlavorA.CHOCOLATE_CHIPS);

        // 26-Nov-2025, tatu: Before 3.1, test assumed that "WRITE_ENUMS_USING_TO_STRING"
        //    prevents use of EnumNamingStrategy -- not so with 3.1 and later
        assertEquals(q("hotChocolateCheetosAndChips"), resultStr);
    }

    @Test
    public void enumNamingStrategyNotApplied() throws Exception {
        String resultString = MAPPER.writeValueAsString(EnumSauceC.SRIRACHA_MAYO);
        assertEquals(q("SRIRACHA_MAYO"), resultString);
    }

    @Test
    public void enumNamingStrategyStartingUnderscoreBecomesUpperCase() throws Exception {
        String flavor = MAPPER.writeValueAsString(EnumFlavorD._PEANUT_BUTTER);
        assertEquals(q("PeanutButter"), flavor);
    }

    @Test
    public void enumNamingStrategyNonPrefixContiguousUnderscoresBecomeOne() throws Exception {
        String flavor1 = MAPPER.writeValueAsString(EnumFlavorD.PEANUT__BUTTER);
        assertEquals(q("peanutButter"), flavor1);

        String flavor2 = MAPPER.writeValueAsString(EnumFlavorD.PEANUT_BUTTER);
        assertEquals(q("peanutButter"), flavor2);
    }

    @Test
    public void enumSetWrite() throws Exception {
        final EnumSet<EnumSauceB> value = EnumSet.of(EnumSauceB.KETCH_UP);
        assertEquals("[\"ketchUp\"]", MAPPER.writeValueAsString(value));
    }

    @Test
    public void enumMapWrite() throws Exception {
        EnumMap<EnumSauceB, String> enums = new EnumMap<>(EnumSauceB.class);
        enums.put(EnumSauceB.MAYO_NEZZ, "value");

        String str = MAPPER.writer()
                .without(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .writeValueAsString(enums);

        assertEquals(a2q("{'mayoNezz':'value'}"), str);
    }

    @Test
    public void enumNamingStrategyWithOverride() throws Exception {
        String almond = MAPPER.writeValueAsString(EnumFlavorE.ALMOND_BUTTER);
        assertEquals(q("almond"), almond);

        String peanut = MAPPER.writeValueAsString(EnumFlavorE.PEANUT_BUTTER);
        assertEquals(q("peanutButter"), peanut);
    }
}
