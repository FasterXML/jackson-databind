package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

public final class CreatorCandidate
{
    protected final AnnotationIntrospector _intr;
    protected final AnnotatedWithParams _creator;
    protected final int _paramCount;
    protected final Param[] _params;

    protected CreatorCandidate(AnnotationIntrospector intr,
            AnnotatedWithParams ct, Param[] params, int count) {
        _intr = intr;
        _creator = ct;
        _params = params;
        _paramCount = count;
    }

    public static CreatorCandidate construct(AnnotationIntrospector intr,
            AnnotatedWithParams creator, BeanPropertyDefinition[] propDefs)
    {
        final int pcount = creator.getParameterCount();
        Param[] params = new Param[pcount];
        for (int i = 0; i < pcount; ++i) {
            AnnotatedParameter annParam = creator.getParameter(i);
            JacksonInject.Value injectId = intr.findInjectableValue(annParam);
            params[i] = new Param(annParam, (propDefs == null) ? null : propDefs[i], injectId);
        }
        return new CreatorCandidate(intr, creator, params, pcount);
    }

    public AnnotatedWithParams creator() { return _creator; }
    public int paramCount() { return _paramCount; }
    public JacksonInject.Value injection(int i) { return _params[i].injection; }
    public AnnotatedParameter parameter(int i) { return _params[i].annotated; }
    public BeanPropertyDefinition propertyDef(int i) { return _params[i].propDef; }

    public PropertyName paramName(int i) {
        BeanPropertyDefinition propDef = _params[i].propDef;
        if (propDef != null) {
            return propDef.getFullName();
        }
        return null;
    }

    public PropertyName explicitParamName(int i) {
        BeanPropertyDefinition propDef = _params[i].propDef;
        if (propDef != null) {
            if (propDef.isExplicitlyNamed()) {
                return propDef.getFullName();
            }
        }
        return null;
    }

    public PropertyName findImplicitParamName(int i) {
        String str = _intr.findImplicitPropertyName(_params[i].annotated);
        if (str != null && !str.isEmpty()) {
            return PropertyName.construct(str);
        }
        return null;
    }

    /**
     * Specialized accessor that finds index of the one and only parameter
     * with NO injection and returns that; or, if none or more than one found,
     * returns -1.
     */
    public int findOnlyParamWithoutInjection()
    {
        int missing = -1;
        for (int i = 0; i < _paramCount; ++i) {
            if (_params[i].injection == null) {
                if (missing >= 0) {
                    return -1;
                }
                missing = i;
            }
        }
        return missing;
    }

    @Override
    public String toString() {
        return _creator.toString();
    }

    public final static class Param {
        public final AnnotatedParameter annotated;
        public final BeanPropertyDefinition propDef;
        public final JacksonInject.Value injection;

        public Param(AnnotatedParameter p, BeanPropertyDefinition pd,
                JacksonInject.Value i)
        {
            annotated = p;
            propDef = pd;
            injection = i;
        }

        public PropertyName fullName() {
            if (propDef == null) {
                return null;
            }
            return propDef.getFullName();
        }

        public boolean hasFullName() {
            if (propDef == null) {
                return false;
            }
            PropertyName n = propDef.getFullName();
            return n.hasSimpleName();
        }
    }
}
