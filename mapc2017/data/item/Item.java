package mapc2017.data.item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mapc2017.data.facility.Shop;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;

public class Item {

	private String 					name;
	private int						volume;
	private Set<String> 			reqTools;
	private Map<String, Integer> 	reqItems;
	private double					availability = -1;
	
	private Map<String, Integer>	reqBaseItems;
	private Set<String>				reqBaseTools;
	private int						reqBaseVolume;
	private int						avgPrice;
	
	public Item(String name, int volume, Set<String> tools, Map<String, Integer> parts) {
		this.name 		= name;
		this.volume		= volume;
		this.reqTools	= tools;
		this.reqItems	= parts;
	}
	
	// GETTERS
	
	public String getName() {
		return name;
	}
	
	public int getNumber() {
		return Integer.parseInt(getName().replace("item", ""));
	}
	
	public int getVolume() {
		return volume;
	}
	
	public boolean reqAssembly() {
		return !reqItems.isEmpty();
	}
	
	public Set<String> getReqTools() {
		return new HashSet<>(reqTools);
	}
	
	public Map<String, Integer> getReqItems() {
		return new HashMap<>(reqItems);
	}
	
	public double getAvailability() 
	{
		if (availability < 0)
		{
			calculateItemAvailability();
		}
		return availability;
	}
	
	public Map<String, Integer> getReqBaseItems() 
	{
		if (reqBaseItems == null)
		{
			calculateBaseRequirements();
		}
		return new HashMap<>(reqBaseItems);
	}
	
	public Set<String> getReqBaseTools()
	{
		if (reqBaseItems == null)
		{
			calculateBaseRequirements();
		}
		return reqBaseTools;
	}
	
	public int getReqBaseVolume() 
	{
		if (reqBaseItems == null)
		{
			calculateBaseRequirements();
		}
		return reqBaseVolume;
	}
	
	public int getAvgPrice() 
	{
		if (avgPrice == 0)
		{
			avgPrice = calculateAvgPrice();
		}
		return avgPrice;
	}
	
	public void calculateItemAvailability()
	{
		ItemInfo iInfo = ItemInfo.get();
		
		int availableAmount = 0;
		
		if (!reqAssembly())
		{
			for (Shop shop : iInfo.getItemLocations(name))
			{
				availableAmount += shop.getAvailableAmount(name);
			}
		}
		
		availability = availableAmount / (double) FacilityInfo.get().getShops().size();
	}
	
	public void calculateBaseRequirements()
	{
		if (reqBaseItems != null) return;
		
		// If required items is empty, this item is a base item 
		// and the required base items are itself
		if (!reqAssembly())
		{
			reqBaseItems 	= new HashMap<>();
			reqBaseItems	.put(name, 1);
			reqBaseTools	= reqTools;
			reqBaseVolume 	= volume;
		}
		// If required items is not empty, the required base items
		// are the required base items for each of the required items
		else
		{
			ItemInfo iInfo 	= ItemInfo.get();
			reqBaseItems	= iInfo.getBaseItems(reqItems);
			reqBaseVolume	= iInfo.getVolume(reqBaseItems);
			reqBaseTools	= iInfo.getBaseTools(reqItems);
			reqBaseTools.addAll(this.getReqTools());
		}
	}
	
	public int calculateAvgPrice() 
	{
		if (!reqAssembly())
			return (int) ItemInfo.get().getItemLocations(name).stream()
						.mapToInt(s -> s.getPrice(name)).average().getAsDouble();
		else
			return reqBaseItems.entrySet().stream()
						.mapToInt(Item::getAvgPrice).sum();
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static int getAvgPrice(Entry<String, Integer> e) {
		return ItemInfo.get().getItem(e.getKey()).getAvgPrice() * e.getValue();
	}
}
