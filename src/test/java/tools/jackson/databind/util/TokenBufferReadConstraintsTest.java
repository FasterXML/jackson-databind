package tools.jackson.databind.util;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests to verify that {@link StreamReadConstraints} a {@link TokenBuffer} was
 * built with are actually applied when buffered contents are replayed: they need
 * to be used by token-count validation inherited from {@code ParserMinimalBase},
 * and not just by the {@code streamReadConstraints()} accessor.
 */
public class TokenBufferReadConstraintsTest extends DatabindTestUtil
{
    private final static int MAX_TOKENS = 4;

    private final JsonFactory CONSTRAINED_F = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxTokenCount(MAX_TOKENS).build())
            .build();

    // 10 tokens: START_ARRAY, 8 x VALUE_NUMBER_INT, END_ARRAY
    private TokenBuffer _constrainedBuffer() throws Exception
    {
        // Parser only used as carrier of constraints; never advanced, so that
        // only replay is measured against the limit
        try (JsonParser p = CONSTRAINED_F.createParser(ObjectReadContext.empty(), "0")) {
            TokenBuffer buf = new TokenBuffer(p, null);
            buf.writeStartArray();
            for (int i = 0; i < 8; ++i) {
                buf.writeNumber(i);
            }
            buf.writeEndArray();
            return buf;
        }
    }

    @Test
    void constraintsRetainedByAsParser() throws Exception
    {
        try (JsonParser p = _constrainedBuffer().asParser()) {
            assertEquals(MAX_TOKENS, p.streamReadConstraints().getMaxTokenCount());
        }
    }

    @Test
    void tokenCountEnforcedOnReplay() throws Exception
    {
        // Before fix: `ParserMinimalBase` got its (final) constraints from
        // `ObjectReadContext.empty()` -- that is, defaults, with no token limit --
        // so replay was effectively unconstrained
        try (JsonParser p = _constrainedBuffer().asParser()) {
            while (p.nextToken() != null) { }
            fail("Should not pass; should fail with token count limit of "+MAX_TOKENS);
        } catch (StreamConstraintsException e) {
            verifyException(e, "Token count");
        }
    }

    @Test
    void defaultConstraintsAllowSmallBuffer() throws Exception
    {
        // Sanity check: buffer with default constraints must not fail on a tiny doc
        TokenBuffer buf = new TokenBuffer(false);
        buf.writeStartArray();
        for (int i = 0; i < 8; ++i) {
            buf.writeNumber(i);
        }
        buf.writeEndArray();
        try (JsonParser p = buf.asParser()) {
            int count = 0;
            while (p.nextToken() != null) { ++count; }
            assertEquals(10, count);
        }
    }
}
