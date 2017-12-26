package com.mesilat.forms;

import com.atlassian.confluence.pages.AbstractPage;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PageParserImpl implements PageParser {
    private static String EMPTY = new String(new byte[] { -62, -96 });

    @Override
    public ArrayNode parse(AbstractPage page) throws ParseException {
        try {
            String pageBody = getPageBody(page);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setValidating(false);
            documentBuilderFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(new InputSource(new StringReader(pageBody)));
            ObjectMapper mapper = new ObjectMapper();
            return read(mapper, doc);
        } catch (SAXException | ParserConfigurationException | IOException ex) {
            throw new ParseException(ex);
        }
    }
    public static ArrayNode read(ObjectMapper mapper, Document doc) throws ParseException{
        ArrayNode arr = mapper.createArrayNode();
        for (Object obj : read(doc.getDocumentElement())){
            if (obj instanceof MyAttribute){
                throw new ParseException(String.format("Found attribute outside of object: %s", ((MyAttribute)obj).getName()));
            } else if (obj instanceof MyObject){
                arr.add(createObject(mapper, (MyObject)obj));
            }
        }
        return arr;
    }
    private static ObjectNode createObject(ObjectMapper mapper, MyObject data) throws ParseException{
        ObjectNode object = mapper.createObjectNode();
        object.put("_type_", data.getTypeName());
        for (Object obj : data.getAttributes()){
            if (obj instanceof MyAttribute){
                MyAttribute attr = (MyAttribute)obj;
                Object attrVal = createAttribute(mapper, attr);
                if (attrVal instanceof String){
                    String text = (String)attrVal;
                    text = text.trim();
                    object.put(attr.getName(), EMPTY.equals(text)? "": text);
                } else if (attrVal instanceof ObjectNode){
                    object.put(attr.getName(), (ObjectNode)attrVal);
                } else if (attrVal instanceof ArrayNode){
                    object.put(attr.getName(), (ArrayNode)attrVal);
                }
            } else if (obj instanceof MyObject){
                throw new ParseException(String.format("Cannot add object of type %s to an object of type %s as anonymous attribute", ((MyObject)obj).getTypeName(), data.getTypeName()));
            }
        }
        return object;
    }
    private static Object createAttribute(ObjectMapper mapper, MyAttribute attr) throws ParseException{
        ArrayNode arr = mapper.createArrayNode();
        StringBuilder sb = new StringBuilder();
        for (Object obj : attr.getValue()){
            if (obj instanceof String){
                sb.append(obj.toString());
            } else if (obj instanceof MyAttribute){
                throw new ParseException(String.format("Cannot add attribute %s directly to attribute %s", ((MyAttribute)obj).getName(), attr.getName()));
            } else if (obj instanceof MyObject){
                arr.add(createObject(mapper, (MyObject)obj));
            }
        }
        switch (arr.size()) {
            case 0:
                return sb.toString().trim();
            case 1:
                return (ObjectNode)arr.get(0);
            default:
                return arr;
        }
    }
    private static String getPageBody(AbstractPage page) {
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
        ).append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
/*
        ).append("<!DOCTYPE page [\n"
        ).append("<!ENTITY nbsp \"&#160;\">\n"
        ).append("<!ENTITY laquo \"&#171;\">\n"
        ).append("<!ENTITY raquo \"&#187;\">\n"
        ).append("<!ENTITY ndash \"&#8211;\">\n"
        ).append("<!ENTITY mdash \"&#8212;\">\n"
        ).append("]>\n"
*/
        ).append("<body page-id=\""
        ).append(page.getIdAsString()
        ).append("\" labels=\"");
        for (int i = 0; i < page.getLabelCount(); i++) {
            if (i > 0) {
                buf.append(",");
            }
            buf.append(page.getLabels().get(i));
        }
        buf.append("\" xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:ac=\"http://www.atlassian.com/schema/confluence/4/ac/\""
        ).append(" xmlns:ri=\"http://www.atlassian.com/schema/confluence/4/ri/\" xmlns:acxhtml=\"http://www.atlassian.com/schema/confluence/4/\">\n"
        ).append(page.getBodyAsString()
        ).append("\n</body>");
        return buf.toString();
    }
    public static Document getPageDocument(String pageBody) throws SAXException, IOException, ParserConfigurationException{
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(false);
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        StringReader reader = new StringReader(pageBody);
        return builder.parse(new InputSource(reader));       
    }
    private static List<Object> read(Element elt){
        if (elt.hasAttribute("class")){
            List<String> htmlClasses = Arrays.asList(elt.getAttribute("class").split("\\s"));
            if (htmlClasses.contains("com-mesilat-hidden-row-lastrow"))
                return new ArrayList<>();
            for (String htmlClass : htmlClasses){
                if (htmlClass.startsWith("com-mesilat-attribute-")){
                    return Arrays.asList(new Object[]{ new MyAttribute(htmlClass.substring(22), readChildren(elt)) });
                } else if (htmlClass.startsWith("com-mesilat-object-")) {
                    return Arrays.asList(new Object[]{ new MyObject(htmlClass.substring(19), readChildren(elt)) });
                }
            }
        }
        return readChildren(elt);
    }
    private static List<Object> readChildren(Element elt){
        List<Object> children = new ArrayList<>();
        NodeList nodes = elt.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++){
            Node node = nodes.item(i);
            if (node instanceof Text){
                Text text = (Text)node;
                if (text.getData() != null && !text.getData().isEmpty())
                    children.add(text.getData());
            } else if (node instanceof Element){
                Element child = (Element)node;
                if (child.getTagName().equals("ac:placeholder"))
                    continue;
                if (child.getTagName().equals("th"))
                    continue;
                if (child.getTagName().equals("ri:page")) {
                    children.add(child.getAttribute("ri:content-title"));
                    continue;
                }
                children.addAll(read(child));
            }
        }
        return children;
    }

    public static class MyAttribute {
        private String name;
        private List<Object> value;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public List<Object> getValue() {
            return value;
        }
        public void setValue(List<Object> value) {
            this.value = value;
        }

        public MyAttribute(){}
        public MyAttribute(String name){
            this.name = name;
        }
        public MyAttribute(String name, List<Object> value){
            this.name = name;
            this.value = value;
        }
    }
    public static class MyObject {
        private String typeName;
        private List<Object> attributes;

        public String getTypeName() {
            return typeName;
        }
        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }
        public List<Object> getAttributes() {
            return attributes;
        }
        public void setAttributes(List<Object> attributes) {
            this.attributes = attributes;
        }

        public MyObject(){}
        public MyObject(String name){
            this.typeName = name;
        }
        public MyObject(String name, List<Object> attributes){
            this.typeName = name;
            this.attributes = attributes;
        }
    }
}