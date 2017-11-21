package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

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
    extends BeanDeserializer
{
    private static final long serialVersionUID = 1L;

    protected final static String PROP_NAME_MESSAGE = "message";

    /*
    /************************************************************
    /* Construction
    /************************************************************
     */

    public ThrowableDeserializer(BeanDeserializer baseDeserializer) {
        super(baseDeserializer);
        // need to disable this, since we do post-processing
        _vanillaProcessing = false;
    }

    /**
     * Alternative constructor used when creating "unwrapping" deserializers
     */
    protected ThrowableDeserializer(BeanDeserializer src, NameTransformer unwrapper) {
        super(src, unwrapper);
    }

    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper) {
        if (getClass() != ThrowableDeserializer.class) {
            return this;
        }
        /* main thing really is to just enforce ignoring of unknown
         * properties; since there may be multiple unwrapped values
         * and properties for all may be interleaved...
         */
        return new ThrowableDeserializer(this, unwrapper);
    }

    /*
    /************************************************************
    /* Overridden methods
    /************************************************************
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
                    "Throwable needs a default contructor, a single-String-arg constructor; or explicit @JsonCreator");
        }
        
        Object throwable = null;
        Object[] pending = null;
        int pendingIx = 0;

        for (; p.currentToken() != JsonToken.END_OBJECT; p.nextToken()) {
            String propName = p.currentName();
            SettableBeanProperty prop = _beanProperties.find(propName);
            p.nextToken(); // to point to field value

            if (prop != null) { // normal case
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

            // Maybe it's "message"?
            if (PROP_NAME_MESSAGE.equals(propName)) {
                if (hasStringCreator) {
                    throwable = _valueInstantiator.createFromString(ctxt, p.getText());
                    // any pending values?
                    if (pending != null) {
                        for (int i = 0, len = pendingIx; i < len; i += 2) {
                            prop = (SettableBeanProperty)pending[i];
                            prop.set(throwable, pending[i+1]);
                        }
                        pending = null;
                    }
                    continue;
                }
            }
            // Things marked as ignorable should not be passed to any setter
            if ((_ignorableProps != null) && _ignorableProps.contains(propName)) {
                p.skipChildren();
                continue;
            }
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(p, ctxt, throwable, propName);
                continue;
            }
            // Unknown: let's call handler method
            handleUnknownProperty(p, ctxt, throwable, propName);
        }
        // Sanity check: did we find "message"?
        if (throwable == null) {
            /* 15-Oct-2010, tatu: Can't assume missing message is an error, since it may be
             *   suppressed during serialization, as per [JACKSON-388].
             *   
             *   Should probably allow use of default constructor, too...
             */
            //throw new JsonMappingException("No 'message' property found: could not deserialize "+_beanType);
            if (hasStringCreator) {
                throwable = _valueInstantiator.createFromString(ctxt, null);
            } else {
                throwable = _valueInstantiator.createUsingDefault(ctxt);
            }
            // any pending values?
            if (pending != null) {
                for (int i = 0, len = pendingIx; i < len; i += 2) {
                    SettableBeanProperty prop = (SettableBeanProperty)pending[i];
                    prop.set(throwable, pending[i+1]);
                }
            }
        }
        return throwable;
    }
}
