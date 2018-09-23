package data;

import cartago.GUARD;
import info.AgentArtifact;
import info.ItemArtifact;
import info.StaticInfoArtifact;
import jason.util.Pair;
import massim.scenario.city.data.Item;

import java.util.*;
import java.util.stream.Collectors;

public class CBuildTeam {
    private Set<String> agents;
    private String truckName;

    private List<String> toBuild;

    private Map<String, String> thingsBeingBuild;

    private Map<String, Integer> missingAgents;
    private int nextIndex;
    private int lastChangeRound = 0;

    private List<Item> levelNon0Items;

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

    public synchronized String getTruckName() {
        return truckName;
    }

    public synchronized String thingToBuild(String agentName) {
        if (levelNon0Items == null) {
            levelNon0Items = new ArrayList<>();
            for (Item i : ItemArtifact.getAllItems()) {
                if (i.needsAssembly()) {
                    levelNon0Items.add(i);
                }
            }
        }

        CStorage storage = StaticInfoArtifact.getStorage();

        // If we have nothing to build, build up some level >0 stuff
        while (toBuild.size() == 0) {
            List<Item> missingItems = new ArrayList<>();
            for (Item item : levelNon0Items) {
                if (storage.getAmount(item.getName()) == 0) {
                    missingItems.add(item);
                }
            }

            Item item;

            if (!missingItems.isEmpty()) {
                item = missingItems.get(this.nextIndex % missingItems.size());
            } else {
                item = levelNon0Items.get(this.nextIndex % levelNon0Items.size());
            }

            this.nextIndex++;

            boolean add = true;

            for (Item i : item.getRequiredItems()) {
                if (storage.getAmount(i.getName()) == 0) {
                    add = false;
                    break;
                }
            }

            if (add) {
                build(item.getName());
            } else {
                Set<Item> missing = item.getRequiredItems().stream()
                        .filter(Item::needsAssembly)
                        .filter(i -> storage.getAmount(i.getName()) == 0)
                        .filter(i -> !toBuild.contains(i.getName()))
                        .collect(Collectors.toSet());

                for (Item i : missing) {
                    build(i.getName());
                }
            }
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

    public synchronized void requestHelp(String agentName) {
        int step = StaticInfoArtifact.getCurrentStep();
        if (lastChangeRound == step) {
            return;
        }

        lastChangeRound = step;
        truckName = agentName;
    }
}
