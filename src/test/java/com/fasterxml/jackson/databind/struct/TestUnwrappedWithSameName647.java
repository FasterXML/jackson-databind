package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.*;

public class TestUnwrappedWithSameName647 extends BaseMapTest
{
    static class UnwrappedWithSamePropertyName {
        public MailHolder mail;
    }

    static class MailHolder {
        @JsonUnwrapped
        public Mail mail;
    }

    static class Mail {
        public String mail;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testUnwrappedWithSamePropertyName() throws Exception {
        final String JSON = "{'mail': {'mail': 'the mail text'}}";
        UnwrappedWithSamePropertyName result = MAPPER.readValue(a2q(JSON), UnwrappedWithSamePropertyName.class);
        assertNotNull(result.mail);
        assertNotNull(result.mail.mail);
        assertEquals("the mail text", result.mail.mail.mail);
    }
}
