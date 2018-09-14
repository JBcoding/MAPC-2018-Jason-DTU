package info;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cartago.Artifact;
import cartago.GUARD;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import data.CUtil;
import eis.iilang.Percept;
import env.EIArtifact;
import env.Translator;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Role;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.data.facilities.ResourceNode;
import massim.scenario.city.data.facilities.Shop;

public class ItemArtifact extends Artifact {

    private static final Logger logger = Logger.getLogger(ItemArtifact.class.getName());

    private static final String ITEM = "item";

    public static final Set<String> PERCEPTS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(ITEM)));

    private static Map<String, Item> items = new HashMap<>();
    private static Map<String, Map<String, Shop>> itemLocations = new HashMap<>();

    public static Iterable<? extends Item> getAllItems() {
        return items.values();
    }

    @OPERATION
    void getItems(OpFeedbackParam<Collection<Item>> ret) {
        ret.set(items.values());
    }

    public static Map<Item, Integer> getBaseItems(String name) {
        return items.get(name).getRequiredBaseItems();
    }

    /**
     * Format: [map("item0", 2),...]
     *
     * @param itemMap
     * @param ret
     */
    @OPERATION
    void getBaseItems(Object[] itemMap, OpFeedbackParam<Object> ret) {
        // Map each item to its base item,
        // where each base item amount is multiplied with the amount of items needed.
        // Flat mapped and same items is combined using SUM
        ret.set(CUtil.toStringMap(getBaseItems(Translator.convertASObjectToMap(itemMap))));
    }

    public static Map<Item, Integer> getBaseItems(Map<Item, Integer> items) {
        return items.entrySet().stream()
                .map(item -> getBaseItems(item.getKey().getName()).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> entry.getValue() * item.getValue()))
                        .entrySet())
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, Integer::sum));
    }

    public static Map<Item, Integer> getItemMap(Map<String, Integer> map) {
        return map.entrySet().stream().collect(Collectors.toMap(e -> items.get(e.getKey()), Entry::getValue));
    }

    @OPERATION
    void getRequiredItems(Object itemName, OpFeedbackParam<Object> ret) {
        ret.set(items.get(itemName).getRequiredItems().stream()
                .collect(Collectors.toMap(Item::getName, x -> 1)));
    }

    /**
     * @param item The item for which a shop selling it should be found
     * @return A collection of all the shops selling the given item
     */
    public static Collection<Shop> getShopSelling(String item) {
        return itemLocations.get(item).values();
    }

    @OPERATION
    void getNearestShopSelling(String item, OpFeedbackParam<String> ret) {
        Location loc = AgentArtifact.getEntity(getOpUserName()).getLocation();

        Optional<Shop> shop = getShopSelling(item).stream().min((x, y) -> distance(x.getLocation(), loc) - distance(y.getLocation(), loc));

        if (shop.isPresent()) {
            ret.set(shop.get().getName());
        } else {
            ret.set("none");
        }
    }

    public int distance(Location x, Location y) {
        return (int) (100000 * Math.sqrt(Math.pow(x.getLat() - y.getLat(), 2)
                + Math.pow(x.getLon() - y.getLon(), 2)));
    }

    @OPERATION
    void getShopsSelling(String item, OpFeedbackParam<Collection<Shop>> ret) {
        ret.set(getShopSelling(item));
    }

    @OPERATION
    void getResourceList(Object[] itemsMap, OpFeedbackParam<Object> ret) {
        ret.set(getResourceList(Translator.convertASObjectToMap(itemsMap)).entrySet().stream()
                .collect(Collectors.toMap(node -> node.getKey().getName(), Entry::getValue)));
    }

    @GUARD
    private boolean doneScouting() {
        return !AgentArtifact.isScouting;
    }

    /**
     * Converts a map of items to buy into a shopping list
     *
     * @param items The items to buy along with the amount
     * @return A map of shops and what to buy where
     */
    public Map<ResourceNode, Integer> getResourceList(Map<Item, Integer> items) {
        // TODO: be more eager than this.
        await("doneScouting");

        Map<ResourceNode, Integer> resourceList = new HashMap<>();

        Map<String, ResourceNode> nodes = FacilityArtifact.getResourceNodes();

        for (Entry<Item, Integer> entry : items.entrySet()) {
            Item item = entry.getKey();
            int amount = entry.getValue();

            if (item.needsAssembly()) {
                throw new IllegalArgumentException("Received non-base items");
            }

            // TODO: threshold is probably always 0
            Optional<ResourceNode> best = nodes.values().stream()
                    .filter(x -> x.getResource().equals(item))
                    .min(Comparator.comparingInt(ResourceNode::getThreshold));

            if (best.isPresent()) {
                resourceList.put(best.get(), amount);
            } else {
                // This probably means something is wrong with scouting.
                logger.severe("No known resource node for " + item);
            }
        }

        return resourceList;
    }

    @OPERATION
    void getClosestFacilitySelling(String item, OpFeedbackParam<String> ret) {
        // TODO: don't trust this
        Location agentLocation = AgentArtifact.getEntity(getOpUserName()).getLocation();

        Collection<Shop> shops = itemLocations.get(item).values();

        ret.set(FacilityArtifact.getClosestFacility(agentLocation, shops));
    }

    /**
     * @param items Map of all the items
     * @return Get the total volume of all the items in the map
     */
    public static int getVolume(Map<Item, Integer> items) {
        return items.entrySet().stream()
                .mapToInt(item -> item.getKey().getVolume() * item.getValue())
                .sum();
    }

    /**
     * Format: [map("item1", 10),...]
     *
     * @param input A AS map of item names and amount
     * @param ret   The total volume of all the items in the input
     */
    @OPERATION
    void getVolume(Object[] input, OpFeedbackParam<Integer> ret) {
        ret.set(ItemArtifact.getVolume(Translator.convertASObjectToMap(input)));
    }


    /**
     * @param item Name of the item
     * @param ret  The volume of all the base items required to assemble this item
     */
    @OPERATION
    void getBaseItemVolume(String item, OpFeedbackParam<Integer> ret) {
        ret.set(ItemArtifact.getVolume(getItem(item).getRequiredBaseItems()));
    }

    /**
     * Format: [map("item1", 10),...]
     *
     * @param input An AS map of items and amount
     * @param ret   The total volume of all the items' base items
     */
    @OPERATION
    void getBaseItemVolume(Object[] input, OpFeedbackParam<Integer> ret) {
        ret.set(ItemArtifact.getVolume(Translator.convertASObjectToMap(input)));
    }

    @OPERATION
    void getAmountToCarry(String itemId, int amount, int capacity, OpFeedbackParam<Integer> retRetrieve, OpFeedbackParam<Integer> retRest) {
        Item item = items.get(itemId);

        int volume = item.getVolume();
        int retrieve = Math.min(capacity / volume, amount);
        int rest = amount - retrieve;

        retRetrieve.set(retrieve);
        retRest.set(rest);
    }

	@OPERATION
	void getItemsToCarry(Object[] items, int capacity, OpFeedbackParam<Object> retRetrieve, OpFeedbackParam<Object> retRest)
	{
		Map<String, Integer> retrieve 	= new HashMap<>();
		Map<String, Integer> rest		= new HashMap<>();

		for (Entry<Item, Integer> entry : Translator.convertASObjectToMap(items).entrySet()) {
			Item 	item 	= entry.getKey();
			int 	amount 	= entry.getValue();
			int		volume	= capacity + 1;

			if (item.getRequiredBaseItems().isEmpty())	volume = item.getVolume() * amount;
			else										volume = ItemArtifact.getVolume(item.getRequiredBaseItems()) * amount;

			if (volume <= capacity)
			{
				capacity -= volume;
				retrieve.put(item.getName(), amount);
			}
			else
			{
				rest.put(item.getName(), amount);
			}
		}

		retRetrieve.set(retrieve);
		retRest.set(rest);
	}

	@OPERATION
    void getRequiredRoles(Object[] items, OpFeedbackParam<Object> ret) {
        Set<String> roles = new HashSet<>();

        for (Entry<Item, Integer> entry : Translator.convertASObjectToMap(items).entrySet()) {
            Item item = entry.getKey();
            for (Role role : item.getRequiredRoles()) {
                roles.add(role.getName());
            }
        }

        ret.set(roles);
    }
	
	public static void perceiveInitial(Collection<Percept> percepts)
	{		
		Map<Item, Set<String>> requirements = new HashMap<>();
		
		percepts.stream().filter(percept -> percept.getName().equals(ITEM))
						 .forEach(item -> perceiveItem(item, requirements));
		
		// TODO: Tools and items that require assembly have a volume of 0. -- AHF: Still relevant?

        // This is annoying since we cannot add parts to items after the fact.
        // Loop through the requirements map until empty and add an item to `items` if all
        // requirements are already in `items`.

        while (!requirements.isEmpty()) {
            Item finished = null;

            for (Entry<Item, Set<String>> entry : requirements.entrySet()) {
                Item item = entry.getKey();

                HashSet<Item> partSet = new HashSet<>();

                boolean missing = false;

                for (String partId : entry.getValue()) {
                    if (items.containsKey(partId)) {
                        partSet.add(items.get(partId));
                    } else {
                        missing = true;
                        break;
                    }
                }

                if (missing) {
                    continue;
                }

                Item newItem = new Item(item.getName(), item.getVolume(), partSet, item.getRequiredRoles());
                items.put(newItem.getName(), newItem);
                finished = item;
                break;
            }

            if (finished != null) {
                requirements.remove(finished);
            }
        }

		if (EIArtifact.LOGGING_ENABLED)
		{
			logger.info("Perceived items:");		
			for (Item item : items.values())
				logger.info(item.toString());
		}
	}

	// Literal(String, int, Literal(List<String>), Literal(List<List<String, int>>))
	private static void perceiveItem(Percept percept, Map<Item, Set<String>> requirements)
	{				
		Object[] args = Translator.perceptToObject(percept);
		
		String id = (String) args[0];
		int volume = (int) args[1];
		Object[] roles = (Object[]) args[2];
        Object[] parts = (Object[]) args[3];


        HashSet<Role> roleSet = new HashSet<>();
        for (Object roleId : (Object[]) roles[0]) {
            Role role = StaticInfoArtifact.getRole((String) roleId);
            roleSet.add(role);
        }

        HashSet<String> partSet = new HashSet<>();
        for (Object partId : (Object[]) parts[0]) {
            // We cannot process the parts yet as we do not know of all items.
            // Instead we add just the identifier here and deal with it in perceiveInitial above.
            partSet.add((String) partId);
        }

        Item item = new Item(id, volume, Collections.emptySet(), roleSet);

        // Only put base items into items and store the rest as keys in requirements.
        // perceiveInitial above will populate items in the correct order.
        if (requirements.isEmpty()) {
            items.put(id, item);
        }
        requirements.put(item, partSet);
    }

	// Used by the FacilityArtifact when adding items to shops.
	public static Item getItem(String itemId)
	{
        if (items.containsKey(itemId)) return items.get(itemId);
        logger.warning("Unknown item: " + itemId);
        return null;
	}
	
	// Used by the FacilityArtifact when adding shops
	protected static void addItemLocation(String itemId, Shop shop)
	{
		if (itemLocations.containsKey(itemId))
		{
			itemLocations.get(itemId).put(shop.getName(), shop);
		}
		else
		{			
			itemLocations.put(itemId, new HashMap<>());
			itemLocations.get(itemId).put(shop.getName(), shop);
		}
	}

	public static void reset() 
	{
		items 			= new HashMap<>();
		itemLocations 	= new HashMap<>();
	}

	public static Set<Item> getLevel0Items() {
	    Set<Item> baseItems = new HashSet<>();
	    for (Item item : items.values()) {
	        if (!item.needsAssembly()) {
	            baseItems.add(item);
            }
        }
        return baseItems;
    }
}
