package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExtensibleSubTypeValidator extends SubTypeValidator
{
    public void addIllegalClassNames(Collection<String> classNames)
    {
        final Set<String> s = new HashSet<String>();
        s.addAll(_cfgIllegalClassNames);
        s.addAll(classNames);
        _cfgIllegalClassNames = Collections.unmodifiableSet(s);
    }
}
