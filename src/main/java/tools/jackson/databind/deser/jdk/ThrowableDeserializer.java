package tools.jackson.databind.deser.jdk;

import tools.jackson.core.*;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.bean.BeanDeserializer;
import tools.jackson.databind.deser.bean.BeanPropertyMap;
import tools.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import tools.jackson.databind.util.NameTransformer;

/**
 * Deserializer that builds on basic {@link BeanDeserializer} but
 * override some aspects like instance construction.
 */
public class ThrowableDeserializer
    extends BeanDeserializer // not the greatest idea but...
{
    protected final static String PROP_NAME_MESSAGE = "message";
    protected final static String PROP_NAME_SUPPRESSED = "suppressed";

    protected final static String PROP_NAME_LOCALIZED_MESSAGE = "localizedMessage";

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected ThrowableDeserializer(BeanDeserializer baseDeserializer) {
        super(baseDeserializer);
        // need to disable this, since we do post-processing
        _vanillaProcessing = false;
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
        return new ThrowableDeserializer(baseDeserializer);
    }

    /**
     * Alternative constructor used when creating "unwrapping" deserializers
     */
    protected ThrowableDeserializer(BeanDeserializer src,
            UnwrappedPropertyHandler unwrapHandler, BeanPropertyMap renamedProperties,
            boolean ignoreAllUnknown) {
        super(src, unwrapHandler, renamedProperties, ignoreAllUnknown);
    }

    @Override
    public ValueDeserializer<Object> unwrappingDeserializer(DeserializationContext ctxt,
            NameTransformer transformer)
    {
        if (getClass() != ThrowableDeserializer.class) {
            return this;
        }
        // main thing really is to just enforce ignoring of unknown properties; since
        // there may be multiple unwrapped values and properties for all may be interleaved...
        UnwrappedPropertyHandler uwHandler = _unwrappedPropertyHandler;
        // delegate further unwraps, if any
        if (uwHandler != null) {
            uwHandler = uwHandler.renameAll(ctxt, transformer);
        }
        // and handle direct unwrapping as well:
        return new ThrowableDeserializer(this, uwHandler,
                _beanProperties.renameAll(ctxt, transformer), true);
    }

    /*
    /**********************************************************************
    /* Overridden methods
    /**********************************************************************
     */

    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
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

        int ix = p.currentNameMatch(_propNameMatcher);
        for (; ; ix = p.nextNameMatch(_propNameMatcher)) {
            if (ix >= 0) {
                p.nextToken();
                SettableBeanProperty prop = _propsByIndex[ix];
                if (throwable != null) {
                    prop.deserializeAndSet(p, ctxt, throwable);
                    continue;
                }
                // nope; need to defer
                if (pending == null) {
                    int len = _beanProperties.size();
                    pending = new Object[len + len];
                }
                pending[pendingIx++] = prop;
                pending[pendingIx++] = prop.deserialize(p, ctxt);
                continue;
            }
            if (ix != PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                if (ix == PropertyNameMatcher.MATCH_END_OBJECT) {
                    break;
                }
                return _handleUnexpectedWithin(p, ctxt, throwable);
            }
            // Maybe it's "message"?
            String propName = p.currentName();
            p.nextToken();
            // 26-May-2022, tatu: [databind#3497] To support property naming strategies,
            //    should ideally mangle property names. But for now let's cheat; works
            //    for case-changing although not for kebab/snake cases and "localizedMessage"
            if (PROP_NAME_MESSAGE.equalsIgnoreCase(propName)) {
                if (hasStringCreator) {
                    throwable = (Throwable) _valueInstantiator.createFromString(ctxt, p.getValueAsString());
                    // any pending values?
                    if (pending != null) {
                        for (int i = 0, len = pendingIx; i < len; i += 2) {
                            SettableBeanProperty prop = (SettableBeanProperty)pending[i];
                            prop.set(throwable, pending[i+1]);
                        }
                        pending = null;
                    }
                    continue;
                }
                // fall through
            }

            // Things marked as ignorable should not be passed to any setter
            if ((_ignorableProps != null) && _ignorableProps.contains(propName)) {
                p.skipChildren();
                continue;
            }
            if (PROP_NAME_SUPPRESSED.equalsIgnoreCase(propName)) { // or "suppressed"?
                suppressed = ctxt.readValue(p, Throwable[].class);
                continue;
            }
            if (PROP_NAME_LOCALIZED_MESSAGE.equalsIgnoreCase(propName)) {
                p.skipChildren();
                continue;
            }
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(p, ctxt, throwable, propName);
                continue;
            }

            // 23-Jan-2018, tatu: One concern would be `message`, but without any-setter or single-String-ctor
            //   (or explicit constructor). We could just ignore it but for now, let it fail

            // Unknown: let's call handler method
            handleUnknownProperty(p, ctxt, throwable, propName);
        }
        // Sanity check: did we find "message"?
        if (throwable == null) {
            // 15-Oct-2010, tatu: Can't assume missing message is an error, since it may be
            //   suppressed during serialization.
            //
            //   Should probably allow use of default constructor, too...
            //throw new XxxException("No 'message' property found: could not deserialize "+_beanType);
            if (hasStringCreator) {
                throwable = (Throwable) _valueInstantiator.createFromString(ctxt, null);
            } else {
                throwable = (Throwable) _valueInstantiator.createUsingDefault(ctxt);
            }
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
                throwable.addSuppressed(s);
            }
        }

        return throwable;
    }
}
