package tools.jackson.databind.deser.jdk;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.std.FromStringDeserializer;

/**
 * Deserializer for {@link Pattern} values, with optional limit on the length of
 * the regex String accepted. By default there is no limit
 * ({@link #DEFAULT_MAX_PATTERN_LENGTH} is {@link #UNLIMITED_PATTERN_LENGTH}).
 *<p>
 * Registered automatically for {@link Pattern} (by {@link JDKMiscDeserializers}),
 * so explicit registration is only needed for limiting maximum pattern
 * length. Since deserializers registered via {@link tools.jackson.databind.JacksonModule}s
 * take precedence over default ones, registering an instance replaces the
 * default one:
 *<pre>
 *  SimpleModule module = new SimpleModule()
 *          .addDeserializer(Pattern.class, new PatternDeserializer(1000));
 *  ObjectMapper mapper = JsonMapper.builder()
 *          .addModule(module)
 *          .build();
 *</pre>
 *
 * @since 3.3
 */
@JacksonStdImpl
public class PatternDeserializer extends FromStringDeserializer<Pattern>
{
    /**
     * Value to pass as maximum pattern length to disable length checking
     * altogether (see {@link #PatternDeserializer(int)}).
     */
    public final static int UNLIMITED_PATTERN_LENGTH = -1;

    /**
     * Maximum length of a regex pattern String accepted unless overridden:
     * by default, no limit.
     */
    public final static int DEFAULT_MAX_PATTERN_LENGTH = UNLIMITED_PATTERN_LENGTH;

    /**
     * Maximum length of a regex pattern String accepted by this instance, or
     * {@link #UNLIMITED_PATTERN_LENGTH} for no limit.
     */
    protected final int _maxPatternLength;

    public PatternDeserializer() { this(DEFAULT_MAX_PATTERN_LENGTH); }

    /**
     * Constructor for specifying maximum length of regex pattern String to accept.
     *
     * @param maxPatternLength Maximum length of pattern String to accept; must be
     *   positive, or {@link #UNLIMITED_PATTERN_LENGTH} to accept patterns of any
     *   length
     */
    public PatternDeserializer(int maxPatternLength) {
        super(Pattern.class);
        if (maxPatternLength <= 0 && maxPatternLength != UNLIMITED_PATTERN_LENGTH) {
            throw new IllegalArgumentException(
                    "Argument `maxPatternLength` must be positive or "+UNLIMITED_PATTERN_LENGTH
                    +" (unlimited), was: "+maxPatternLength);
        }
        _maxPatternLength = maxPatternLength;
    }

    @Override
    protected Pattern _deserialize(String value, DeserializationContext ctxt)
        throws JacksonException
    {
        // Reject regex patterns longer than configured maximum, if any
        if ((_maxPatternLength != UNLIMITED_PATTERN_LENGTH)
                && (value.length() > _maxPatternLength)) {
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
