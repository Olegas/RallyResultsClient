package ru.elifantiev.rallyresults.infrastructure;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;


public class StatRecord extends HashMap<String, String> {

    private String number, start = "", finish = "";
    private String[] startParts = {"", "", "", ""}, finishParts = {"", "", "", ""};
    private int competitionId, sectionId;

    public StatRecord(Node node, int competitionId, int sectionId) throws ParseException {
        NamedNodeMap attrs = node.getAttributes();
        number = ((Attr) attrs.getNamedItem("number")).getValue();
        parseStart(((Attr) attrs.getNamedItem("start")).getValue());
        parseFinish(((Attr) attrs.getNamedItem("finish")).getValue());
        this.competitionId = competitionId;
        this.sectionId = sectionId;
    }

    public StatRecord(String number, int competitionId, int sectionId) {
        this.number = number;
        this.competitionId = competitionId;
        this.sectionId = sectionId;
    }

    public int getCompetitionId() {
        return competitionId;
    }

    public int getSectionId() {
        return sectionId;
    }

    public String getNumber() {
        return number;
    }

    public String getStart() {
        if (start.equals(""))
            return start;
        else
            return start.substring(0, start.indexOf('.'));
    }

    public void setStart(String start) throws ParseException {
        parseStart(start);
    }

    public void setStart(String hour, String minute, String second) throws ParseException {
        parseStart(String.format("%s:%s:%s.0", hour, minute, second));
    }

    public String getFinish() {
        return finish;
    }

    public void setFinish(String finish) throws ParseException {
        parseFinish(finish);
    }

    public void setFinish(String hour, String minute, String second, String msecond) throws ParseException {
        parseFinish(String.format("%s:%s:%s.%s", hour, minute, second, msecond));
    }

    private void parseStart(String start) throws ParseException {
        if (start.equals("n/a"))
            return;

        String[] _startParts = start.split(":|\\.");
        if (_startParts.length == 4) {
            startParts = _startParts;
            this.start = start;
        } else
            throw new ParseException("Wrong format: " + start);
    }

    private void parseFinish(String finish) throws ParseException {
        if (finish.equals("n/a"))
            return;

        String[] _finishParts = finish.split(":|\\.");
        if (_finishParts.length == 4) {
            finishParts = _finishParts;
            this.finish = finish;
        } else
            throw new ParseException("Wrong format: " + finish);
    }

    public String getStartHour() {
        return startParts[0];
    }

    public String getStartMinute() {
        return startParts[1];
    }

    public String getStartSecond() {
        return startParts[2];
    }

    public String getFinishHour() {
        return finishParts[0];
    }

    public String getFinishMinute() {
        return finishParts[1];
    }

    public String getFinishSecond() {
        return finishParts[2];
    }

    public String getFinishMSecond() {
        return finishParts[3];
    }

    public Document toXML() {
        DocumentBuilder docB = null;
        try {
            docB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // ignore
        }

        Document retval = docB.getDOMImplementation().createDocument(null, "statrecord", null);

        Element root = retval.getDocumentElement();
        root.setAttribute("car", number);
        root.setAttribute("competitonId", String.valueOf(competitionId));
        root.setAttribute("sectionId", String.valueOf(sectionId));

        Element start = retval.createElement("start");
        Element finish = retval.createElement("finish");

        start.setAttribute("startH", new Integer(startParts[0]).toString());
        start.setAttribute("startM", new Integer(startParts[1]).toString());
        start.setAttribute("startS", new Integer(startParts[2]).toString());

        finish.setAttribute("endH", new Integer(finishParts[0]).toString());
        finish.setAttribute("endM", new Integer(finishParts[1]).toString());
        finish.setAttribute("endS", new Integer(finishParts[2]).toString());

        finish.setAttribute("endDS", new Integer(finishParts[3]).toString());

        root.appendChild(start);
        root.appendChild(finish);

        return retval;
    }

    public class ParseException extends Exception {
        ParseException(String message) {
            super(message);
        }
    }

}
