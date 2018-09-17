package data;

import info.AgentArtifact;

import java.util.*;

public class CBuildTeam {
    private Set<String> agents;
    private String truckName;

    private List<String> toBuild;

    public CBuildTeam() {
        agents = new HashSet<>();
        toBuild = new ArrayList<>();

        // TODO: REMOVE THIS IS ONLY A TEST
        build("item5");
    }

    public void addAgent(String agentName) {
        if (AgentArtifact.getEntity(agentName).getRole().getName().equals("truck")) {
            truckName = agentName;
        }
        agents.add(agentName);
    }

    public void build(String itemName) {
        toBuild.add(itemName);
    }

    public String getTruckName() {
        return truckName;
    }

    public List<String> thingsToBuild() {
        return toBuild;
    }
}
