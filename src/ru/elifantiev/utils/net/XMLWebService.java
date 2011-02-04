package ru.elifantiev.utils.net;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.elifantiev.utils.SimpleXMLSerializer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

public class XMLWebService extends WebService<Document, Document> {

    public XMLWebService(String _baseUrl, String _login, String _password) {
        super(_baseUrl, _login, _password);
    }

    protected Document callMethod(String method) throws IOException {
        return callMethod(method, new HashMap<String, String>());
    }

    @Override
    protected Document transformInput(String input) {

        DocumentBuilder docB = null;
        try {
            docB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // ignore
        }

        Document doc = null;
        if (docB != null) {
            try {
                doc = docB.parse(new ByteArrayInputStream(input.getBytes()));
            } catch (SAXException e) {
                // ignore
            } catch (IOException e) {
                // ignore
            }
        }

        return doc;
    }

    @Override
    protected String transformOutput(Document output) {
        return SimpleXMLSerializer.serialize(output);
    }
}
