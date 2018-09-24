package data;

import info.AgentArtifact;
import info.FacilityArtifact;
import info.ItemArtifact;
import info.StaticInfoArtifact;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.data.facilities.ResourceNode;
import massim.scenario.city.data.facilities.Storage;
import massim.scenario.city.data.facilities.Workshop;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CStorage {

    private Storage storage;
    private Workshop mainWorkshop;

    private Map<String, Double> itemsVar;
    private Map<String, Integer> items;

    private HashMap<String, Integer> reserved;

    public boolean gatherEnabled = true;

    public CStorage() {
        Collection<Facility> storages = FacilityArtifact.getFacilities(FacilityArtifact.STORAGE);
        Collection<Facility> workshops = FacilityArtifact.getFacilities(FacilityArtifact.WORKSHOP);
        Facility bestStorage = null;
        Workshop bestWorkshop = null;
        double bestDistance = Double.MAX_VALUE;
        for (Facility storage : storages) {
            for (Facility workshop : workshops) {
                double dist = StaticInfoArtifact.getMap().getLength(storage.getLocation(), workshop.getLocation());
                if (dist < bestDistance) {
                    bestDistance = dist;
                    bestStorage = storage;
                    bestWorkshop = (Workshop) workshop;
                }
            }
        }
        storage = (Storage) bestStorage;
        mainWorkshop = bestWorkshop;
        items = new HashMap<>();
        itemsVar = new HashMap<>();
        for (Item item : ItemArtifact.getAllItems()) {
            items.put(item.getName(), 0);
            itemsVar.put(item.getName(), 0.0);
        }

        reserved = new HashMap<>();
    }

    private double getItemCountWithVar(String name) {
        return getAmount(name) + itemsVar.get(name);
    }

    public synchronized Facility getLowestResourceNode(AgentArtifact agent) {
        if (!gatherEnabled) {
            return null;
        }

        String agentName = agent.agentName;
        int speed = agent.getEntity().getCurrentSpeed();

        Collection<Facility> nodes = FacilityArtifact.getFacilities(FacilityArtifact.RESOURCE_NODE);

        ResourceNode bestNode = null;
        double lowestAmount = Double.MAX_VALUE;

        Location storageLoc = getMainStorageFacility().getLocation();

        for (Facility nodeF : nodes) {
            ResourceNode node = (ResourceNode) nodeF;
            if (getItemCountWithVar(node.getResource().getName()) < lowestAmount
                || (bestNode != null
                    && getItemCountWithVar(node.getResource().getName()) <= lowestAmount
                    && StaticInfoArtifact.getRoute(agentName, node.getLocation(), storageLoc).getRouteDuration(speed)
                    < StaticInfoArtifact.getRoute(agentName, bestNode.getLocation(), storageLoc).getRouteDuration(speed))) {
                lowestAmount = getItemCountWithVar(node.getResource().getName());
                bestNode = node;
            }
        }

        if (bestNode == null) {
            return FacilityArtifact.getFacility(FacilityArtifact.CHARGING_STATION);
        }

        itemsVar.put(bestNode.getResource().getName(), itemsVar.get(bestNode.getResource().getName()) + 0.0001);

        return bestNode;
    }

    public Storage getMainStorageFacility() {
        return storage;
    }

    public void updateItemCount(String name, int quantity) {
        items.put(name, quantity);
    }

    public void clearItemsCount() {
        for (String key : items.keySet()) {
            items.put(key, 0);
        }
    }

    public String getMainWorkShop() {
        return mainWorkshop.getName();
    }

    public synchronized void reserve(String itemName, int amount) {
        reserved.put(itemName, amount + reserved.getOrDefault(itemName, 0));
    }

    public synchronized void unreserve(String itemName, int amount) {
        reserved.put(itemName, reserved.getOrDefault(itemName, 0) - amount);
    }

    public synchronized int getAmount(String itemName) {
        int amount = items.getOrDefault(itemName, 0);

        if (amount > 0) {
            amount -= reserved.getOrDefault(itemName, 0);
        }

        return Math.max(amount, 0);
    }

    public synchronized int getActualAmount(String itemName) {
        return items.getOrDefault(itemName, 0);
    }
}
