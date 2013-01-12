package com.fasterxml.jackson.databind.deser.impl;

import java.lang.reflect.Member;
import java.util.*;


import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Container class for storing information on creators (based on annotations,
 * visibility), to be able to build actual instantiator later on.
 */
public class CreatorCollector
{
    /// Type of bean being created
    final protected BeanDescription _beanDesc;

    final protected boolean _canFixAccess;

    /**
     * Reference to the default creator (constructor or factory method).
     *<p>
     * Note: name is a misnomer, after resolving of [JACKSON-850], since this
     * can also point to factory method.
     */
    protected AnnotatedWithParams _defaultConstructor;
    
    protected AnnotatedWithParams _stringCreator, _intCreator, _longCreator;
    protected AnnotatedWithParams _doubleCreator, _booleanCreator;

    protected AnnotatedWithParams _delegateCreator;
    // when there are injectable values along with delegate:
    protected CreatorProperty[] _delegateArgs;
    
    protected AnnotatedWithParams _propertyBasedCreator;
    protected CreatorProperty[] _propertyBasedArgs = null;

    protected AnnotatedParameter _incompleteParameter;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public CreatorCollector(BeanDescription beanDesc, boolean canFixAccess)
    {
        _beanDesc = beanDesc;
        _canFixAccess = canFixAccess;
    }

    public ValueInstantiator constructValueInstantiator(DeserializationConfig config)
    {
        StdValueInstantiator inst = new StdValueInstantiator(config, _beanDesc.getType());

        JavaType delegateType;

        if (_delegateCreator == null) {
            delegateType = null;
        } else {
            // need to find type...
            int ix = 0;
            if (_delegateArgs != null) {
                for (int i = 0, len = _delegateArgs.length; i < len; ++i) {
                    if (_delegateArgs[i] == null) { // marker for delegate itself
                        ix = i;
                        break;
                    }
                }
            }
            TypeBindings bindings = _beanDesc.bindingsForBeanType();
            delegateType = bindings.resolveType(_delegateCreator.getGenericParameterType(ix));
        }
        
        inst.configureFromObjectSettings(_defaultConstructor,
                _delegateCreator, delegateType, _delegateArgs,
                _propertyBasedCreator, _propertyBasedArgs);
        inst.configureFromStringCreator(_stringCreator);
        inst.configureFromIntCreator(_intCreator);
        inst.configureFromLongCreator(_longCreator);
        inst.configureFromDoubleCreator(_doubleCreator);
        inst.configureFromBooleanCreator(_booleanCreator);
        inst.configureIncompleteParameter(_incompleteParameter);
        return inst;
    }
    
    /*
    /**********************************************************
    /* Setters
    /**********************************************************
     */

    /**
     * @deprecated since 2.1, use {@link #setDefaultCreator} instead.
     */
    @Deprecated
    public void setDefaultConstructor(AnnotatedConstructor ctor) {
        _defaultConstructor = _fixAccess(ctor);
    }
    
    /**
     * Method called to indicate the default creator: no-arguments
     * constructor or factory method that is called to instantiate
     * a value before populating it with data. Default creator is
     * only used if no other creators are indicated.
     * 
     * @param creator Creator method; no-arguments constructor or static
     *   factory method.
     */
    public void setDefaultCreator(AnnotatedWithParams creator)
    {
        // !!! TODO: 23-Jul-2012, tatu: Should change to directly change things
        //    here in future; but for backwards compatibility, can't do that yet
        if (creator instanceof AnnotatedConstructor) {
            setDefaultConstructor((AnnotatedConstructor) creator);
            return;
        }
        _defaultConstructor = _fixAccess(creator);
    }
    
    public void addStringCreator(AnnotatedWithParams creator) {
        _stringCreator = verifyNonDup(creator, _stringCreator, "String");
    }
    public void addIntCreator(AnnotatedWithParams creator) {
        _intCreator = verifyNonDup(creator, _intCreator, "int");
    }
    public void addLongCreator(AnnotatedWithParams creator) {
        _longCreator = verifyNonDup(creator, _longCreator, "long");
    }
    public void addDoubleCreator(AnnotatedWithParams creator) {
        _doubleCreator = verifyNonDup(creator, _doubleCreator, "double");
    }
    public void addBooleanCreator(AnnotatedWithParams creator) {
        _booleanCreator = verifyNonDup(creator, _booleanCreator, "boolean");
    }

    public void addDelegatingCreator(AnnotatedWithParams creator,
            CreatorProperty[] injectables)
    {
        _delegateCreator = verifyNonDup(creator, _delegateCreator, "delegate");
        _delegateArgs = injectables;
    }
    
    public void addPropertyCreator(AnnotatedWithParams creator, CreatorProperty[] properties)
    {
        _propertyBasedCreator = verifyNonDup(creator, _propertyBasedCreator, "property-based");
        // [JACKSON-470] Better ensure we have no duplicate names either...
        if (properties.length > 1) {
            HashMap<String,Integer> names = new HashMap<String,Integer>();
            for (int i = 0, len = properties.length; i < len; ++i) {
                String name = properties[i].getName();
                /* [Issue-13]: Need to consider Injectables, which may not have
                 *   a name at all, and need to be skipped
                 */
                if (name.length() == 0 && properties[i].getInjectableValueId() != null) {
                    continue;
                }
                Integer old = names.put(name, Integer.valueOf(i));
                if (old != null) {
                    throw new IllegalArgumentException("Duplicate creator property \""+name+"\" (index "+old+" vs "+i+")");
                }
            }
        }
        _propertyBasedArgs = properties;
    }

    public void addIncompeteParameter(AnnotatedParameter parameter) {
        if (_incompleteParameter == null) {
            _incompleteParameter = parameter;
        }
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    /**
     * @since 2.1
     */
    public boolean hasDefaultCreator() {
        return _defaultConstructor != null;
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private <T extends AnnotatedMember> T _fixAccess(T member)
    {
        if (member != null && _canFixAccess) {
            ClassUtil.checkAndFixAccess((Member) member.getAnnotated());
        }
        return member;
    }

    protected AnnotatedWithParams verifyNonDup(AnnotatedWithParams newOne, AnnotatedWithParams oldOne,
            String type)
    {
        if (oldOne != null) {
            // important: ok to override factory with constructor; but not within same type, so:
            if (oldOne.getClass() == newOne.getClass()) {
                throw new IllegalArgumentException("Conflicting "+type+" creators: already had "+oldOne+", encountered "+newOne);
            }
        }
        return _fixAccess(newOne);
    }
}
