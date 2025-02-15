package tools.jackson.databind.ext.xml;

import java.util.stream.Stream;
import javax.xml.namespace.QName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QNameAsObjectReadWrite4771Test extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class BeanWithQName {
        @JsonFormat(shape = JsonFormat.Shape.OBJECT)
        public QName qname;

        BeanWithQName() { }

        public BeanWithQName(QName qName) {
            this.qname = qName;
        }
    }

    @ParameterizedTest
    @MethodSource("provideAllPerumtationsOfQNameConstructor")
    void testQNameWithObjectSerialization(QName originalQName) throws Exception
    {
        BeanWithQName bean = new BeanWithQName(originalQName);

        String json = MAPPER.writeValueAsString(bean);

        QName deserializedQName = MAPPER.readValue(json, BeanWithQName.class).qname;

        assertEquals(originalQName.getLocalPart(), deserializedQName.getLocalPart());
        assertEquals(originalQName.getNamespaceURI(), deserializedQName.getNamespaceURI());
        assertEquals(originalQName.getPrefix(), deserializedQName.getPrefix());
    }

    static Stream<Arguments> provideAllPerumtationsOfQNameConstructor()
    {
        return Stream.of(
                Arguments.of(new QName("test-local-part")),
                Arguments.of(new QName("test-namespace-uri", "test-local-part")),
                Arguments.of(new QName("test-namespace-uri", "test-local-part", "test-prefix"))
        );
    }
}
