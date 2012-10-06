package com.fasterxml.jackson.databind;

import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.cfg.BaseSettings;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfigBase;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.LinkedNode;

/**
 * Object that contains baseline configuration for deserialization
 * process. An instance is owned by {@link ObjectMapper}, which
 * passes an immutable instance to be used for deserialization process.
 *<p>
 * Note that instances are considered immutable and as such no copies
 * should need to be created for sharing; all copying is done with
 * "fluent factory" methods.
 * Note also that unlike with Jackson 1, these instances can not be
 * assigned to {@link ObjectMapper}; in fact, application code should
 * rarely interact directly with these instance (unlike core Jackson code)
 */
public final class DeserializationConfig
    extends MapperConfigBase<DeserializationFeature, DeserializationConfig>
    implements java.io.Serializable // since 2.1
{
    // for 2.1.0
    private static final long serialVersionUID = -4227480407273773599L;

    /**
     * Set of features enabled; actual type (kind of features)
     * depends on sub-classes.
     */
    protected final int _deserFeatures;

    /**
     * Linked list that contains all registered problem handlers.
     * Implementation as front-added linked list allows for sharing
     * of the list (tail) without copying the list.
     */
    protected final LinkedNode<DeserializationProblemHandler> _problemHandlers;
    
    /**
     * Factory used for constructing {@link com.fasterxml.jackson.databind.JsonNode} instances.
     */
    protected final JsonNodeFactory _nodeFactory;
    
    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    /**
     * Constructor used by ObjectMapper to create default configuration object instance.
     */
    public DeserializationConfig(BaseSettings base,
            SubtypeResolver str, Map<ClassKey,Class<?>> mixins)
    {
        super(base, str, mixins);
        _deserFeatures = collectFeatureDefaults(DeserializationFeature.class);
        _nodeFactory = JsonNodeFactory.instance;
        _problemHandlers = null;
    }

    /**
     * Copy constructor used to create a non-shared instance with given mix-in
     * annotation definitions and subtype resolver.
     */
    private DeserializationConfig(DeserializationConfig src, SubtypeResolver str)
    {
        super(src, str);
        _deserFeatures = src._deserFeatures;
        _nodeFactory = src._nodeFactory;
        _problemHandlers = src._problemHandlers;
    }

    private DeserializationConfig(DeserializationConfig src,
            int mapperFeatures, int deserFeatures)
    {
        super(src, mapperFeatures);
        _deserFeatures = deserFeatures;
        _nodeFactory = src._nodeFactory;
        _problemHandlers = src._problemHandlers;
    }
    
    private DeserializationConfig(DeserializationConfig src, BaseSettings base)
    {
        super(src, base);
        _deserFeatures = src._deserFeatures;
        _nodeFactory = src._nodeFactory;
        _problemHandlers = src._problemHandlers;
    }
    
    private DeserializationConfig(DeserializationConfig src, JsonNodeFactory f)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = f;
    }

    private DeserializationConfig(DeserializationConfig src,
            LinkedNode<DeserializationProblemHandler> problemHandlers)
    {
        super(src);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = problemHandlers;
        _nodeFactory = src._nodeFactory;
    }

    private DeserializationConfig(DeserializationConfig src, String rootName)
    {
        super(src, rootName);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
    }

    private DeserializationConfig(DeserializationConfig src, Class<?> view)
    {
        super(src, view);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
    }

    /**
     * @since 2.1
     */
    protected DeserializationConfig(DeserializationConfig src, Map<ClassKey,Class<?>> mixins)
    {
        super(src, mixins);
        _deserFeatures = src._deserFeatures;
        _problemHandlers = src._problemHandlers;
        _nodeFactory = src._nodeFactory;
    }
    
    // for unit tests only:
    protected BaseSettings getBaseSettings() { return _base; }
    
    /*
    /**********************************************************
    /* Life-cycle, factory methods from MapperConfig
    /**********************************************************
     */

    @Override
    public DeserializationConfig with(MapperFeature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperFeature f : features) {
            newMapperFlags |= f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this :
            new DeserializationConfig(this, newMapperFlags, _deserFeatures);
    }

    @Override
    public DeserializationConfig without(MapperFeature... features)
    {
        int newMapperFlags = _mapperFeatures;
        for (MapperFeature f : features) {
             newMapperFlags &= ~f.getMask();
        }
        return (newMapperFlags == _mapperFeatures) ? this :
            new DeserializationConfig(this, newMapperFlags, _deserFeatures);
    }
    
    @Override
    public DeserializationConfig with(ClassIntrospector ci) {
        return _withBase(_base.withClassIntrospector(ci));
    }

    @Override
    public DeserializationConfig with(AnnotationIntrospector ai) {
        return _withBase(_base.withAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig with(VisibilityChecker<?> vc) {
        return _withBase(_base.withVisibilityChecker(vc));
    }

    @Override
    public DeserializationConfig withVisibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        return _withBase( _base.withVisibility(forMethod, visibility));
    }
    
    @Override
    public DeserializationConfig with(TypeResolverBuilder<?> trb) {
        return _withBase(_base.withTypeResolverBuilder(trb));
    }

    @Override
    public DeserializationConfig with(SubtypeResolver str) {
        return (_subtypeResolver == str) ? this : new DeserializationConfig(this, str);
    }
    
    @Override
    public DeserializationConfig with(PropertyNamingStrategy pns) {
        return _withBase(_base.withPropertyNamingStrategy(pns));
    }

    @Override
    public DeserializationConfig withRootName(String rootName) {
        if (rootName == null) {
            if (_rootName == null) {
                return this;
            }
        } else if (rootName.equals(_rootName)) {
            return this;
        }
        return new DeserializationConfig(this, rootName);
    }
    
    @Override
    public DeserializationConfig with(TypeFactory tf) {
        return _withBase( _base.withTypeFactory(tf));
    }

    @Override
    public DeserializationConfig with(DateFormat df) {
        return _withBase(_base.withDateFormat(df));
    }
    
    @Override
    public DeserializationConfig with(HandlerInstantiator hi) {
        return _withBase(_base.withHandlerInstantiator(hi));
    }

    @Override
    public DeserializationConfig withInsertedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withInsertedAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig withAppendedAnnotationIntrospector(AnnotationIntrospector ai) {
        return _withBase(_base.withAppendedAnnotationIntrospector(ai));
    }

    @Override
    public DeserializationConfig withView(Class<?> view) {
        return (_view == view) ? this : new DeserializationConfig(this, view);
    }

    @Override
    public DeserializationConfig with(Locale l) {
        return _withBase(_base.with(l));
    }

    @Override
    public DeserializationConfig with(TimeZone tz) {
        return _withBase(_base.with(tz));
    }

    @Override
    public DeserializationConfig with(Base64Variant base64) {
        return _withBase(_base.with(base64));
    }
    
    private final DeserializationConfig _withBase(BaseSettings newBase) {
        return (_base == newBase) ? this : new DeserializationConfig(this, newBase);
    }
    
    /*
    /**********************************************************
    /* Life-cycle, deserialization-specific factory methods
    /**********************************************************
     */

    /**
     * Fluent factory method that will construct a new instance with
     * specified {@link JsonNodeFactory}
     */
    public DeserializationConfig with(JsonNodeFactory f) {
        if (_nodeFactory == f) {
            return this;
        }
        return new DeserializationConfig(this, f);
    }

    /**
     * Method that can be used to add a handler that can (try to)
     * resolve non-fatal deserialization problems.
     */
    public DeserializationConfig withHandler(DeserializationProblemHandler h)
    {
        // Sanity check: let's prevent adding same handler multiple times
        if (LinkedNode.contains(_problemHandlers, h)) {
            return this;
        }
        return new DeserializationConfig(this,
                new LinkedNode<DeserializationProblemHandler>(h, _problemHandlers));
    }

    /**
     * Method for removing all configured problem handlers; usually done to replace
     * existing handler(s) with different one(s)
     */
    public DeserializationConfig withNoProblemHandlers() {
        if (_problemHandlers == null) {
            return this;
        }
        return new DeserializationConfig(this,
                (LinkedNode<DeserializationProblemHandler>) null);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(DeserializationFeature feature)
    {
        int newDeserFeatures = (_deserFeatures | feature.getMask());
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig with(DeserializationFeature first,
            DeserializationFeature... features)
    {
        int newDeserFeatures = _deserFeatures | first.getMask();
        for (DeserializationFeature f : features) {
            newDeserFeatures |= f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public DeserializationConfig withFeatures(DeserializationFeature... features)
    {
        int newDeserFeatures = _deserFeatures;
        for (DeserializationFeature f : features) {
            newDeserFeatures |= f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }
    
    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public DeserializationConfig without(DeserializationFeature feature)
    {
        int newDeserFeatures = _deserFeatures & ~feature.getMask();
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig without(DeserializationFeature first,
            DeserializationFeature... features)
    {
        int newDeserFeatures = _deserFeatures & ~first.getMask();
        for (DeserializationFeature f : features) {
            newDeserFeatures &= ~f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public DeserializationConfig withoutFeatures(DeserializationFeature... features)
    {
        int newDeserFeatures = _deserFeatures;
        for (DeserializationFeature f : features) {
            newDeserFeatures &= ~f.getMask();
        }
        return (newDeserFeatures == _deserFeatures) ? this :
            new DeserializationConfig(this, _mapperFeatures, newDeserFeatures);
    }
    
    /*
    /**********************************************************
    /* MapperConfig implementation
    /**********************************************************
     */
    
    /**
     * Method for getting {@link AnnotationIntrospector} configured
     * to introspect annotation values used for configuration.
     */
    @Override
    public AnnotationIntrospector getAnnotationIntrospector()
    {
        /* 29-Jul-2009, tatu: it's now possible to disable use of
         *   annotations; can be done using "no-op" introspector
         */
        if (isEnabled(MapperFeature.USE_ANNOTATIONS)) {
            return super.getAnnotationIntrospector();
        }
        return NopAnnotationIntrospector.instance;
    }

    @Override
    public boolean useRootWrapping()
    {
        if (_rootName != null) { // empty String disables wrapping; non-empty enables
            return (_rootName.length() > 0);
        }
        return isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE);
    }
    
    /**
     * Accessor for getting bean description that only contains class
     * annotations: useful if no getter/setter/creator information is needed.
     */
    @Override
    public BeanDescription introspectClassAnnotations(JavaType type) {
        return getClassIntrospector().forClassAnnotations(this, type, this);
    }

    /**
     * Accessor for getting bean description that only contains immediate class
     * annotations: ones from the class, and its direct mix-in, if any, but
     * not from super types.
     */
    @Override
    public BeanDescription introspectDirectClassAnnotations(JavaType type) {
        return getClassIntrospector().forDirectClassAnnotations(this, type, this);
    }

    @Override
    public VisibilityChecker<?> getDefaultVisibilityChecker()
    {
        VisibilityChecker<?> vchecker = super.getDefaultVisibilityChecker();
        if (!isEnabled(MapperFeature.AUTO_DETECT_SETTERS)) {
            vchecker = vchecker.withSetterVisibility(Visibility.NONE);
        }
        if (!isEnabled(MapperFeature.AUTO_DETECT_CREATORS)) {
            vchecker = vchecker.withCreatorVisibility(Visibility.NONE);
        }
        if (!isEnabled(MapperFeature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        return vchecker;
    }

    public final boolean isEnabled(DeserializationFeature f) {
        return (_deserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Other configuration
    /**********************************************************
     */

    public final int getDeserializationFeatures() {
        return _deserFeatures;
    }
    
    /**
     * Method for getting head of the problem handler chain. May be null,
     * if no handlers have been added.
     */
    public LinkedNode<DeserializationProblemHandler> getProblemHandlers() {
        return _problemHandlers;
    }

    public final JsonNodeFactory getNodeFactory() {
        return _nodeFactory;
    }
    
    /*
    /**********************************************************
    /* Introspection methods
    /**********************************************************
     */

    /**
     * Method that will introspect full bean properties for the purpose
     * of building a bean deserializer
     *
     * @param type Type of class to be introspected
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspect(JavaType type) {
        return (T) getClassIntrospector().forDeserialization(this, type, this);
    }

    /**
     * Method that will introspect subset of bean properties needed to
     * construct bean instance.
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectForCreation(JavaType type) {
        return (T) getClassIntrospector().forCreation(this, type, this);
    }

    /**
     * @since 2.0
     */
    @SuppressWarnings("unchecked")
    public <T extends BeanDescription> T introspectForBuilder(JavaType type) {
        return (T) getClassIntrospector().forDeserializationWithBuilder(this, type, this);
    }
}
