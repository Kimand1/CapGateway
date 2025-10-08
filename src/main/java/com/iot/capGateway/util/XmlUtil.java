package com.iot.capGateway.util;
import jakarta.xml.bind.*;
import java.io.*;
public final class XmlUtil {
    public static <T> T fromXml(String xml, Class<T> type){
        try {
            JAXBContext ctx = JAXBContext.newInstance(type);
            Unmarshaller um = ctx.createUnmarshaller();
            return type.cast(um.unmarshal(new StringReader(xml)));
        } catch(Exception e){ throw new RuntimeException(e); }
    }
    public static String toXml(Object obj){
        try {
            JAXBContext ctx = JAXBContext.newInstance(obj.getClass());
            Marshaller m = ctx.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            m.marshal(obj, sw);
            return sw.toString();
        } catch(Exception e){ throw new RuntimeException(e); }
    }
}
