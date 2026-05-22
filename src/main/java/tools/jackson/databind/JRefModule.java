package tools.jackson.databind;

import java.util.List;

import tools.jackson.databind.BeanDescription.Supplier;
import tools.jackson.databind.deser.BeanDeserializerBuilder;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.ArrayType;
import tools.jackson.databind.type.CollectionLikeType;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.type.MapType;
import tools.jackson.databind.type.ReferenceType;

public class JRefModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public JRefModule() {
		super("JRef");
	}

	protected class JRefValueDeserializerModifier extends ValueDeserializerModifier {

		private static final long serialVersionUID = 1L;

		protected ValueDeserializer<?> makeJRefValueDeserializer(DeserializationConfig config, JavaType jt,
				Supplier beanDescRef, ValueDeserializer<?> d) {
			return new JRefValueDeserializer(d);
		}

		@Override
		public ValueDeserializer<?> modifyArrayDeserializer(DeserializationConfig config, ArrayType valueType,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, valueType, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyCollectionDeserializer(DeserializationConfig config, CollectionType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyEnumDeserializer(DeserializationConfig config, JavaType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyCollectionLikeDeserializer(DeserializationConfig config,
				CollectionLikeType type, Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, Supplier beanDescRef,
				ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, null, beanDescRef, deserializer);
		}

		@Override
		public KeyDeserializer modifyKeyDeserializer(DeserializationConfig config, JavaType type,
				KeyDeserializer deserializer) {
			return super.modifyKeyDeserializer(config, type, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public ValueDeserializer<?> modifyMapLikeDeserializer(DeserializationConfig config, MapLikeType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return makeJRefValueDeserializer(config, type, beanDescRef, deserializer);
		}

		@Override
		public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, Supplier beanDescRef,
				BeanDeserializerBuilder builder) {
			return super.updateBuilder(config, beanDescRef, builder);
		}

		@Override
		public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, Supplier beanDescRef,
				List<BeanPropertyDefinition> propDefs) {
			return super.updateProperties(config, beanDescRef, propDefs);
		}

		@Override
		public ValueDeserializer<?> modifyReferenceDeserializer(DeserializationConfig config, ReferenceType type,
				Supplier beanDescRef, ValueDeserializer<?> deserializer) {
			return super.modifyReferenceDeserializer(config, type, beanDescRef, deserializer);
		}
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addDeserializerModifier(new JRefValueDeserializerModifier());
	}
}
