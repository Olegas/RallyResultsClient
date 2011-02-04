package ru.elifantiev.rallyresults.infrastructure;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.LinkedList;


public class RallySection {

    public String getSectionName() {
        return sectionName;
    }

    public String getCompetitionName() {
        return competitionName;
    }

    protected String sectionName = "", competitionName = "";
    protected LinkedList<StatRecord> stats = new LinkedList<StatRecord>();

    public RallySection(Document svcResponse) {
        Node section = svcResponse.getElementsByTagName("section").item(0);
        Node competition = svcResponse.getElementsByTagName("competition").item(0);
        sectionName = section.getFirstChild().getNodeValue();
        competitionName = competition.getFirstChild().getNodeValue();
        int competitionId = new Integer(competition.getAttributes().getNamedItem("id").getNodeValue());
        int sectionId = new Integer(section.getAttributes().getNamedItem("id").getNodeValue());


        NodeList statsNodes = svcResponse.getElementsByTagName("record");
        for(int i = 0, l = statsNodes.getLength(); i < l; i++) {
            try {
                stats.add(new StatRecord(statsNodes.item(i), competitionId, sectionId));
            } catch (StatRecord.ParseException e) {
                // ignore
            }
        }
    }

    public RallySection() {

    }

    public LinkedList<StatRecord> getStats() {
        return stats;
    }
}
