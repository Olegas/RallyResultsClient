package ru.elifantiev.rallyresults;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ru.elifantiev.rallyresults.infrastructure.RallySection;
import ru.elifantiev.rallyresults.infrastructure.StatRecord;
import ru.elifantiev.utils.net.XMLWebService;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RallyWebService extends XMLWebService {

    public RallyWebService(String _baseUrl, String _login, String _password) {
        super(_baseUrl, _login, _password);
    }

    public boolean login() {

        try {
            return super.callMethod("check").getElementsByTagName("result").item(0).getFirstChild().getNodeValue().equals("1");
        } catch (IOException e) {
            return false;
        }

    }

    public Map<Integer, String> getCompetitions() {

        Map<Integer, String> rv = new HashMap<Integer, String>();

        try {
            NodeList data = super.callMethod("competitions").getElementsByTagName("competition");

            for (int i = 0; i < data.getLength(); i++) {
                Element elem = (Element) data.item(i);
                Integer id = new Integer(elem.getElementsByTagName("id").item(0).getFirstChild().getNodeValue());
                String name = elem.getElementsByTagName("name").item(0).getFirstChild().getNodeValue();

                rv.put(id, name);
            }
        } catch (IOException e) {
            // ignore
        }

        return rv;
    }

    public Map<Integer, String> getSections(int competitionId) {

        Map<Integer, String> rv = new LinkedHashMap<Integer, String>();
        HashMap<String, String> args = new HashMap<String, String>();
        args.put("competition", Integer.toString(competitionId));

        try {
            NodeList data = super.callMethod("sections", args).getElementsByTagName("section");

            for (int i = 0; i < data.getLength(); i++) {
                Element elem = (Element) data.item(i);
                Integer id = new Integer(elem.getElementsByTagName("id").item(0).getFirstChild().getNodeValue());
                String name = elem.getElementsByTagName("name").item(0).getFirstChild().getNodeValue();

                rv.put(id, name);
            }
        } catch (IOException e) {
            // ignore
        }

        return rv;
    }

    public RallySection getSectionStats(int competitionId, int sectionId) {
        HashMap<String, String> args = new HashMap<String, String>(2);
        args.put("competitionId", Integer.toString(competitionId));
        args.put("sectionId", Integer.toString(sectionId));

        try {
            Document doc = super.callMethod("sectionstat", args);
            return new RallySection(doc);
        } catch (IOException e) {
            return new RallySection();
        }
    }

    public RallySection updateStatRecord(StatRecord record) throws IOException {
        HashMap<String, String> args = new HashMap<String, String>(2);
        args.put("competitionId", String.valueOf(record.getCompetitionId()));
        args.put("sectionId", String.valueOf(record.getSectionId()));

        Document doc = super.callMethod("putstat", args, record.toXML());
        return new RallySection(doc);

    }


}
