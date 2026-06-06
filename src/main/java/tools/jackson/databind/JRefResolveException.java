package tools.jackson.databind;

import java.util.Objects;

public class JRefResolveException extends DatabindException {

	private static final long serialVersionUID = 1L;
	private final JRefResolver resolver;
	private final Object root;

	public JRefResolveException(DeserializationContext ctxt, JRefResolver resolver, Object root, String message, Throwable cause) {
		super(ctxt.getParser(), message, cause);
		Objects.requireNonNull(resolver, "Resolver must not be null");
		this.resolver = resolver;
		this.root = root;
	}

	public JRefResolveException(DeserializationContext ctxt, JRefResolver resolver, Object root, String message) {
		this(ctxt, resolver, root, message, null);
	}

	public JRefResolver getResolver() {
		return this.resolver;
	}

	public Object getRoot() {
		return this.root;
	}

	@Override
	public String toString() {
		return "JRefResolveException [resolver=" + resolver + ", root=" + root + ", message=" + super.getMessage()
				+ "]";
	}

}
