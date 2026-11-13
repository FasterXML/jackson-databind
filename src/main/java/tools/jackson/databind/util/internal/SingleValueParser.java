package tools.jackson.databind.util.internal;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.*;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.JsonParserDelegate;
import tools.jackson.core.util.JsonParserSequence;

/**
 * Parser delegate that only exposes the value the underlying parser currently
 * points to (that is, its first token is the current token of the underlying
 * parser), reporting end-of-content after the value. Underlying parser is
 * left pointing to the last token of the value.
 *<p>
 * Used to allow reading a value from the input stream as part of a
 * {@link JsonParserSequence}, without having to buffer it first.
 *<p>
 * To avoid each nesting level adding layers of delegation (which would make
 * cost of reading nested values proportional to nesting depth), instances
 * read directly from the innermost parser, bypassing enclosing
 * {@link JsonParserSequence}s and {@link SingleValueParser}s (as both
 * just pass tokens through within the value). Bypassed instances do not see
 * the end marker of the value, so they are notified once it has been read.
 *
 * @since 3.1.9
 */
public final class SingleValueParser extends JsonParserDelegate
{
    private final static SingleValueParser[] NO_PARSERS = new SingleValueParser[0];

    /**
     * Enclosing instances bypassed by this instance
     */
    private final SingleValueParser[] _bypassed;

    private int _depth;

    private boolean _started;

    private boolean _done;

    private SingleValueParser(JsonParser p, SingleValueParser[] bypassed) {
        super(p);
        _bypassed = bypassed;
    }

    /**
     * Factory method for constructing instance for reading the value
     * given parser currently points to.
     */
    public static SingleValueParser construct(JsonParser p)
    {
        List<SingleValueParser> bypassed = null;
        while (true) {
            if (p instanceof JsonParserSequence seq) {
                p = seq.delegate();
            } else if (p instanceof SingleValueParser svp) {
                if (bypassed == null) {
                    bypassed = new ArrayList<>();
                }
                bypassed.add(svp);
                p = svp.delegate();
            } else {
                break;
            }
        }
        return new SingleValueParser(p,
                (bypassed == null) ? NO_PARSERS : bypassed.toArray(NO_PARSERS));
    }

    @Override
    public JsonToken nextToken() throws JacksonException
    {
        if (_done) {
            return null;
        }
        JsonToken t;
        if (!_started) {
            _started = true;
            t = delegate.currentToken();
        } else if (_depth == 0) {
            _done = true;
            return null;
        } else {
            t = delegate.nextToken();
        }
        if (t == null) {
            _done = true;
            return null;
        }
        if (t.isStructStart()) {
            ++_depth;
        } else if (t.isStructEnd()) {
            if (--_depth == 0) {
                // Bypassed parsers saw start marker of the value, but not end marker
                for (SingleValueParser svp : _bypassed) {
                    --svp._depth;
                }
            }
        }
        return t;
    }

    @Override
    public JsonToken nextValue() throws JacksonException
    {
        JsonToken t = nextToken();
        if (t == JsonToken.PROPERTY_NAME) {
            t = nextToken();
        }
        return t;
    }

    @Override
    public JsonParser skipChildren() throws JacksonException
    {
        if (_done || !_started) {
            return this;
        }
        JsonToken t = delegate.currentToken();
        if ((t != JsonToken.START_OBJECT) && (t != JsonToken.START_ARRAY)) {
            return this;
        }
        int open = 1;
        while (true) {
            t = nextToken();
            if (t == null) {
                return this;
            }
            if (t.isStructStart()) {
                ++open;
            } else if (t.isStructEnd()) {
                if (--open == 0) {
                    return this;
                }
            }
        }
    }

    @Override
    public String nextName() throws JacksonException {
        return (nextToken() == JsonToken.PROPERTY_NAME) ? currentName() : null;
    }

    @Override
    public boolean nextName(SerializableString str) throws JacksonException {
        String name = nextName();
        return (name != null) && name.equals(str.getValue());
    }

    @Override
    public int nextNameMatch(PropertyNameMatcher matcher) throws JacksonException {
        String name = nextName();
        if (name != null) {
            return matcher.matchName(name);
        }
        if (!_done && hasToken(JsonToken.END_OBJECT)) {
            return PropertyNameMatcher.MATCH_END_OBJECT;
        }
        return PropertyNameMatcher.MATCH_ODD_TOKEN;
    }

    // Underlying parser is not owned by this delegate
    @Override
    public void close() { }
}
