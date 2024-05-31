package tools.jackson.databind.deser.bean;

import com.fasterxml.jackson.annotation.JacksonInject;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.AnnotatedWithParams;
import tools.jackson.databind.introspect.BeanPropertyDefinition;

public final class CreatorCandidate
{
    protected final MapperConfig<?> _config;
    protected final AnnotatedWithParams _creator;
    protected final int _paramCount;
    protected final Param[] _params;

    protected CreatorCandidate(MapperConfig<?> config,
            AnnotatedWithParams ct, Param[] params, int count) {
        _config = config;
        _creator = ct;
        _params = params;
        _paramCount = count;
    }

    public static CreatorCandidate construct(MapperConfig<?> config,
            AnnotatedWithParams creator, BeanPropertyDefinition[] propDefs)
    {
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        final int pcount = creator.getParameterCount();
        Param[] params = new Param[pcount];
        for (int i = 0; i < pcount; ++i) {
            AnnotatedParameter annParam = creator.getParameter(i);
            JacksonInject.Value injectId = intr.findInjectableValue(config, annParam);
            params[i] = new Param(annParam, (propDefs == null) ? null : propDefs[i], injectId);
        }
        return new CreatorCandidate(config, creator, params, pcount);
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
