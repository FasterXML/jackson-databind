package com.fasterxml.jackson.databind.introspect;

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
    
    public final AnnotatedWithParams creator;

    public final JsonCreator.Mode creatorMode;

    private PropertyName[] implicitParamNames;
    
    private PropertyName[] explicitParamNames;

    public PotentialCreator(AnnotatedWithParams cr,
            JsonCreator.Mode cm)
    {
        creator = cr;
        creatorMode = cm;
    }

    /*
    /**********************************************************************
    /* Mutators
    /**********************************************************************
     */

    public PotentialCreator introspectParamNames(MapperConfig<?> config)
    {
        if (implicitParamNames != null) {
            return this;
        }
        
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        final int paramCount = creator.getParameterCount();

        if (paramCount == 0) {
            implicitParamNames = explicitParamNames = NO_NAMES;
            return this;
        }

        explicitParamNames = new PropertyName[paramCount];
        implicitParamNames = new PropertyName[paramCount];

        for (int i = 0; i < paramCount; ++i) {
            AnnotatedParameter param = creator.getParameter(i);

            String rawImplName = intr.findImplicitPropertyName(param);
            if (rawImplName != null && !rawImplName.isEmpty()) {
                implicitParamNames[i] = PropertyName.construct(rawImplName);
            }
            PropertyName explName = intr.findNameForDeserialization(param);
            if (explName != null && !explName.isEmpty()) {
                explicitParamNames[i] = explName;
            }
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    public int paramCount() {
        return creator.getParameterCount();
    }

    public AnnotatedParameter param(int ix) {
        return creator.getParameter(ix);
    }

    public boolean hasExplicitNames() {
        for (int i = 0, end = explicitParamNames.length; i < end; ++i) {
            if (explicitParamNames[i] != null) {
                return true;
            }
        }
        return false;
    }

    public PropertyName explicitName(int ix) {
        return explicitParamNames[ix];
    }
    
    public PropertyName implicitName(int ix) {
        return implicitParamNames[ix];
    }

    public String implicitNameSimple(int ix) {
        PropertyName pn = implicitParamNames[ix];
        return (pn == null) ? null : pn.getSimpleName();
    }

    /*
    /**********************************************************************
    /* Misc other
    /**********************************************************************
     */

    // For troubleshooting
    @Override
    public String toString() {
        return "(mode="+creatorMode+")"+creator;
    }
}

