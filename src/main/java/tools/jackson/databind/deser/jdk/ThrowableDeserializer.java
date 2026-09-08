package tools.jackson.databind.deser.jdk;

import java.util.Arrays;
import java.util.Set;

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
import tools.jackson.databind.util.ViewMatcher;

/**
 * Deserializer that builds on basic {@link BeanDeserializer} but
 * override some aspects like instance construction.
 */
@JacksonStdImpl
public class ThrowableDeserializer
    extends BeanDeserializer // not the greatest idea but...
{
    public final static String PROP_NAME_MESSAGE = "message";
    public final static String PROP_NAME_SUPPRESSED = "suppressed";

    public final static String PROP_NAME_LOCALIZED_MESSAGE = "localizedMessage";

    // Properties that should not be set if value is null (would cause NPE or other issues)
    public final static String PROP_NAME_CAUSE = "cause";
    public final static String PROP_NAME_STACK_TRACE = "stackTrace";

    /**
     * Internal (Java) names of the standard {@link Throwable} properties, as declared by
     * {@link Throwable} itself. Exposed because {@code BeanDeserializerFactory} needs the
     * same set when deciding which properties are exempt from {@code @JsonView} filtering
     * ([databind#6174], [databind#6190]); keeping one copy here, next to the constants it
     * is built from, prevents the two from drifting apart.
     *<p>
     * NOTE: these are the canonical names -- a {@link PropertyNamingStrategy} may rename
     * the properties, in which case the external names are resolved separately (see
     * [databind#6188]).
     *
     * @since 3.3
     */
    public final static Set<String> STD_PROP_NAMES = Set.of(PROP_NAME_MESSAGE,
            PROP_NAME_LOCALIZED_MESSAGE, PROP_NAME_SUPPRESSED,
            PROP_NAME_CAUSE, PROP_NAME_STACK_TRACE);

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

        /**
         * Explicit view definitions for "message" and "suppressed", if any; {@code null}
         * if unrestricted (included under every active view, per [databind#6174]).
         *<p>
         * Needed because -- unlike "cause" and "stackTrace" -- these two have no setter
         * in the common case, so they are never bound as regular properties and are
         * instead consumed by the "unknown name" branches of the read loop, where no
         * {@link SettableBeanProperty} (and hence no view matcher) is available.
         * "localizedMessage" needs none: it is discarded regardless of views.
         *
         * @since 3.3
         */
        public final ViewMatcher messageViews, suppressedViews;

        /**
         * Whether either of the two properties carries an explicit {@code @JsonView}:
         * if so the read loop must resolve the active view even when no regular
         * property has views of its own.
         *
         * @since 3.3
         */
        public boolean hasExplicitViews() {
            return (messageViews != null) || (suppressedViews != null);
        }

        protected StdPropNames(String msg, String localizedMsg, String suppr,
                String cse, String stackTr,
                ViewMatcher msgViews, ViewMatcher supprViews) {
            message = msg;
            localizedMessage = localizedMsg;
            suppressed = suppr;
            cause = cse;
            stackTrace = stackTr;
            messageViews = msgViews;
            suppressedViews = supprViews;
        }

        /**
         * Names to use when no {@link PropertyNamingStrategy} is configured.
         */
        protected final static StdPropNames DEFAULT = new StdPropNames(PROP_NAME_MESSAGE,
                PROP_NAME_LOCALIZED_MESSAGE, PROP_NAME_SUPPRESSED,
                PROP_NAME_CAUSE, PROP_NAME_STACK_TRACE, null, null);
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
     * Helper for finding the views a class-level {@code @JsonView} places all properties
     * of given type in, if any; {@code null} if there is no such annotation.
     *<p>
     * Exists because the distinction matters for the standard {@link Throwable}
     * properties and is easy to get wrong: {@code findDefaultViews()} returns an
     * <i>empty</i> array (not {@code null}) for "no annotation, and
     * {@code MapperFeature.DEFAULT_VIEW_INCLUSION} disabled" -- which is exactly the
     * defaulted-out-of-every-view case those properties are exempted from
     * ([databind#6174]) -- but the annotated classes for a real one, which must be
     * honored ([databind#6190]). Shared with {@code BeanDeserializerFactory}, which
     * assigns the views this class then reads back.
     *
     * @since 3.3
     */
    public static Class<?>[] explicitClassViews(BeanDescription beanDesc) {
        Class<?>[] defViews = beanDesc.findDefaultViews();
        return ((defViews != null) && (defViews.length > 0)) ? defViews : null;
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
        // [databind#6190]: a class-level `@JsonView` covers every property, including these
        final Class<?>[] classViews = explicitClassViews(beanDesc);
        return new StdPropNames(
                _externalName(beanDesc, "getMessage", PROP_NAME_MESSAGE),
                _externalName(beanDesc, "getLocalizedMessage", PROP_NAME_LOCALIZED_MESSAGE),
                _externalName(beanDesc, "getSuppressed", PROP_NAME_SUPPRESSED),
                _externalName(beanDesc, "getCause", PROP_NAME_CAUSE),
                _externalName(beanDesc, "getStackTrace", PROP_NAME_STACK_TRACE),
                _viewMatcher(beanDesc, "getMessage", classViews),
                _viewMatcher(beanDesc, "getSuppressed", classViews));
    }

    /**
     * Helper for finding explicit view definitions of one standard {@link Throwable}
     * property: its own {@code @JsonView} if present, else the class-level one (if any),
     * else {@code null} for "no restriction".
     */
    private static ViewMatcher _viewMatcher(BeanDescription beanDesc, String getterName,
            Class<?>[] classViews)
    {
        Class<?>[] views = null;
        AnnotatedMethod m = beanDesc.findMethod(getterName, null);
        if (m != null) {
            for (BeanPropertyDefinition propDef : beanDesc.findProperties()) {
                if (m.equals(propDef.getGetter())) {
                    views = propDef.findViews();
                    break;
                }
            }
        }
        if (views == null) {
            views = classViews;
        }
        return (views == null) ? null : ViewMatcher.construct(views);
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

        // [databind#6190]: `_needViewProcesing` only accounts for settable properties, but
        // "message"/"suppressed" may carry an explicit `@JsonView` without being bound as
        // one -- so consult those too, else their views would be ignored whenever no
        // regular property has any (as happens with `DEFAULT_VIEW_INCLUSION` enabled)
        final Class<?> activeView = (_needViewProcesing || _stdPropNames.hasExplicitViews())
                ? ctxt.getActiveView() : null;
        int ix = p.currentNameMatch(_propNameMatcher);
        for (; ; ix = p.nextNameMatch(_propNameMatcher)) {
            if (ix >= 0) {
                p.nextToken();
                SettableBeanProperty prop = _propsByIndex[ix];
                // Property not part of the active view must not be set from input.
                // Standard `Throwable` properties without explicit views have no view restrictions
                // configured, while those with explicit `@JsonView` honor them (see [databind#6190]).
                if ((activeView != null) && !prop.visibleInView(activeView)) {
                    _handleViewExcluded(p, ctxt, prop.getName(), activeView);
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
                // [databind#6190]: explicit `@JsonView` on "message" must be honored;
                // left unset, it is instantiated with `null` message after the loop
                if (!_visibleInView(_stdPropNames.messageViews, activeView)) {
                    _handleViewExcluded(p, ctxt, propName, activeView);
                    continue;
                }
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
                // [databind#6190]: explicit `@JsonView` on "suppressed" must be honored
                if (!_visibleInView(_stdPropNames.suppressedViews, activeView)) {
                    _handleViewExcluded(p, ctxt, propName, activeView);
                    continue;
                }
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
            // `Throwable` properties above, which are never subject to filtering.
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
     * Helper for the "unknown name" branches, which have no {@link SettableBeanProperty}
     * to ask: a {@code null} matcher means the property carries no explicit
     * {@code @JsonView} and so is included under every active view ([databind#6174]).
     *
     * @since 3.3
     */
    /**
     * Helper for a property the active view excludes: reported as an unexpected property
     * if {@code FAIL_ON_UNEXPECTED_VIEW_PROPERTIES} is enabled ([databind#437]), and
     * simply skipped otherwise. Shared by all branches of the read loop so that a
     * property behaves the same whether or not it happens to be bound as a settable one.
     *
     * @since 3.3
     */
    private void _handleViewExcluded(JsonParser p, DeserializationContext ctxt,
            String propName, Class<?> activeView)
        throws JacksonException
    {
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)) {
            ctxt.reportInputMismatch(handledType(),
                    String.format("Input mismatch while deserializing %s. Property '%s' is not part of current active view '%s'" +
                            " (disable 'DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES' to allow)",
                            ClassUtil.nameOf(handledType()), propName, activeView.getName()));
        }
        p.skipChildren();
    }

    private boolean _visibleInView(ViewMatcher views, Class<?> activeView) {
        return (activeView == null) || (views == null) || views.isVisibleForView(activeView);
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

}
