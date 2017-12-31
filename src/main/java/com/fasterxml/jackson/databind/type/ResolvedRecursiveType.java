package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Internal placeholder type used for self-references.
 *
 * @since 2.7
 */
public class ResolvedRecursiveType extends TypeBase
{
    private static final long serialVersionUID = 1L;

    protected JavaType _referencedType;

    public ResolvedRecursiveType(Class<?> erasedType, TypeBindings bindings) {
        super(erasedType, bindings, null, null, 0, null, null, false);
    }

    public void setReference(JavaType ref)
    {
        // sanity check; should not be called multiple times
        if (_referencedType != null) {
            throw new IllegalStateException("Trying to re-set self reference; old value = "+_referencedType+", new = "+ref);
        }
        _referencedType = ref;
    }
   
    @Override
    public JavaType getSuperClass() {
    	if (_referencedType != null) {
    		return _referencedType.getSuperClass();
    	}
    	return super.getSuperClass();
    }

    public JavaType getSelfReferencedType() { return _referencedType; }

    @Override
    public StringBuilder getGenericSignature(StringBuilder sb) {
        return _referencedType.getGenericSignature(sb);
    }

    @Override
    public StringBuilder getErasedSignature(StringBuilder sb) {
        return _referencedType.getErasedSignature(sb);
    }

    @Override
    public JavaType withContentType(JavaType contentType) {
        return this;
    }
    
    @Override
    public JavaType withTypeHandler(Object h) {
        return this;
    }

    @Override
    public JavaType withContentTypeHandler(Object h) {
        return this;
    }

    @Override
    public JavaType withValueHandler(Object h) {
        return this;
    }

    @Override
    public JavaType withContentValueHandler(Object h) {
        return this;
    }

    @Override
    public JavaType withStaticTyping() {
        return this;
    }

    @Override
    public JavaType refine(Class<?> rawType, TypeBindings bindings,
            JavaType superClass, JavaType[] superInterfaces) {
        return null;
    }

    @Override
    public boolean isContainerType() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(40)
                .append("[recursive type; ");
        if (_referencedType == null) {
            sb.append("UNRESOLVED");
        } else {
            // [databind#1301]: Typically resolves to a loop so short-cut
            //   and only include type-erased class
            sb.append(_referencedType.getRawClass().getName());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() == getClass()) {
            // 16-Jun-2017, tatu: as per [databind#1658], cannot do recursive call since
            //    there is likely to be a cycle...

            // but... true or false?
            return false;
            
            /*
            // Do NOT ever match unresolved references
            if (_referencedType == null) {
                return false;
            }
            return (o.getClass() == getClass()
                    && _referencedType.equals(((ResolvedRecursiveType) o).getSelfReferencedType()));
                    */
        }
        return false;
    }
}
