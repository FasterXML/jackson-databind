package tools.jackson.databind.deser.jdk;

import java.util.Arrays;

import tools.jackson.core.*;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.bean.BeanDeserializer;
import tools.jackson.databind.deser.bean.BeanPropertyMap;
import tools.jackson.databind.deser.bean.PropertyBasedCreator;
import tools.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.IgnorePropertiesUtil;
import tools.jackson.databind.util.NameTransformer;

/**
 * Deserializer that builds on basic {@link BeanDeserializer} but
 * override some aspects like instance construction.
 */
@JacksonStdImpl
public class ThrowableDeserializer
    extends BeanDeserializer // not the greatest idea but...
{
    protected final static String PROP_NAME_MESSAGE = "message";
    protected final static String PROP_NAME_SUPPRESSED = "suppressed";

    protected final static String PROP_NAME_LOCALIZED_MESSAGE = "localizedMessage";

    // Properties that should not be set if value is null (would cause NPE or other issues)
    protected final static String PROP_NAME_CAUSE = "cause";
    protected final static String PROP_NAME_STACK_TRACE = "stackTrace";

    /**
     * External ("JSON") names of the standard {@link Throwable} properties: needed
     * because a {@link PropertyNamingStrategy} may rename them. Unlike simple
     * case-changing renames -- which {@code equalsIgnoreCase()} can absorb --
     * snake- and kebab-cased ones cannot be matched against the canonical names
     * at all (see [databind#3497], [databind#6188]), so names are resolved once,
     * at construction.
     *
     * @since 3.3
     */
    protected static class StdPropNames
    {
        public final String message, localizedMessage, suppressed, cause, stackTrace;

        protected StdPropNames(String msg, String localizedMsg, String suppr,
                String cse, String stackTr) {
            message = msg;
            localizedMessage = localizedMsg;
            suppressed = suppr;
            cause = cse;
            stackTrace = stackTr;
        }

        /**
         * Names to use when no {@link PropertyNamingStrategy} is configured.
         */
        protected final static StdPropNames DEFAULT = new StdPropNames(PROP_NAME_MESSAGE,
                PROP_NAME_LOCALIZED_MESSAGE, PROP_NAME_SUPPRESSED,
                PROP_NAME_CAUSE, PROP_NAME_STACK_TRACE);
    }

    /**
     * Resolved external names of the standard {@link Throwable} properties.
     *
     * @since 3.3
     */
    protected final StdPropNames _stdPropNames;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected ThrowableDeserializer(BeanDeserializer baseDeserializer,
            StdPropNames stdPropNames) {
        super(baseDeserializer);
        // need to disable this, since we do post-processing
        _vanillaProcessing = false;
        _stdPropNames = stdPropNames;
    }

    /**
     * @deprecated Since 3.3 use variant that takes {@link BeanDescription.Supplier}:
     *    without it standard {@link Throwable} property names cannot be resolved
     *    against a configured {@link PropertyNamingStrategy}.
     */
    @Deprecated
    public static ThrowableDeserializer construct(DeserializationContext ctxt,
            BeanDeserializer baseDeserializer)
    {
        return construct(ctxt, baseDeserializer, null);
    }

    /**
     * @since 3.3
     */
    public static ThrowableDeserializer construct(DeserializationContext ctxt,
            BeanDeserializer baseDeserializer, BeanDescription.Supplier beanDescRef)
    {
        return new ThrowableDeserializer(baseDeserializer,
                _resolveStdPropNames(beanDescRef));
    }

    /**
     * Helper method for resolving the external names of the standard
     * {@link Throwable} properties ([databind#6188]; completes what was left
     * undone by [databind#3497]).
     *<p>
     * Names are taken from the property definitions regular introspection already
     * produced, rather than re-derived here: that way they match whatever the rest
     * of databind bound the properties to, accounting for a mapper-level
     * {@link PropertyNamingStrategy}, a class-level {@code @JsonNaming} (including
     * its "use default" pseudo-value, which overrides the mapper-level one) and an
     * explicit {@code @JsonProperty} rename alike.
     */
    private static StdPropNames _resolveStdPropNames(BeanDescription.Supplier beanDescRef)
    {
        // No introspection available (deprecated `construct()`): canonical names apply
        if (beanDescRef == null) {
            return StdPropNames.DEFAULT;
        }
        final BeanDescription beanDesc = beanDescRef.get();
        return new StdPropNames(
                _externalName(beanDesc, "getMessage", PROP_NAME_MESSAGE),
                _externalName(beanDesc, "getLocalizedMessage", PROP_NAME_LOCALIZED_MESSAGE),
                _externalName(beanDesc, "getSuppressed", PROP_NAME_SUPPRESSED),
                _externalName(beanDesc, "getCause", PROP_NAME_CAUSE),
                _externalName(beanDesc, "getStackTrace", PROP_NAME_STACK_TRACE));
    }

    /**
     * Finds the external name that the property using given standard {@link Throwable}
     * accessor was bound to; the accessor is located by signature (not by matching
     * property names), so a rename cannot hide it.
     */
    private static String _externalName(BeanDescription beanDesc, String getterName,
            String defaultName)
    {
        AnnotatedMethod m = beanDesc.findMethod(getterName, null);
        if (m != null) {
            for (BeanPropertyDefinition propDef : beanDesc.findProperties()) {
                if (m.equals(propDef.getGetter())) {
                    return propDef.getName();
                }
            }
        }
        // Not found (should not happen for `Throwable`): fall back to canonical
        return defaultName;
    }

    /**
     * Alternative constructor used when creating "unwrapping" deserializers
     */
    protected ThrowableDeserializer(BeanDeserializer src,
            UnwrappedPropertyHandler unwrapHandler, PropertyBasedCreator pbCreator,
                    BeanPropertyMap renamedProperties,
            boolean ignoreAllUnknown, StdPropNames stdPropNames) {
        super(src, unwrapHandler, pbCreator, renamedProperties, ignoreAllUnknown);
        _stdPropNames = stdPropNames;
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
        PropertyBasedCreator pbCreator = _propertyBasedCreator;
        if (pbCreator != null) {
            pbCreator = pbCreator.renameAll(ctxt, transformer);
        }
        // and handle direct unwrapping as well:
        return new ThrowableDeserializer(this, uwHandler, pbCreator,
                _beanProperties.renameAll(ctxt, transformer), true, _stdPropNames);
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

        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        int ix = p.currentNameMatch(_propNameMatcher);
        for (; ; ix = p.nextNameMatch(_propNameMatcher)) {
            if (ix >= 0) {
                p.nextToken();
                SettableBeanProperty prop = _propsByIndex[ix];
                // Property not part of the active view must not be set from input
                // (but standard `Throwable` properties always are, see below)
                if ((activeView != null) && !prop.visibleInView(activeView)
                        && !_isStandardThrowableProperty(prop.getName())) {
                    // [databind#437]: fields in other views to be considered as unknown properties
                    if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)) {
                        ctxt.reportInputMismatch(handledType(),
                                String.format("Input mismatch while deserializing %s. Property '%s' is not part of current active view '%s'" +
                                        " (disable 'DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES' to allow)",
                                        ClassUtil.nameOf(handledType()), prop.getName(), activeView.getName()));
                    }
                    p.skipChildren();
                    continue;
                }
                if (throwable != null) {
                    // 07-Dec-2023, tatu: [databind#4248] Interesting that "cause"
                    //    with `null` blows up. So, avoid.
                    // Same for "stackTrace" - setStackTrace(null) throws NPE
                    if (p.hasToken(JsonToken.VALUE_NULL)
                            && _shouldSkipNullValue(prop.getName())) {
                        continue;
                    }
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
            if (ix != PropertyNameMatcher.MATCH_UNKNOWN_NAME) {
                if (ix == PropertyNameMatcher.MATCH_END_OBJECT) {
                    break;
                }
                return _handleUnexpectedWithin(p, ctxt, throwable);
            }
            // Maybe it's "message"?
            String propName = p.currentName();
            p.nextToken();
            // 04-Sep-2026: [databind#6188] Names compared against are the ones resolved
            //    at construction, so a `PropertyNamingStrategy` is accounted for; the
            //    case-insensitive compare remains for case-insensitive input matching
            if (_stdPropNames.message.equalsIgnoreCase(propName)) {
                throwable = _instantiate(ctxt, hasStringCreator, p.getValueAsString());
                // any pending values?
                if (pending != null) {
                    for (int i = 0, len = pendingIx; i < len; i += 2) {
                        SettableBeanProperty prop = (SettableBeanProperty)pending[i];
                        Object value = pending[i+1];
                        // Skip null values for properties that don't accept them
                        if (value == null && _shouldSkipNullValue(prop.getName())) {
                            continue;
                        }
                        prop.set(ctxt, throwable, value);
                    }
                    pending = null;
                }
                continue;
            }

            if (_stdPropNames.suppressed.equalsIgnoreCase(propName)) {
                // 07-Dec-2023, tatu: Not sure how/why, but JSON Null is otherwise
                //    not handled with such call so...
                if (p.hasToken(JsonToken.VALUE_NULL)) {
                    suppressed = null;
                } else {
                    // Inlined `DeserializationContext.readValue()` to minimize call depth
                    ValueDeserializer<Object> deser = ctxt.findRootValueDeserializer(
                            ctxt.constructType(Throwable[].class));
                    suppressed = (Throwable[]) deser.deserialize(p, ctxt);
                }
                continue;
            }
            if (_stdPropNames.localizedMessage.equalsIgnoreCase(propName)) {
                p.skipChildren();
                continue;
            }
            // Things marked as ignorable (or not in the "include" allow-list) should
            // not be passed to any setter. NOTE: checked only after the standard
            // `Throwable` properties above, which are never subject to filtering
            // (same rationale as `_isStandardThrowableProperty()`)
            if (IgnorePropertiesUtil.shouldIgnore(propName, _ignorableProps, _includableProps)) {
                handleIgnoredProperty(p, ctxt, handledType(), propName);
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
            if (_stdPropNames.message.equalsIgnoreCase(propName)) {
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
                Object value = pending[i+1];
                // Skip null values for properties that don't accept them
                if (value == null && _shouldSkipNullValue(prop.getName())) {
                    continue;
                }
                prop.set(ctxt, throwable, value);
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

    /**
     * Helper method to check if a property with null value should be skipped
     * during deserialization. Some Throwable setters throw NPE when called with null.
     *
     * @since 3.1
     */
    private boolean _shouldSkipNullValue(String propertyName) {
        return _stdPropNames.cause.equals(propertyName)
                || _stdPropNames.stackTrace.equals(propertyName);
    }

    /**
     * Helper method to check whether given property is one of the standard
     * {@link Throwable} properties, which are never subject to {@code @JsonView}
     * filtering: they carry no View annotations of their own, and since
     * {@code MapperFeature.DEFAULT_VIEW_INCLUSION} defaults to disabled, would
     * otherwise be excluded from every view. Note that "message",
     * "localizedMessage" and "suppressed" are normally handled separately (not as
     * regular properties) but are included here for consistency.
     *
     * @since 3.1
     */
    private boolean _isStandardThrowableProperty(String propertyName) {
        return _stdPropNames.cause.equals(propertyName)
                || _stdPropNames.stackTrace.equals(propertyName)
                || _stdPropNames.message.equals(propertyName)
                || _stdPropNames.localizedMessage.equals(propertyName)
                || _stdPropNames.suppressed.equals(propertyName);
    }
}
