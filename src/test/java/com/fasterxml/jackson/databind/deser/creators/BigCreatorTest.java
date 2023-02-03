package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// Test(s) for "big" creators; ones with at least 32 arguments (sic!).
// Needed because codepaths diverge wrt handling of bitset
public class BigCreatorTest extends BaseMapTest
{
    static class Biggie {
        final int[] stuff;

        @JsonCreator
        public Biggie(
                @JsonProperty("v1") int v1, @JsonProperty("v2") int v2,
                @JsonProperty("v3") int v3, @JsonProperty("v4") int v4,
                @JsonProperty("v5") int v5, @JsonProperty("v6") int v6,
                @JsonProperty("v7") int v7, @JsonProperty("v8") int v8,
                @JsonProperty("v9") int v9, @JsonProperty("v10") int v10,
                @JsonProperty("v11") int v11, @JsonProperty("v12") int v12,
                @JsonProperty("v13") int v13, @JsonProperty("v14") int v14,
                @JsonProperty("v15") int v15, @JsonProperty("v16") int v16,
                @JsonProperty("v17") int v17, @JsonProperty("v18") int v18,
                @JsonProperty("v19") int v19, @JsonProperty("v20") int v20,
                @JsonProperty("v21") int v21, @JsonProperty("v22") int v22,
                @JsonProperty("v23") int v23, @JsonProperty("v24") int v24,
                @JsonProperty("v25") int v25, @JsonProperty("v26") int v26,
                @JsonProperty("v27") int v27, @JsonProperty("v28") int v28,
                @JsonProperty("v29") int v29, @JsonProperty("v30") int v30,
                @JsonProperty("v31") int v31, @JsonProperty("v32") int v32,
                @JsonProperty("v33") int v33, @JsonProperty("v34") int v34,
                @JsonProperty("v35") int v35, @JsonProperty("v36") int v36,
                @JsonProperty("v37") int v37, @JsonProperty("v38") int v38,
                @JsonProperty("v39") int v39, @JsonProperty("v40") int v40
                ) {
            stuff = new int[] {
                    v1, v2, v3, v4, v5, v6, v7, v8, v9, v10,
                    v11, v12, v13, v14, v15, v16, v17, v18, v19, v20,
                    v21, v22, v23, v24, v25, v26, v27, v28, v29, v30,
                    v31, v32, v33, v34, v35, v36, v37, v38, v39, v40,
            };
        }
    }

    private final ObjectReader BIGGIE_READER = objectReader(Biggie.class);

    public void testBigPartial() throws Exception
    {
        Biggie value = BIGGIE_READER.readValue(a2q(
                "{'v7':7, 'v8':8,'v29':29, 'v35':35}"
                ));
        int[] stuff = value.stuff;
        for (int i = 0; i < stuff.length; ++i) {
            int exp;

            switch (i) {
            case 6: // These are off-by-one...
            case 7:
            case 28:
            case 34:
                exp = i+1;
                break;
            default:
                exp = 0;
            }
            assertEquals("Entry #"+i, exp, stuff[i]);
        }
    }
}
