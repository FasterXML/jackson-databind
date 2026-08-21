package tools.jackson.databind.deser.jdk;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.std.FromStringDeserializer;

/**
 * Deserializer for {@link Pattern} values, with a limit on the length of the
 * regex String accepted: regex compilation and matching can be exponential in
 * pattern length, so the limit is much lower than the general String value
 * length limit.
 *<p>
 * Registered automatically for {@link Pattern} (by {@link JDKMiscDeserializers}),
 * so explicit registration is only needed for overriding the default maximum
 * pattern length.
 *
 * @since 3.3
 */
@JacksonStdImpl
public class PatternDeserializer extends FromStringDeserializer<Pattern>
{
    /**
     * Maximum length of a regex pattern String accepted unless overridden.
     */
    public final static int DEFAULT_MAX_PATTERN_LENGTH = 1000;

    /**
     * Maximum length of a regex pattern String accepted by this instance.
     */
    protected final int _maxPatternLength;

    public PatternDeserializer() { this(DEFAULT_MAX_PATTERN_LENGTH); }

    /**
     * Constructor for specifying maximum length of regex pattern String to accept.
     *
     * @param maxPatternLength Maximum length of pattern String to accept; must be
     *   positive
     */
    public PatternDeserializer(int maxPatternLength) {
        super(Pattern.class);
        if (maxPatternLength <= 0) {
            throw new IllegalArgumentException("Argument `maxPatternLength` must be positive");
        }
        _maxPatternLength = maxPatternLength;
    }

    @Override
    protected Pattern _deserialize(String value, DeserializationContext ctxt)
        throws JacksonException
    {
        // Reject excessively long regex patterns to prevent catastrophic backtracking
        // and excessive resource usage
        if (value.length() > _maxPatternLength) {
            return (Pattern) ctxt.handleWeirdStringValue(_valueClass, value,
                    "regex pattern length (%d) exceeds maximum (%d)",
                    value.length(), _maxPatternLength);
        }
        try {
            return Pattern.compile(value);
        } catch (PatternSyntaxException e) {
            return (Pattern) ctxt.handleWeirdStringValue(_valueClass, value,
                    "Invalid Pattern, problem: "+e.getDescription());
        }
    }

    @Override
    protected boolean _shouldTrim() {
        // 04-Dec-2021, tatu: [databind#3299] Do not trim (trailing) white space:
        return false;
    }
}
