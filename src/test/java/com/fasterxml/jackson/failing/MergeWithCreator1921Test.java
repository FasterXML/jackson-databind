package com.fasterxml.jackson.failing;

import java.util.Objects;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

// For [databind#1921]: Creator method not used when merging even if there is
// no feasible alternative. Unclear whether this can even theoretically be
// improved since combination of Creator + Setter(s)/field is legit; but use
// of Creator always means that operation is not true merge.
// But added test just in case future brings us a good idea of way forward.
public class MergeWithCreator1921Test extends BaseMapTest
{
    static class Account {
        @JsonMerge(value = OptBoolean.TRUE)
        private final Validity validity;

        @JsonCreator
        public Account(@JsonProperty(value = "validity", required = true) Validity validity) {
            this.validity = validity;
        }

        public Validity getValidity() {
            return validity;
        }
    }

    static class Validity {
        public static final String VALID_FROM_CANT_BE_NULL = "Valid from can't be null";
        public static final String VALID_TO_CANT_BE_BEFORE_VALID_FROM = "Valid to can't be before valid from";

        private final String _validFrom;
        private final String _validTo;

        @JsonCreator
        public Validity(@JsonProperty(value = "validFrom", required = true) String validFrom,
                        @JsonProperty("validTo") String validTo) {
            checkValidity(validFrom, validTo);

            this._validFrom = validFrom;
            this._validTo = validTo;
        }

        private void checkValidity(String from, String to) {
            Objects.requireNonNull(from, VALID_FROM_CANT_BE_NULL);
            if (to != null) {
                if (from.compareTo(to) > 0) {
                    throw new IllegalStateException(VALID_TO_CANT_BE_BEFORE_VALID_FROM);
                }
            }
        }

        public String getValidFrom() {
            return _validFrom;
        }

        public String getValidTo() {
            return _validTo;
        }
    }

    public void testMergeWithCreator() throws Exception
    {
        final String JSON = "{ \"validity\": { \"validFrom\": \"2018-02-01\", \"validTo\": \"2018-01-31\" } }";

        final ObjectMapper mapper = newJsonMapper();

        try {
            mapper.readValue(JSON, Account.class);
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, Validity.VALID_TO_CANT_BE_BEFORE_VALID_FROM);
        }

        try {
            Account acc = new Account(new Validity("abc", "def"));
            mapper.readerForUpdating(acc)
                .readValue(JSON);
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, Validity.VALID_TO_CANT_BE_BEFORE_VALID_FROM);
        }
    }
}
