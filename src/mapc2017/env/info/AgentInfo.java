package mapc2017.env.info;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mapc2017.data.Role;
import mapc2017.data.item.Item;
import mapc2017.data.item.ItemList;
import mapc2017.data.item.Tool;
import massim.scenario.city.data.Location;
import massim.scenario.city.util.GraphHopperManager;

public class AgentInfo {
	
	private static Map<String, AgentInfo> instances = new HashMap<>();	
	public  static AgentInfo get(String agent) { return instances.get(agent); }	
	public  static Collection<AgentInfo> get() { return instances.values(); }
	
	private double		lat,
						lon;
	private int 		charge, 
						load;
	private String		name,
						permission,
						facility,
						lastFacility,
						lastAction,
						lastActionResult;
	private String[]	lastActionParams;
	private Role		role;
	private ItemList 	inventory = new ItemList();
	
	public AgentInfo(String name) { 
		instances.put(name, this); 
		this.name = name;
	}
	
	/////////////
	// GETTERS //
	/////////////
	
	public String getName() {
		return name;
	}
	
	public int getNumber() {
		return Integer.parseInt(getName().replace("agent", ""));
	}

	public Location getLocation() {
		return new Location(lon, lat);
	}
	
	public int getCharge() {
		return charge;
	}
	
	public int getLoad() {
		return load;
	}
	
	public String getFacility() {
		return facility;
	}
	
	public String getLastFacility() {
		return lastFacility;
	}
	
	public String getLastAction() {
		return lastAction;
	}

	public String getLastActionResult() {
		return lastActionResult;
	}

	public synchronized String[] getLastActionParams() {
		return lastActionParams;
	}
	
	public Role getRole() {
		return role;
	}
	
	public synchronized Set<String> getTools() {
		return role.getTools();
	}
	
	public synchronized Set<String> getUsableTools(Set<String> tools) {
		Set<String> usableTools = new HashSet<>(tools);
		usableTools.retainAll(getTools());
		return usableTools;
	}
	
	public String getPermission() {
		return permission;
	}

	public synchronized ItemList getInventory() {
		return new ItemList(inventory);
	}
	
	public synchronized boolean hasItem(String item) {
		return inventory.containsKey(item);
	}
	
	public synchronized int getAmount(String item) {
		return inventory.get(item).intValue();
	}
	
	public int getCapacity() {
		return role.getLoad() - load - 30;
	}
	
	/////////////
	// SETTERS //
	/////////////
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setLon(double lon) {
		this.lon = lon;
	}
	
	public void setCharge(int charge) {
		this.charge = charge;
	}
	
	public void setLoad(int load) {
		this.load = load;
	}
	
	public void setFacility(String facility) {
		this.facility = facility;
	}
	
	public void setLastFacility() {
		this.lastFacility = this.facility;
	}
	
	public void setLastAction(String lastAction) {
		this.lastAction = lastAction;
	}

	public void setLastActionResult(String lastActionResult) {
		this.lastActionResult = lastActionResult;
	}

	public void setLastActionParams(String[] lastActionParams) {
		this.lastActionParams = lastActionParams;
	}
	
	public void setRole(Role role) {
		this.role = role;
		this.permission = role.getName().equals("drone") ? 
				GraphHopperManager.PERMISSION_AIR : 
				GraphHopperManager.PERMISSION_ROAD;
	}

	public synchronized void clearInventory() {
		this.inventory.clear();
	}
	
	public synchronized void addItem(Entry<String, Integer> item) {
		this.inventory.put(item.getKey(), item.getValue());
	}	
	
	/////////////
	// METHODS //
	/////////////
	
	public boolean canUseTool(String tool) 
	{
		return getRole().canUseTool(tool);
	}

	public ItemList getMissingItems(Map<String, Integer> items) 
	{
		ItemList missing = new ItemList(items);		
		
		missing.subtract(getInventory());
		
		return missing;
	}
	
	public Set<String> getMissingTools(Collection<String> tools) 
	{
		Set<String> missing = new HashSet<>(tools);
		
		missing.removeAll(getInventory().keySet());
		
		return missing;
	}
	
	public int getVolumeToCarry(Map<Item, Integer> items)
	{		
		int toCarry	 = 0;
		int capacity = this.getCapacity();

		for (Entry<Item, Integer> entry : items.entrySet())
		{
			Item 	item 		= entry.getKey();
			
			if (item instanceof Tool && !canUseTool(item.getName())) continue;
			
			int 	needAmount 	= entry.getValue();
			int		itemVolume	= item.getReqBaseVolume();
			
//			if (this.hasItem(item.getName()))
//			{
//				int hasAmount 	= this.getAmount(item.getName());
//				toCarry 	   += itemVolume * hasAmount + 200 * hasAmount;
//				needAmount 	   -= hasAmount;		
//			}				
				
			int amountToCarry = Math.min(needAmount, capacity / itemVolume);			
			
			if (amountToCarry > 0) 
			{				
				int vol   = itemVolume * amountToCarry;
				capacity -= vol;
				toCarry  += vol;
			}
		}
		return toCarry;
	}

	public ItemList getItemsToCarry(Map<String, Integer> items)
	{		
		ItemList itemsToCarry = new ItemList();
		
		int capacity = this.getCapacity();
		
		if (capacity > 100) capacity -= 30;

		for (Entry<Item, Integer> entry : ItemInfo.get().stringToItemMap(items).entrySet())
		{
			Item 	item 	= entry.getKey();
			
			if (item instanceof Tool && !canUseTool(item.getName())) continue;
			
			int 	amount 	= entry.getValue();
			int		volume	= item.getReqBaseVolume();

			int amountToCarry = Math.min(amount, capacity / volume);			
			
			if (amountToCarry > 0) 
			{				
				capacity -= volume * amountToCarry;
				
				itemsToCarry.add(item.getName(), amountToCarry);
			}
		}
		return itemsToCarry;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
