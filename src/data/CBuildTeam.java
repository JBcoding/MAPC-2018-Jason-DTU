package data;

import info.AgentArtifact;
import info.ItemArtifact;
import info.StaticInfoArtifact;
import massim.scenario.city.data.Item;

import java.util.*;
import java.util.stream.Collectors;

public class CBuildTeam {
    private Set<String> agents;
    private String truckName;

    private List<String> toBuild;

    private Map<String, String> thingsBeingBuild;

    private Map<String, Integer> missingAgents = new HashMap<>();
    private int nextIndex;

    public CBuildTeam() {
        agents = new HashSet<>();
        toBuild = new ArrayList<>();
        thingsBeingBuild = new HashMap<>();

        missingAgents = new HashMap<>();
        missingAgents.put("drone", 1);
        missingAgents.put("motorcycle", 1);
        missingAgents.put("car", 1);
        missingAgents.put("truck", 3);
    }

    public void addAgent(String agentName) {
        if (AgentArtifact.getEntity(agentName).getRole().getName().equals("truck")) {
            truckName = agentName;
        }
        String agentRole = AgentArtifact.getEntity(agentName).getRole().getName();
        missingAgents.replace(agentRole, missingAgents.get(agentRole) - 1);
        agents.add(agentName);
    }

    public void build(String itemName) {
        Item item = ItemArtifact.getItem(itemName);
        for (Item part : item.getRequiredItems()) {
            if (part.needsAssembly()) {
                build(part.getName());
            }
        }
        toBuild.add(itemName);
    }

    public String getTruckName() {
        return truckName;
    }

    public String thingToBuild(String agentName) {
        // If we have nothing to build, build op some level 1 stuff
        if (toBuild.size() == 0) {
            Set<Item> level1Set = ItemArtifact.getLevel1Items();
            Item[] level1s = level1Set.toArray(new Item[level1Set.size()]);
            Item item = level1s[this.nextIndex % level1s.length];
            this.nextIndex++;
            build(item.getName());
            System.out.println("HEREEER________ " + toBuild);
        }

        if (!thingsBeingBuild.containsKey(agentName)) {
            thingsBeingBuild.put(agentName, toBuild.get(0));
            toBuild.remove(0);
        }
        return thingsBeingBuild.get(agentName);
    }

    public void iAmDone(String agentName) {
        thingsBeingBuild.remove(agentName);
    }

    public boolean needThis(String roleName) {
        return missingAgents.get(roleName) > 0;
    }

    private int lastChangeRound = 0;
    public void requestHelp(String agentName) {
        if (lastChangeRound == StaticInfoArtifact.getCurrentStep()) {
            return;
        }
        lastChangeRound = StaticInfoArtifact.getSteps();
        truckName = agentName;
    }
}
