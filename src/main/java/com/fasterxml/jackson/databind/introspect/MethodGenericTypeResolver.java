package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Internal utility functionality to handle type resolution for method type variables
 * based on the requested result type: this is needed to work around the problem of
 * static factory methods not being able to use class type variable bindings
 * (see [databind#2895] for more).
 *
 * @since 2.12
 */
final class MethodGenericTypeResolver
{
    /*
     * Attempt to narrow types on a generic factory method based on the expected result (requestedType).
     * If narrowing was possible, a new TypeResolutionContext is returned with the discovered TypeBindings,
     * otherwise the emptyTypeResCtxt argument is returned.
     *
     * For example:
     * Given type Wrapper<T> with
     * @JsonCreator static <T> Wrapper<T> fromJson(T value)
     * When a Wrapper<Duck> is requested the factory must return a Wrapper<Duck> and we can bind T to Duck
     * as though the method was written with defined types:
     * @JsonCreator static Wrapper<Duck> fromJson(Duck value)
     */
    public static TypeResolutionContext narrowMethodTypeParameters(
            Method candidate,
            JavaType requestedType,
            TypeFactory typeFactory,
            TypeResolutionContext emptyTypeResCtxt) {
        TypeBindings newTypeBindings = bindMethodTypeParameters(candidate, requestedType, emptyTypeResCtxt);
        return newTypeBindings == null
                ? emptyTypeResCtxt
                : new TypeResolutionContext.Basic(typeFactory, newTypeBindings);
    }

    /**
     * Returns {@link TypeBindings} with additional type information
     * based on {@code requestedType} if possible, otherwise {@code null}.
     */
    static TypeBindings bindMethodTypeParameters(
            Method candidate,
            JavaType requestedType,
            TypeResolutionContext emptyTypeResCtxt) {
        TypeVariable<Method>[] methodTypeParameters = candidate.getTypeParameters();
        if (methodTypeParameters.length == 0
                // If the primary type has no type parameters, there's nothing to do
                || requestedType.getBindings().isEmpty()) {
            // Method has no type parameters: no need to modify the resolution context.
            return null;
        }
        Type genericReturnType = candidate.getGenericReturnType();
        if (!(genericReturnType instanceof ParameterizedType)) {
            // Return value is not parameterized, it cannot be used to associate the requestedType expectations
            // onto parameters.
            return null;
        }

        ParameterizedType parameterizedGenericReturnType = (ParameterizedType) genericReturnType;
        // Primary type and result type must be the same class, otherwise we would need to
        // trace generic parameters to a common superclass or interface.
        if (!Objects.equals(requestedType.getRawClass(), parameterizedGenericReturnType.getRawType())) {
            return null;
        }

        // Construct TypeBindings based on the requested type, and type variables that occur in the generic return type.
        // For example given requestedType: Foo<String, Int>
        // and method static <T, U> Foo<T, U> func(Bar<T, U> in)
        // Produces TypeBindings{T=String, U=Int}.
        Type[] methodReturnTypeArguments = parameterizedGenericReturnType.getActualTypeArguments();
        ArrayList<String> names = new ArrayList<>(methodTypeParameters.length);
        ArrayList<JavaType> types = new ArrayList<>(methodTypeParameters.length);
        for (int i = 0; i < methodReturnTypeArguments.length; i++) {
            Type methodReturnTypeArgument = methodReturnTypeArguments[i];
            // Note: This strictly supports only TypeVariables of the forms "T" and "? extends T",
            // not complex wildcards with nested type variables
            TypeVariable<?> typeVar = maybeGetTypeVariable(methodReturnTypeArgument);
            if (typeVar != null) {
                String typeParameterName = typeVar.getName();
                if (typeParameterName == null) {
                    return null;
                }

                JavaType bindTarget = requestedType.getBindings().getBoundType(i);
                if (bindTarget == null) {
                    return null;
                }
                // If the type parameter name is not present in the method type parameters we
                // fall back to default type handling.
                TypeVariable<?> methodTypeVariable = findByName(methodTypeParameters, typeParameterName);
                if (methodTypeVariable == null) {
                    return null;
                }
                if (pessimisticallyValidateBounds(emptyTypeResCtxt, bindTarget, methodTypeVariable.getBounds())) {
                    // Avoid duplicate entries for the same type variable, e.g. '<T> Map<T, T> foo(Class<T> in)'
                    int existingIndex = names.indexOf(typeParameterName);
                    if (existingIndex != -1) {
                        JavaType existingBindTarget = types.get(existingIndex);
                        if (bindTarget.equals(existingBindTarget)) {
                            continue;
                        }
                        boolean existingIsSubtype = existingBindTarget.isTypeOrSubTypeOf(bindTarget.getRawClass());
                        boolean newIsSubtype = bindTarget.isTypeOrSubTypeOf(existingBindTarget.getRawClass());
                        if (!existingIsSubtype && !newIsSubtype) {
                            // No way to satisfy the requested type.
                            return null;
                        }
                        if ((existingIsSubtype ^ newIsSubtype) && newIsSubtype) {
                            // If the new type is more specific than the existing type, the new type replaces the old.
                            types.set(existingIndex, bindTarget);
                        }
                    } else {
                        names.add(typeParameterName);
                        types.add(bindTarget);
                    }
                }
            }
        }
        // Fall back to default handling if no specific types from the requestedType are used
        if (names.isEmpty()) {
            return null;
        }
        return TypeBindings.create(names, types);
    }

    /* Returns the TypeVariable if it can be extracted, otherwise null. */
    private static TypeVariable<?> maybeGetTypeVariable(Type type) {
        if (type instanceof TypeVariable) {
            return (TypeVariable<?>) type;
        }
        // Extract simple type variables from wildcards matching '? extends T'
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            // Exclude any form of '? super T'
            if (wildcardType.getLowerBounds().length != 0) {
                return null;
            }
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length == 1) {
                return maybeGetTypeVariable(upperBounds[0]);
            }
        }
        return null;
    }

    /* Returns the TypeVariable if it can be extracted, otherwise null. */
    private static ParameterizedType maybeGetParameterizedType(Type type) {
        if (type instanceof ParameterizedType) {
            return (ParameterizedType) type;
        }
        // Extract simple type variables from wildcards matching '? extends T'
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            // Exclude any form of '? super T'
            if (wildcardType.getLowerBounds().length != 0) {
                return null;
            }
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length == 1) {
                return maybeGetParameterizedType(upperBounds[0]);
            }
        }
        return null;
    }

    private static boolean pessimisticallyValidateBounds(
            TypeResolutionContext context, JavaType boundType, Type[] upperBound) {
        for (Type type : upperBound) {
            if (!pessimisticallyValidateBound(context, boundType, type)) {
                return false;
            }
        }
        return true;
    }

    private static boolean pessimisticallyValidateBound(
            TypeResolutionContext context, JavaType boundType, Type type) {
        if (!boundType.isTypeOrSubTypeOf(context.resolveType(type).getRawClass())) {
            return false;
        }
        ParameterizedType parameterized = maybeGetParameterizedType(type);
        if (parameterized != null
                // 09-Nov-2020, ckozak: Validate equivalent parameters if possible, however when types do not
                // exactly match, there's not much validation we can reasonably do.
                && Objects.equals(boundType.getRawClass(), parameterized.getRawType())) {
            Type[] typeArguments = parameterized.getActualTypeArguments();
            TypeBindings bindings = boundType.getBindings();
            if (bindings.size() != typeArguments.length) {
                return false;
            }
            for (int i = 0; i < bindings.size(); i++) {
                JavaType boundTypeBound = bindings.getBoundType(i);
                Type typeArg = typeArguments[i];
                if (!pessimisticallyValidateBound(context, boundTypeBound, typeArg)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static TypeVariable<?> findByName(TypeVariable<?>[] typeVariables, String name) {
        if (typeVariables == null || name == null) {
            return null;
        }
        for (TypeVariable<?> typeVariable : typeVariables) {
            if (name.equals(typeVariable.getName())) {
                return typeVariable;
            }
        }
        return null;
    }
}
