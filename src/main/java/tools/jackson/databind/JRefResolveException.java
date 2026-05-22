package tools.jackson.databind;

public class JRefResolveException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final JRefResolver resolver;
	private final Object root;

	public JRefResolveException(JRefResolver resolver, Object root, String message, Throwable cause) {
		super(message, cause);
		this.resolver = resolver;
		this.root = root;
	}

	public JRefResolveException(JRefResolver resolver, Object root, String message) {
		super(message);
		this.resolver = resolver;
		this.root = root;
	}

	public JRefResolveException(JRefResolver resolver, String message) {
		this(resolver, null, message);
	}

	public JRefResolver getResolver() {
		return this.resolver;
	}

	public Object getRoot() {
		return this.root;
	}
}
