package mapc2017.env.info;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import mapc2017.data.facility.Shop;
import mapc2017.data.item.Item;
import mapc2017.data.item.ItemList;
import mapc2017.data.item.Tool;

public class ItemInfo {
	
	private static ItemInfo instance;	
	public  static ItemInfo get() { return instance; }

	private Map<String, Tool> 				tools 			= new HashMap<>();
	private Map<String, Item>				items 			= new HashMap<>();
	private Map<String, Map<String, Shop>> 	itemLocations 	= new HashMap<>();
	
	public ItemInfo() {
		instance = this;
	}
	
	/////////////
	// GETTERS //
	/////////////
	
	public Item getItem(String name) {
		if (name.startsWith("tool")) return tools.get(name);
		else						 return items.get(name);
	}
	
	public Collection<Item> getItems() {
		return items.values();
	}
	
	public Tool getTool(String name) {
		return tools.get(name);
	}
	
	public Collection<Tool> getTools() {
		return tools.values();
	}
	
	public Map<String, Map<String, Shop>> getAllItemLocations() {
		return itemLocations;
	}
	
	public Set<Shop> getItemLocations(String item) {
		return new HashSet<>(getAllItemLocations().get(item).values());
	}
	
	/////////////
	// SETTERS //
	/////////////

	public void addItem(Item item) {
		if (item instanceof Tool) 	tools.put(item.getName(), (Tool) item);
		else 						items.put(item.getName(), 		 item);
	}
	
	public void addItemLocation(String item, Shop shop) {
		if (itemLocations.containsKey(item)) {
			itemLocations.get(item).put(shop.getName(), shop);
		} else {			
			itemLocations.put(item, new HashMap<>());
			itemLocations.get(item).put(shop.getName(), shop);
		}
	}
	
	public void clearItems() {
		items.clear();
		tools.clear();
	}
	
	public void clearItemLocations() {
		itemLocations.clear();
	}
	
	/////////////
	// METHODS //
	/////////////
	
	public boolean isItemsAvailable(Map<String, Integer> items) 
	{
		for (Entry<String, Integer> entry : items.entrySet())
		{
			String 	item 	= entry.getKey();
			int 	amount	= entry.getValue();
			
			Collection<Shop> shops = getItemLocations(item);
			
			int availableAmount = shops.stream().mapToInt(s -> s.getAvailableAmount(item)).sum();
			
			if (availableAmount < amount) return false;
		}
		return true;
	}
	
	public synchronized ItemList getLeastAvailableItems(AgentInfo agent, Shop shop) 
	{
		Optional<Item> opt = shop.getItemsExcludingTools().stream()
				.map(this::getItem)
				.min(Comparator.comparingDouble(Item::getAvailability));
		
		if (!opt.isPresent()) return null;
		
		Item 	item 	 = opt.get();
		int 	available = Math.min(10, shop.getAvailableAmount(item.getName()));
		
		int amount = Math.min(available, agent.getCapacity() / item.getVolume());
		
		if (amount <= 3) return null;
		
		shop.addReserved(item.getName(), amount);
		
		return new ItemList(item.getName(), amount);
	}
	
	public ItemList getBaseItems(Map<String, Integer> items) {
		return new ItemList(stringToItemMap(items).entrySet().stream()
				.map(item -> item.getKey().getReqBaseItems().entrySet().stream()
						.collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue() * item.getValue())).entrySet())
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, Integer::sum)));
	}
	
	public Set<String> getBaseTools(Map<String, Integer> items) {
		return stringToItemMap(items).keySet().stream()
				.map(Item::getReqBaseTools)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}
	
	public int getVolume(Collection<String> tools) {
		return tools.stream()
				.map(this::getTool)
				.mapToInt(Tool::getVolume)
				.sum();
	}
	
	public int getVolume(Map<String, Integer> items) {
		return getItemVolume(stringToItemMap(items));
	}
	
	public int getBaseVolume(Map<String, Integer> items) {
		return getVolume(getBaseItems(items));
	}

	private Item getItem(Entry<String, ?> entry) {
		return this.getItem(entry.getKey());
	}

	public Map<Item, Integer> stringToItemMap(Map<String, Integer> items) {
		return items.entrySet().stream().collect(Collectors.toMap(this::getItem, Entry::getValue));
	}
	
	public static int getItemVolume(Map<Item, Integer> items) {
		return items.entrySet().stream().mapToInt(item -> 
			item.getKey().getVolume() * item.getValue()).sum();
	}
}
