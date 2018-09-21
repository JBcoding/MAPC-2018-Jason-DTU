package data;

import info.AgentArtifact;
import info.ItemArtifact;
import info.StaticInfoArtifact;
import massim.scenario.city.data.Item;

import java.util.*;

public class CDeliverTeam {
    private Set<String> agents;
    private Map<String, Integer> missingAgents = new HashMap<>();

    public CDeliverTeam() {
        agents = new HashSet<>();

        missingAgents = new HashMap<>();
        missingAgents.put("drone", 0); // We set the three scouting drones to deliver later.
        missingAgents.put("motorcycle", 1);
        missingAgents.put("car", 1);
        missingAgents.put("truck", 1);
    }

    public void addAgent(String agentName) {
        String agentRole = AgentArtifact.getEntity(agentName).getRole().getName();
        missingAgents.replace(agentRole, missingAgents.get(agentRole) - 1);
        agents.add(agentName);
    }

    public boolean needThis(String roleName) {
        return missingAgents.get(roleName) > 0;
    }
}
