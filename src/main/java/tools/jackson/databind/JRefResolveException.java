package tools.jackson.databind;

import tools.jackson.core.JsonParser;

public class JRefResolveException extends DatabindException {

	private static final long serialVersionUID = 1L;
	private final Object root;

	public JRefResolveException(JsonParser parser, Object root, String message, Throwable cause) {
		super(parser, message, cause);
		this.root = root;
	}

	public JRefResolveException(JsonParser parser, Object root, String message) {
		this(parser, root, message, null);
	}

	public Object getRoot() {
		return this.root;
	}

	@Override
	public String toString() {
		return "JRefResolveException [root=" + root + ", message=" + super.getMessage()
				+ "]";
	}

}
