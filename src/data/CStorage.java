package data;

import info.FacilityArtifact;
import info.ItemArtifact;
import info.StaticInfoArtifact;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.data.facilities.ResourceNode;
import massim.scenario.city.data.facilities.Storage;
import massim.scenario.city.data.facilities.Workshop;

import java.util.*;
import java.util.stream.Collectors;

public class CStorage {

    Storage storage;
    Workshop mainWorkshop;

    Map<String, Double> itemsVar;
    Map<String, Integer> items;

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
    }

    private double getItemCountWithVar(String name) {
        return items.get(name) + itemsVar.get(name);
    }

    public Facility getLowestResourceNode() {
        Collection<Facility> nodes = FacilityArtifact.getFacilities(FacilityArtifact.RESOURCE_NODE);
        ResourceNode bestNode = null;
        Double lowestAmount = Double.MAX_VALUE;
        for (Facility nodeF : nodes) {
            ResourceNode node = (ResourceNode) nodeF;
            if (getItemCountWithVar(node.getResource().getName()) < lowestAmount) {
                lowestAmount = getItemCountWithVar(node.getResource().getName());
                bestNode = node;
            }
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
}
