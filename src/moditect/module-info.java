import com.fasterxml.jackson.core.ObjectCodec;

module com.fasterxml.jackson.databind {
	uses java.nio.file.spi.FileSystemProvider;
	exports com.fasterxml.jackson.databind;
	exports com.fasterxml.jackson.databind.annotation;

	exports com.fasterxml.jackson.databind.ser.std;
	exports com.fasterxml.jackson.databind.deser.std;


	exports com.fasterxml.jackson.databind.module to com.fasterxml.jackson.module.paranamer;
	exports com.fasterxml.jackson.databind.introspect  to com.fasterxml.jackson.module.paranamer, com.fasterxml.jackson.module.mrbean, com.fasterxml.jackson.module.guice, com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.module.jaxb;
	exports com.fasterxml.jackson.databind.cfg to  com.fasterxml.jackson.module.mrbean, com.fasterxml.jackson.module.jaxb;
	exports com.fasterxml.jackson.databind.type to  com.fasterxml.jackson.module.mrbean, com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jdk8, com.fasterxml.jackson.module.jaxb;
	exports com.fasterxml.jackson.databind.ser to  com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jdk8;
	exports com.fasterxml.jackson.databind.ser.impl  to  com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jdk8;
	exports com.fasterxml.jackson.databind.jsontype to  com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jdk8, com.fasterxml.jackson.module.jaxb;
	exports com.fasterxml.jackson.databind.util to  com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jdk8, com.fasterxml.jackson.module.jaxb;

	exports com.fasterxml.jackson.databind.deser to  com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jdk8;
	exports com.fasterxml.jackson.databind.deser.impl to  com.fasterxml.jackson.module.afterburner;
	exports com.fasterxml.jackson.databind.jsonFormatVisitors to  com.fasterxml.jackson.datatype.jdk8, com.fasterxml.jackson.module.jaxb;
	exports com.fasterxml.jackson.databind.node to com.fasterxml.jackson.module.jaxb;
	exports com.fasterxml.jackson.databind.jsontype.impl to com.fasterxml.jackson.module.jaxb;


	requires transitive com.fasterxml.jackson.core;

	requires java.xml;
	requires java.logging;

	//Optionals
	requires static com.fasterxml.jackson.annotation;
	requires static java.sql;
	//TODThe JDK 7 Impl, Think about getting rid of this
	requires static java.desktop;

	provides ObjectCodec with com.fasterxml.jackson.databind.ObjectMapper;
}
