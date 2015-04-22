package com.fasterxml.jackson.databind.ext;

import javax.xml.soap.Detail;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestSOAP extends com.fasterxml.jackson.databind.BaseMapTest {

    public void testSerializeSOAP() throws SOAPException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SOAPFactory fac = SOAPFactory.newInstance();
        Detail detailElement = fac.createDetail();
        detailElement.setTextContent("test");
        String result = objectMapper.writer().writeValueAsString(detailElement);
        assertNotNull(result);
    }
}
