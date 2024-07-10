package com.fasterxml.jackson.databind.introspect;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

/**
 * Information about a single Creator (constructor or factory method),
 * kept during property introspection.
 *
 * @since 2.18
 */
public class PotentialCreator
{
    private static final PropertyName[] NO_NAMES = new PropertyName[0];
    
    private final AnnotatedWithParams _creator;

    private final boolean _isAnnotated;

    /**
     * Declared Mode of the creator, if explicitly annotated; {@code null} otherwise
     */
    private JsonCreator.Mode _creatorMode;

    private PropertyName[] _implicitParamNames;
    
    private PropertyName[] _explicitParamNames;

    /**
     * Parameter definitions if (and only if) this represents a
     * Property-based Creator.
     */
    private List<BeanPropertyDefinition> propertyDefs;

    public PotentialCreator(AnnotatedWithParams cr,
            JsonCreator.Mode cm)
    {
        _creator = cr;
        _isAnnotated = (cm != null);
        _creatorMode = (cm == null) ? JsonCreator.Mode.DEFAULT : cm;
    }

    /**
     * Method that can be called to change the {@code creatorMode} this
     * Creator has: typically used to "mark" Creator as {@code JsonCreator.Mode.DELEGATING}
     * or {@code JsonCreator.Mode.PROPERTIES} when further information is gathered).
     *
     * @param mode Mode to set {@code creatorMode} to
     *
     * @return This creator instance
     */
    public PotentialCreator overrideMode(JsonCreator.Mode mode) {
        _creatorMode = mode;
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    public void assignPropertyDefs(List<? extends BeanPropertyDefinition> propertyDefs) {
        this.propertyDefs = (List<BeanPropertyDefinition>) propertyDefs;
    }

    public PotentialCreator introspectParamNames(MapperConfig<?> config)
    {
        if (_implicitParamNames != null) {
            return this;
        }
        final int paramCount = _creator.getParameterCount();

        if (paramCount == 0) {
            _implicitParamNames = _explicitParamNames = NO_NAMES;
            return this;
        }

        _explicitParamNames = new PropertyName[paramCount];
        _implicitParamNames = new PropertyName[paramCount];

        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0; i < paramCount; ++i) {
            AnnotatedParameter param = _creator.getParameter(i);

            String rawImplName = intr.findImplicitPropertyName(param);
            if (rawImplName != null && !rawImplName.isEmpty()) {
                _implicitParamNames[i] = PropertyName.construct(rawImplName);
            }
            PropertyName explName = intr.findNameForDeserialization(param);
            if (explName != null && !explName.isEmpty()) {
                _explicitParamNames[i] = explName;
            }
        }
        return this;
    }

    /**
     * Variant used when implicit names are known; such as case for JDK
     * Record types.
     */
    public PotentialCreator introspectParamNames(MapperConfig<?> config,
           PropertyName[] implicits)
    {
        if (_implicitParamNames != null) {
            return this;
        }
        final int paramCount = _creator.getParameterCount();
        if (paramCount == 0) {
            _implicitParamNames = _explicitParamNames = NO_NAMES;
            return this;
        }

        _explicitParamNames = new PropertyName[paramCount];
        _implicitParamNames = implicits;

        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0; i < paramCount; ++i) {
            AnnotatedParameter param = _creator.getParameter(i);

            PropertyName explName = intr.findNameForDeserialization(param);
            if (explName != null && !explName.isEmpty()) {
                _explicitParamNames[i] = explName;
            }
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    public boolean isAnnotated() {
        return _isAnnotated;
    }

    public AnnotatedWithParams creator() {
        return _creator;
    }

    /**
     * @return Mode declared for this Creator by annotation, if any; {@code null}
     *    if not annotated
     */
    public JsonCreator.Mode creatorMode() {
        return _creatorMode;
    }

    /**
     * Same as {@link #creatorMode()} except that if {@code null} was to be
     * returned, will instead return {@code JsonCreator.Mode.DEFAULT}/
     */
    public JsonCreator.Mode creatorModeOrDefault() {
        if (_creatorMode == null) {
            return JsonCreator.Mode.DEFAULT;
        }
        return _creatorMode;
    }

    public int paramCount() {
        return _creator.getParameterCount();
    }

    public AnnotatedParameter param(int ix) {
        return _creator.getParameter(ix);
    }

    public boolean hasExplicitNames() {
        for (int i = 0, end = _explicitParamNames.length; i < end; ++i) {
            if (_explicitParamNames[i] != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNameFor(int ix) {
        return (_explicitParamNames[ix] != null)
                || (_implicitParamNames[ix] != null);
    }

    public boolean hasNameOrInjectForAllParams(MapperConfig<?> config)
    {
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0, end = _implicitParamNames.length; i < end; ++i) {
            if (!hasNameFor(i)) {
                if (intr == null || intr.findInjectableValue(_creator.getParameter(i)) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public PropertyName explicitName(int ix) {
        return _explicitParamNames[ix];
    }

    public PropertyName implicitName(int ix) {
        return _implicitParamNames[ix];
    }

    public String implicitNameSimple(int ix) {
        PropertyName pn = _implicitParamNames[ix];
        return (pn == null) ? null : pn.getSimpleName();
    }

    public BeanPropertyDefinition[] propertyDefs() {
        if (propertyDefs == null || propertyDefs.isEmpty()) {
            return new BeanPropertyDefinition[0];
        }
        return propertyDefs.toArray(new BeanPropertyDefinition[propertyDefs.size()]);
    }

    /*
    /**********************************************************************
    /* Misc other
    /**********************************************************************
     */

    // For troubleshooting
    @Override
    public String toString() {
        return "(mode="+_creatorMode+")"+_creator;
    }
}

