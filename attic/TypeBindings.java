package com.fasterxml.jackson.databind.type;

import java.lang.reflect.*;
import java.util.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used for resolving type parameters for given class
 */
public class TypeBindings
{
    private final static JavaType[] NO_TYPES = new JavaType[0];

    /**
     * Marker to use for (temporarily) unbound references.
     */
    public final static JavaType UNBOUND = new SimpleType(Object.class);

    /**
     * Factory to use for constructing resolved related types.
     */
    protected final TypeFactory _typeFactory;

    /**
     * @since 2.7
     */
    protected final ClassStack _classStack;
    
    /**
     * Context type used for resolving all types, if specified. May be null,
     * in which case {@link #_contextClass} is used instead.
     */
    protected final JavaType _contextType;

    /**
     * Specific class to use for resolving all types, for methods and fields
     * class and its superclasses and -interfaces contain.
     */
    protected final Class<?> _contextClass;

    /**
     * Lazily-instantiated bindings of resolved type parameters
     */
    protected Map<String,JavaType> _bindings;

    /**
     * Also: we may temporarily want to mark certain named types
     * as resolved (but without exact type); if so, we'll just store
     * names here.
     */
    protected HashSet<String> _placeholders;

    /**
     * Sometimes it is necessary to allow hierarchic resolution of types: specifically
     * in cases where there are local bindings (for methods, constructors). If so,
     * we'll just use simple delegation model.
     */
    private final TypeBindings _parentBindings;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    public TypeBindings(TypeFactory typeFactory, ClassStack stack, Class<?> cc)
    {
        this(typeFactory, null, stack, cc, null);
    }

    public TypeBindings(TypeFactory typeFactory, ClassStack stack, JavaType type)
    {
        this(typeFactory, null, stack, type.getRawClass(), type);
    }

    /**
     * Constructor used to create "child" instances; mostly to
     * allow delegation from explicitly defined local overrides
     * (local type variables for methods, constructors) to
     * contextual (class-defined) ones.
     */
    public TypeBindings childInstance() {
        return new TypeBindings(_typeFactory, this, _classStack, _contextClass, _contextType);
    }

    private TypeBindings(TypeFactory tf, TypeBindings parent, ClassStack stack,
            Class<?> cc, JavaType type)
    {
        _typeFactory = tf;
        _parentBindings = parent;
        _classStack = stack;
        _contextClass = cc;
        _contextType = type;
    }

    /*
    /**********************************************************
    /* Pass-through type resolution methods
    /**********************************************************
     */

    public JavaType resolveType(Class<?> cls) {
        return _typeFactory._constructType(_classStack, cls, this);
    }

    public JavaType resolveType(Type type) {
        return _typeFactory._constructType(_classStack, type, this);
    }

    /*
    /**********************************************************
    /* Accesors
    /**********************************************************
     */

    public JavaType findType(String name, boolean mustFind)
    {
        if (_bindings == null) {
            _resolve();
        }
        JavaType t = _bindings.get(name);
        if (t != null) {
            return t;
        }
        if (_placeholders != null && _placeholders.contains(name)) {
            return UNBOUND;
        }
        if (_parentBindings != null) {
            return _parentBindings.findType(name, mustFind);
        }
        // nothing found, so...
        // Should we throw an exception or just return null?

        /* 18-Feb-2011, tatu: There are some tricky type bindings within
         *   java.util, such as HashMap$KeySet; so let's punt the problem
         *   (honestly not sure what to do -- they are unbound for good, I think)
         */
        if (_contextClass != null) {
            if (ClassUtil.getEnclosingClass(_contextClass) != null) {
                // [JACKSON-572]: Actually, let's skip this for all non-static inner classes
                //   (which will also cover 'java.util' type cases...
                if (!Modifier.isStatic(_contextClass.getModifiers())) {
                    return UNBOUND;
                }
            }
        }

        if (!mustFind) {
            return null;
        }
        
        String className;
        if (_contextClass != null) {
            className = _contextClass.getName();
        } else if (_contextType != null) {
            className = _contextType.toString();
        } else {
            className = "UNKNOWN";
        }
        throw new IllegalArgumentException("Type variable '"+name
                +"' can not be resolved (with context of class "+className+")");
        //t = UNBOUND;                
    }

    public void addBinding(String name, JavaType type)
    {
        // note: emptyMap() is unmodifiable, hence second check is needed:
        if (_bindings == null || _bindings.size() == 0) {
            _bindings = new LinkedHashMap<String,JavaType>();
        }
        _bindings.put(name, type);
    }

    public JavaType[] typesAsArray()
    {
        if (_bindings == null) {
            _resolve();
        }
        if (_bindings.size() == 0) {
            return NO_TYPES;
        }
        return _bindings.values().toArray(new JavaType[_bindings.size()]);
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    // Only for tests!
    protected int getBindingCount() {
        if (_bindings == null) {
            _resolve();
        }
        return _bindings.size();
    }
    
    protected void _resolve()
    {
        _resolveBindings(_contextClass);

        // finally: may have root level type info too
        if (_contextType != null) {
            int count = _contextType.containedTypeCount();
            if (count > 0) {
                for (int i = 0; i < count; ++i) {
                    String name = _contextType.containedTypeName(i);
                    JavaType type = _contextType.containedType(i);
                    addBinding(name, type);
                }
            }
        }

        // nothing bound? mark with empty map to prevent further calls
        if (_bindings == null) {
            _bindings = Collections.emptyMap();
        }
    }

    public void _addPlaceholder(String name) {
        if (_placeholders == null) {
            _placeholders = new HashSet<String>();
        }
        _placeholders.add(name);
    }

    protected void _resolveBindings(Type t)
    {
        if (t == null) return;

        Class<?> raw;
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Type[] args = pt.getActualTypeArguments();
            if (args  != null && args.length > 0) {
                Class<?> rawType = (Class<?>) pt.getRawType();    
                TypeVariable<?>[] vars = rawType.getTypeParameters();
                if (vars.length != args.length) {
                    throw new IllegalArgumentException("Strange parametrized type (in class "+rawType.getName()+"): number of type arguments != number of type parameters ("+args.length+" vs "+vars.length+")");
                }
                for (int i = 0, len = args.length; i < len; ++i) {
                    TypeVariable<?> var = vars[i];
                    String name = var.getName();
                    if (_bindings == null) {
                        _bindings = new LinkedHashMap<String,JavaType>();
                    } else {
                        // 24-Mar-2010, tatu: Better ensure that we do not overwrite something
                        //  collected earlier (since we descend towards super-classes):
                        if (_bindings.containsKey(name)) continue;
                    }
                    // first: add a placeholder to prevent infinite loops
                    _addPlaceholder(name);
                    // then resolve type
                    _bindings.put(name, _typeFactory._constructType(_classStack, args[i], this));
                }
            }
            raw = (Class<?>)pt.getRawType();
        } else if (t instanceof Class<?>) {
            raw = (Class<?>) t;
            /* [JACKSON-677]: If this is an inner class then the generics are defined on the 
             * enclosing class so we have to check there as well.  We don't
             * need to call getEnclosingClass since anonymous classes declare 
             * generics
             */
            Class<?> decl = ClassUtil.getDeclaringClass(raw);
            /* 08-Feb-2013, tatu: Except that if context is also super-class, we must
             *   skip it; context will be checked anyway, and we'd get StackOverflow if
             *   we went there.
             */
            if (decl != null && !decl.isAssignableFrom(raw)) {
                _resolveBindings(decl);
            }

            /* 24-Mar-2010, tatu: Can not have true generics definitions, but can
             *   have lower bounds ("<T extends BeanBase>") in declaration itself
             */
            TypeVariable<?>[] vars = raw.getTypeParameters();
            if (vars != null && vars.length > 0) {
                JavaType[] typeParams = null;

                if (_contextType != null && raw.isAssignableFrom(_contextType.getRawClass())) {
                    typeParams = _typeFactory.findTypeParameters(_contextType, raw);
                }

                for (int i = 0; i < vars.length; i++) {
                    TypeVariable<?> var = vars[i];

                    String name = var.getName();
                    Type varType = var.getBounds()[0];
                    if (varType != null) {
                        if (_bindings == null) {
                            _bindings = new LinkedHashMap<String,JavaType>();
                        } else { // and no overwriting...
                            if (_bindings.containsKey(name)) continue;
                        }
                        _addPlaceholder(name); // to prevent infinite loops

                        if (typeParams != null && typeParams.length > i) {
                            _bindings.put(name, typeParams[i]);
                        } else {
                            _bindings.put(name, _typeFactory._constructType(_classStack, varType, this));
                        }
                    }
                }
            }
        } else { // probably can't be any of these... so let's skip for now
            //if (type instanceof GenericArrayType) {
            //if (type instanceof TypeVariable<?>) {
            // if (type instanceof WildcardType) {
            return;
        }
        // but even if it's not a parameterized type, its super types may be:
        _resolveBindings(ClassUtil.getGenericSuperclass(raw));
        for (Type intType : raw.getGenericInterfaces()) {
            _resolveBindings(intType);
        }
    }

    @Override
    public String toString()
    {
        if (_bindings == null) {
            _resolve();
        }
        StringBuilder sb = new StringBuilder("[TypeBindings for ");
        if (_contextType != null) {
            sb.append(_contextType.toString());
        } else {
            sb.append(_contextClass.getName());
        }
        sb.append(": ").append(_bindings).append("]");
        return sb.toString();
    }
}
