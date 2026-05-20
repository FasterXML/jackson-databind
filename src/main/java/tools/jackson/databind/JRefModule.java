package tools.jackson.databind;

import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;

public class JRefModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public JRefModule() {
		super("JRef");
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addDeserializerModifier(new ValueDeserializerModifier() {
			@Override
			public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, Supplier beanDescRef,
					ValueDeserializer<?> deserializer) {
				return new JRefValueDeserializer(deserializer);
			}
		});
	}
}

