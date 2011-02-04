package ru.elifantiev.utils;


import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SimpleXMLSerializer {

    public static String serialize(Document doc) {
        StringBuilder ret = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        parseTag(ret, doc.getDocumentElement());
        return ret.toString();
    }

    private static void parseTag(StringBuilder output, Node node) {
        output.append("\n<").append(node.getNodeName());
        parseAttributes(output, node.getAttributes());
        output.append(">\n");
        parseChildren(output, node.getChildNodes());
        output.append("\n</").append(node.getNodeName()).append(">");
    }

    private static void parseAttributes(StringBuilder output, NamedNodeMap attributes) {
        for(int i = 0, l = attributes.getLength(); i<l; i++) {
            output.append(" ").append(attributes.item(i).getNodeName());
            output.append("=\"").append(attributes.item(i).getNodeValue()).append("\" ");
        }
    }

    private static void parseChildren(StringBuilder output, NodeList children) {
        for(int i = 0, l = children.getLength(); i<l; i++) {
            parseTag(output, children.item(i));
        }
    }

}
