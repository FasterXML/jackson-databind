package tools.jackson.databind.type;

import java.io.Serial;
import java.lang.reflect.Modifier;
import java.util.*;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.util.ClassUtil;

/**
 * Simple recursive-descent parser for parsing canonical {@link JavaType}
 * representations and constructing type instances.
 */
public class TypeParser
    implements java.io.Serializable
{
    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * Maximum length of canonical type definition we will try to parse.
     * Used as protection for malformed generic type declarations.
     *
     * @since 2.16
     */
    protected static final int MAX_TYPE_LENGTH = 64_000;

    /**
     * Maximum levels of nesting allowed for parameterized types.
     * Used as protection for malformed generic type declarations.
     *
     * @since 2.16
     */
    protected static final int MAX_TYPE_NESTING = 1000;

    public final static TypeParser instance = new TypeParser();

    public TypeParser() { }

    public JavaType parse(TypeFactory tf, String canonical) throws IllegalArgumentException
    {
        if (canonical.length() > MAX_TYPE_LENGTH) {
            throw new IllegalArgumentException("Failed to parse type %s: too long (%d characters), maximum length allowed: %d".formatted(
                    _quoteTruncated(canonical),
                    canonical.length(),
                    MAX_TYPE_LENGTH));

        }
        MyTokenizer tokens = new MyTokenizer(canonical.trim());
        JavaType type = parseType(tf, tokens, MAX_TYPE_NESTING);
        // must be end, now
        if (tokens.hasMoreTokens()) {
            throw _problem(tokens, "Unexpected tokens after complete type");
        }
        return type;
    }

    protected JavaType parseType(TypeFactory tf, MyTokenizer tokens, int nestingAllowed)
        throws IllegalArgumentException
    {
        if (!tokens.hasMoreTokens()) {
            throw _problem(tokens, "Unexpected end-of-string");
        }
        Class<?> base = findClass(tf, tokens.nextToken(), tokens);

        JavaType result = parseClass(tf, base, null, tokens, nestingAllowed);
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (!token.startsWith("$")) {
                tokens.pushBack(token);
                break;
            }
            if (--nestingAllowed < 0) {
                throw _problem(tokens, "too deeply nested; exceeds maximum of "
                        +MAX_TYPE_NESTING+" nesting levels");
            }
            Class<?> member = findClass(tf, result.getRawClass().getName() + token, tokens);
            if (member.getDeclaringClass() != result.getRawClass()
                    || Modifier.isStatic(member.getModifiers())) {
                throw _problem(tokens, "Class '"+member.getName()
                        +"' is not a non-static member of '"+result.getRawClass().getName()+"'");
            }
            result = parseClass(tf, member, result, tokens, nestingAllowed);
        }
        return result;
    }

    private JavaType parseClass(TypeFactory tf, Class<?> base, JavaType owner,
            MyTokenizer tokens, int nestingAllowed)
    {
        JavaType[] parameters = new JavaType[0];
        boolean explicitArguments = false;
        if (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if ("<".equals(token)) {
                explicitArguments = true;
                List<JavaType> parameterTypes = parseTypes(tf, tokens, nestingAllowed-1);
                parameters = parameterTypes.toArray(new JavaType[0]);
            } else {
                tokens.pushBack(token);
            }
        }
        TypeBindings bindings = (owner == null && !explicitArguments)
                ? TypeBindings.emptyBindings() : TypeBindings.create(base, parameters, owner);
        return tf._fromClass(null, base, bindings);
    }

    protected List<JavaType> parseTypes(TypeFactory tf, MyTokenizer tokens, int nestingAllowed)
        throws IllegalArgumentException
    {
        if (nestingAllowed < 0) {
            throw _problem(tokens, "too deeply nested; exceeds maximum of "
                    +MAX_TYPE_NESTING+" nesting levels");
        }
        ArrayList<JavaType> types = new ArrayList<JavaType>();
        if (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (">".equals(token)) {
                return types;
            }
            tokens.pushBack(token);
        }
        while (tokens.hasMoreTokens()) {
            types.add(parseType(tf, tokens, nestingAllowed));
            if (!tokens.hasMoreTokens()) break;
            String token = tokens.nextToken();
            if (">".equals(token)) return types;
            if (!",".equals(token)) {
                throw _problem(tokens, "Unexpected token '"+token+"', expected ',' or '>')");
            }
        }
        throw _problem(tokens, "Unexpected end-of-string");
    }

    protected Class<?> findClass(TypeFactory tf, String className, MyTokenizer tokens)
    {
        try {
            return tf.findClass(className);
        } catch (Exception e) {
            ClassUtil.throwIfRTE(e);
            throw _problem(tokens, "Cannot locate class '"+className+"', problem: "+e.getMessage());
        }
    }

    protected IllegalArgumentException _problem(MyTokenizer tokens, String msg)
    {
        return new IllegalArgumentException("Failed to parse type %s (remaining: %s): %s".formatted(
                _quoteTruncated(tokens.getAllInput()),
                _quoteTruncated(tokens.getRemainingInput()),
                msg));
    }

    private static String _quoteTruncated(String str) {
        if (str.length() <= 1000) {
            return "'"+str+"'";
        }
        return "'%s...'[truncated %d charaters]".formatted(
                str.substring(0, 1000), str.length() - 1000);
    }

    final static protected class MyTokenizer extends StringTokenizer
    {
        protected final String _input;

        protected int _index;

        protected String _pushbackToken;

        public MyTokenizer(String str) {
            super(str, "<,>", true);
            _input = str;
        }

        @Override
        public boolean hasMoreTokens() {
            return (_pushbackToken != null) || super.hasMoreTokens();
        }

        @Override
        public String nextToken() {
            String token;
            if (_pushbackToken != null) {
                token = _pushbackToken;
                _pushbackToken = null;
            } else {
                token = super.nextToken();
                _index += token.length();
                token = token.trim();
            }
            return token;
        }

        public void pushBack(String token) {
            _pushbackToken = token;
            // let's NOT change index for now, since token may have been trim()ed
        }

        public String getAllInput() { return _input; }
//        public String getUsedInput() { return _input.substring(0, _index); }
        public String getRemainingInput() { return _input.substring(_index); }
    }
}
