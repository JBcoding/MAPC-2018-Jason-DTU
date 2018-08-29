package info;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import data.CUtil;
import eis.iilang.Percept;
import env.EIArtifact;
import env.Translator;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.facilities.Shop;

public class ItemArtifact extends Artifact {
	
	private static final Logger logger = Logger.getLogger(ItemArtifact.class.getName());

	private static final String ITEM = "item";

	public static final Set<String>	PERCEPTS = Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(ITEM)));

	private static Map<String, Item>				items 			= new HashMap<>();
	private static Map<String, Map<String, Shop>> 	itemLocations 	= new HashMap<>();	
	
	@OPERATION
	void getItems(OpFeedbackParam<Collection<Item>> ret) {
		ret.set(items.values());
	}
	
	public static Map<Item, Integer> getBaseItems(String name)
	{	
		return items.get(name).getRequiredBaseItems().entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));				
	}

	/**
	 * Format: [map("item0", 2),...]
	 * @param itemMap
	 * @param ret
	 */
	@OPERATION
	void getBaseItems(Object[] itemMap, OpFeedbackParam<Object> ret)
	{	
		// Map each item to its base item,
		// where each base item amount is multiplied with the amount of items needed.
		// Flat mapped and same items is combined using SUM
		ret.set(CUtil.toStringMap(getBaseItems(Translator.convertASObjectToMap(itemMap))));
	}
	
	public static Map<Item, Integer> getBaseItems(Map<Item, Integer> items)
	{
		return items.entrySet().stream()
				.map(item -> getBaseItems(item.getKey().getName()).entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey, 
								entry -> entry.getValue() * item.getValue()))
						.entrySet())
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, Integer::sum));
	}
	
	public static Map<Item, Integer> getItemMap(Map<String, Integer> map)
	{
		return map.entrySet().stream().collect(Collectors.toMap(e -> items.get(e.getKey()), Entry::getValue));
	}
	
	@OPERATION 
	void getRequiredItems(Object itemName, OpFeedbackParam<Object> ret)
	{
		ret.set(items.get(itemName).getRequiredItems().stream()
				.collect(Collectors.toMap(Item::getName, x -> x)));
	}
	
	/**
	 * @param item The item for which a shop selling it should be found
	 * @return A collection of all the shops selling the given item
	 */
	public static Collection<Shop> getShopSelling(String item)
	{
		return itemLocations.get(item).values();
	}
	
	@OPERATION
	void getNearestShopSelling(String item, OpFeedbackParam<String> ret)
	{
		Location loc = AgentArtifact.getEntity(getOpUserName()).getLocation();
		
		Optional<Shop> shop = getShopSelling(item).stream().min((x, y) -> distance(x.getLocation(), loc) - distance(y.getLocation(), loc));
		
		if (shop.isPresent())
		{
			ret.set(shop.get().getName());
		}
		else 
		{
			ret.set("none");
		}
	}
	
	public int distance(Location x, Location y)
	{
		return (int) (100000 * Math.sqrt(Math.pow(x.getLat() - y.getLat(), 2)
				+ Math.pow(x.getLon() - y.getLon(), 2)));
	}
	
	@OPERATION
	void getShopsSelling(String item, OpFeedbackParam<Collection<Shop>> ret) 
	{
		ret.set(getShopSelling(item));
	}
	
	@OPERATION
	void getShoppingList(Object[] itemsMap, OpFeedbackParam<Object> ret)
	{
		ret.set(getShoppingList(Translator.convertASObjectToMap(itemsMap)).entrySet().stream()
				.collect(Collectors.toMap(shop -> shop.getKey().getName(), map -> map.getValue().entrySet().stream()
						.collect(Collectors.toMap(item -> item.getKey().getName(), amount -> amount.getValue())))));
	}
	
	/**
	 * Converts a map of items to buy into a shopping list
	 * @param items The items to buy along with the amount
	 * @return A map of shops and what to buy where
	 */
	public static Map<Shop, Map<Item, Integer>> getShoppingList(Map<Item, Integer> items)
	{	
		Map<Shop, Map<Item, Integer>> shoppingList = new HashMap<>();

		return shoppingList;
	}

	@OPERATION
	void getClosestFacilitySelling(String item, OpFeedbackParam<String> ret)
	{
		Location agentLocation = AgentArtifact.getEntity(getOpUserName()).getLocation();
		
		Collection<Shop> shops = itemLocations.get(item).values();
		
		ret.set(FacilityArtifact.getClosestFacility(agentLocation, shops));
	}
		
	/**
	 * @param items Map of all the items
	 * @return Get the total volume of all the items in the map
	 */
	public static int getVolume(Map<Item, Integer> items)
	{
		return items.entrySet().stream()
				.mapToInt(item -> item.getKey().getVolume() * item.getValue())
				.sum();
	}
	
	/**
	 * Format: [map("item1", 10),...]
	 * @param input A AS map of item names and amount
	 * @param ret The total volume of all the items in the input
	 */
	@OPERATION
	void getVolume(Object[] input, OpFeedbackParam<Integer> ret)
	{
		ret.set(ItemArtifact.getVolume(Translator.convertASObjectToMap(input)));
	}
	
	
	/**
	 * @param item Name of the item
	 * @param ret The volume of all the base items required to assemble this item
	 */
	@OPERATION 
	void getBaseItemVolume(String item, OpFeedbackParam<Integer> ret)
	{
		ret.set(ItemArtifact.getVolume(getItem(item).getRequiredBaseItems()));
	}
	
	/**
	 * Format: [map("item1", 10),...]
	 * @param input An AS map of items and amount
	 * @param ret The total volume of all the items' base items
	 */
	@OPERATION
	void getBaseItemVolume(Object[] input, OpFeedbackParam<Integer> ret)
	{
		ret.set(ItemArtifact.getVolume(Translator.convertASObjectToMap(input)));
	}
	
	@OPERATION
	void getItemsToCarry(Object[] items, int capacity, OpFeedbackParam<Object> retRetrieve, OpFeedbackParam<Object> retRest)
	{
		Map<String, Integer> retrieve 	= new HashMap<>();
		Map<String, Integer> rest		= new HashMap<>();

		for (Entry<Item, Integer> entry : Translator.convertASObjectToMap(items).entrySet())
		{
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
	
	public static void perceiveInitial(Collection<Percept> percepts)
	{		
		Map<Item, Set<Object[]>> requirements = new HashMap<>();
		
		percepts.stream().filter(percept -> percept.getName().equals(ITEM))
						 .forEach(item -> perceiveItem(item, requirements));
		
		// Item requirements has to be added after all items have been created, 
		// since they are not necessarily given in a chronological order.
		// TODO: Tools and items that require assembly have a volume of 0.

        throw new Error("TODO");

		/*for (Entry<Item, Set<Object[]>> entry : requirements.entrySet())
		{
			Item item = entry.getKey();
			
			for (Object[] part : entry.getValue())
			{
				String itemId   = (String) part[0];
				int    quantity = (int)    part[1];
				
				item.addRequirement(items.get(itemId), quantity);
			}
		}

		if (EIArtifact.LOGGING_ENABLED)
		{
			logger.info("Perceived items:");		
			for (Item item : items.values())
				logger.info(item.toString());
		}*/
	}

	// Literal(String, int, Literal(List<String>), Literal(List<List<String, int>>))
	private static void perceiveItem(Percept percept, Map<Item, Set<Object[]>> requirements)
	{				
		Object[] args = Translator.perceptToObject(percept);
		
		String     id		= (String) args[0];
		int 	   volume	= (int)    args[1];

        Item item = new Item(id, volume, Collections.emptySet(), Collections.emptySet());

        // TODO: FIX

        throw new Error("TODO");

        /*for (Object rolesArg : ((Object[]) ((Object[]) args[2])[0]))
        {
            String toolId = (String) rolesArg;

            if (!tools.containsKey(toolId))
            {
                tools.put(toolId, new Tool(toolId, 0, 0));
            }
            item.addRequiredTool(tools.get(toolId));
        }

        Set<Object[]> parts = new HashSet<>();

        for (Object part : ((Object[]) ((Object[]) args[3])[0]))
        {
            parts.add((Object[]) part);
        }
        items.put(id, item);
        requirements.put(item, parts);*/

    }

	// Used by the FacilityArtifact when adding items to shops.
	public static Item getItem(String itemId)
	{
        if (items.containsKey(itemId)) return items.get(itemId);
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
}
