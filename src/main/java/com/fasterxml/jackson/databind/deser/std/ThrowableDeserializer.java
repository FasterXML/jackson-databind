package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Deserializer that builds on basic {@link BeanDeserializer} but
 * override some aspects like instance construction.
 */
public class ThrowableDeserializer
    extends BeanDeserializer // not the greatest idea but...
{
    private static final long serialVersionUID = 1L;

    protected final static String PROP_NAME_MESSAGE = "message";
    protected final static String PROP_NAME_SUPPRESSED = "suppressed";

    protected final static String PROP_NAME_LOCALIZED_MESSAGE = "localizedMessage";

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /**
     * Alternative constructor used when creating "unwrapping" deserializers
     */
    protected ThrowableDeserializer(BeanDeserializer src, NameTransformer unwrapper) {
        super(src, unwrapper);
        // need to disable this, since we do post-processing
        _vanillaProcessing = false;
    }

    @Deprecated // since 2.14
    public ThrowableDeserializer(BeanDeserializer baseDeserializer) {
        this(baseDeserializer, null);
    }

    public static ThrowableDeserializer construct(DeserializationContext ctxt,
            BeanDeserializer baseDeserializer)
    {
        // 27-May-2022, tatu: TODO -- handle actual renaming of fields to support
        //    strategies like kebab- and snake-case where there are changes beyond
        //    simple upper-/lower-casing
        /*
        PropertyNamingStrategy pts = ctxt.getConfig().getPropertyNamingStrategy();
        if (pts != null) {
        }
        */
        return new ThrowableDeserializer(baseDeserializer, (NameTransformer) null);
    }

    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper) {
        // Should possibly for failure? But for now at least don't "undo"
        // custom deserializer
        if (getClass() != ThrowableDeserializer.class) {
            return this;
        }
        // main thing really is to just enforce ignoring of unknown
        // properties; since there may be multiple unwrapped values
        // and properties for all may be interleaved...
        return new ThrowableDeserializer(this, unwrapper);
    }

    /*
    /**********************************************************************
    /* Overridden methods
    /**********************************************************************
     */

    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // 30-Sep-2010, tatu: Need to allow use of @JsonCreator, so:
        if (_propertyBasedCreator != null) { // proper @JsonCreator
            return _deserializeUsingPropertyBased(p, ctxt);
        }
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(p, ctxt));
        }
        if (_beanType.isAbstract()) { // for good measure, check this too
            return ctxt.handleMissingInstantiator(handledType(), getValueInstantiator(), p,
                    "abstract type (need to add/enable type information?)");
        }
        boolean hasStringCreator = _valueInstantiator.canCreateFromString();
        boolean hasDefaultCtor = _valueInstantiator.canCreateUsingDefault();
        // and finally, verify we do have single-String arg constructor (if no @JsonCreator)
        if (!hasStringCreator && !hasDefaultCtor) {
            return ctxt.handleMissingInstantiator(handledType(), getValueInstantiator(), p,
                    "Throwable needs a default constructor, a single-String-arg constructor; or explicit @JsonCreator");
        }
        Throwable throwable = null;
        Object[] pending = null;
        Throwable[] suppressed = null;
        int pendingIx = 0;
        for (; !p.hasToken(JsonToken.END_OBJECT); p.nextToken()) {
            String propName = p.currentName();
            SettableBeanProperty prop = _beanProperties.find(propName);
            p.nextToken(); // to point to field value

            if (prop != null) { // normal case
                // 07-Dec-2023, tatu: [databind#4248] Interesting that "cause"
                //    with `null` blows up. So, avoid.
                if ("cause".equals(prop.getName())
                        && p.hasToken(JsonToken.VALUE_NULL)) {
                    continue;
                }
                if (throwable != null) {
                    prop.deserializeAndSet(p, ctxt, throwable);
                    continue;
                }
                // nope; need to defer
                if (pending == null) {
                    int len = _beanProperties.size();
                    pending = new Object[len + len];
                } else if (pendingIx == pending.length) {
                    // NOTE: only occurs with duplicate properties, possible
                    // with some formats (most notably XML; but possibly with
                    // JSON if duplicate detection not enabled). Most likely
                    // only occurs with malicious content so use linear buffer
                    // resize (no need to optimize performance)
                    pending = Arrays.copyOf(pending, pendingIx + 16);
                }
                pending[pendingIx++] = prop;
                pending[pendingIx++] = prop.deserialize(p, ctxt);
                continue;
            }

            // Maybe it's "message"?

            // 26-May-2022, tatu: [databind#3497] To support property naming strategies,
            //    should ideally mangle property names. But for now let's cheat; works
            //    for case-changing although not for kebab/snake cases and "localizedMessage"
            if (PROP_NAME_MESSAGE.equalsIgnoreCase(propName)) {
                throwable = _instantiate(ctxt, hasStringCreator, p.getValueAsString());
                continue;
            }

            // Things marked as ignorable should not be passed to any setter
            if ((_ignorableProps != null) && _ignorableProps.contains(propName)) {
                p.skipChildren();
                continue;
            }
            if (PROP_NAME_SUPPRESSED.equalsIgnoreCase(propName)) { // or "suppressed"?
                // 07-Dec-2023, tatu: Not sure how/why, but JSON Null is otherwise
                //    not handled with such call so...
                if (p.hasToken(JsonToken.VALUE_NULL)) {
                    suppressed = null;
                } else {
                    // Inlined `DeserializationContext.readValue()` to minimize call depth
                    JsonDeserializer<Object> deser = ctxt.findRootValueDeserializer(
                            ctxt.constructType(Throwable[].class));
                    suppressed = (Throwable[]) deser.deserialize(p, ctxt);
                }
                continue;
            }
            if (PROP_NAME_LOCALIZED_MESSAGE.equalsIgnoreCase(propName)) {
                p.skipChildren();
                continue;
            }
            if (_anySetter != null) {
                // [databind#4316] Since 2.16.2 : at this point throwable should be non-null
                if (throwable == null) {
                    throwable = _instantiate(ctxt, hasStringCreator, null);
                }
                _anySetter.deserializeAndSet(p, ctxt, throwable, propName);
                continue;
            }

            // 23-Jan-2018, tatu: One concern would be `message`, but without any-setter or single-String-ctor
            //   (or explicit constructor). We could just ignore it but for now, let it fail
            // [databind#4071]: In case of "message", skip for default constructor
            if (PROP_NAME_MESSAGE.equalsIgnoreCase(propName)) {
                p.skipChildren();
                continue;
            }

            // Unknown: let's call handler method
            handleUnknownProperty(p, ctxt, throwable, propName);
        }
        // Sanity check: did we find "message"?
        if (throwable == null) {
            throwable = _instantiate(ctxt, hasStringCreator, null);
        }

        // any pending values?
        if (pending != null) {
            for (int i = 0, len = pendingIx; i < len; i += 2) {
                SettableBeanProperty prop = (SettableBeanProperty)pending[i];
                prop.set(throwable, pending[i+1]);
            }
        }

        // any suppressed exceptions?
        if (suppressed != null) {
            for (Throwable s : suppressed) {
                // 13-Dec-2023, tatu: But skip any `null` entries we might have gotten
                if (s != null) {
                    throwable.addSuppressed(s);
                }
            }
        }

        return throwable;
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    /**
     * Helper method to initialize Throwable
     *
     * @since 2.16.2
     */
    private Throwable _instantiate(DeserializationContext ctxt, boolean hasStringCreator, String valueAsString)
        throws IOException
    {
        /* 15-Oct-2010, tatu: Can't assume missing message is an error, since it may be
         *   suppressed during serialization.
         *
         *   Should probably allow use of default constructor, too...
         */
        //throw new XxxException("No 'message' property found: could not deserialize "+_beanType);
        if (hasStringCreator) {
            if (valueAsString != null) {
                return (Throwable) _valueInstantiator.createFromString(ctxt, valueAsString);
            } else {
                return (Throwable) _valueInstantiator.createFromString(ctxt, null);
            }
        } else {
            return (Throwable) _valueInstantiator.createUsingDefault(ctxt);
        }
    }
}
