package mapc2017.data.item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import mapc2017.data.facility.Shop;
import mapc2017.env.info.ItemInfo;

public class ShoppingList extends HashMap<String, ItemList> {

	private static final long serialVersionUID = 1L;
	
	public ShoppingList() {
		super();
	}
	
	public ShoppingList(String shop, ItemList items) {
		super();
		this.put(shop, items);
	}
	
	public ShoppingList(String shop, ItemList items1, ItemList items2) {
		super();
		ItemList items = new ItemList(items1);
		items.add(items2);
		this.put(shop, items);
	}
	
	public void put(String shop, String item, int amount) {
		if (this.containsKey(shop))	this.get(shop).put(item, amount);
		else						this.put(shop, new ItemList(item, amount));
	}

	/**
	 * Creates and returns a ShoppingList based on the given items.
	 */
	public static ShoppingList getShoppingList(Map<String, Integer> items)
	{	
		ShoppingList shoppingList 	= new ShoppingList();
		ItemInfo 	 iInfo 			= ItemInfo.get();
//		Workshop	 workshop		= (Workshop) FacilityInfo.get().getFacility(workshopName);
		
		for (Entry<String, Integer> entry : iInfo.getBaseItems(items).entrySet())
		{			
			String 	item 	= entry.getKey();
			int 	amount 	= entry.getValue();
			
			Collection<Shop> shops = iInfo.getItemLocations(item);

//			Optional<Shop> opt = shops.stream().filter(s -> s.getAvailableAmount(item) >= amount).min(Comparator.comparingInt(s -> StaticInfo.get().getRouteLength(s.getLocation(), workshop.getLocation())));

			while (amount > 0)
			{				
				Shop shop = shops.stream().max((x, y) -> 
								x.getAvailableAmount(item) - 
								y.getAvailableAmount(item)).get();
				
				shops.remove(shop);
				
				int buyAmount = shops.isEmpty() ? amount : Math.min(amount, shop.getAvailableAmount(item));
				
				if (buyAmount > 0) shoppingList.put(shop.getName(), item, buyAmount);
				
				amount -= buyAmount;
			}
		}		
		
		for (String tool : iInfo.getBaseTools(items))
		{
			Collection<Shop> shops = iInfo.getItemLocations(tool);
			
			Optional<Shop> shopOpt = shops.stream()
							.filter(s -> shoppingList.containsKey(s.getName())).findAny();
			
			String shop = shopOpt.isPresent() 	? shopOpt.get().getName() 
												: shops.stream().findAny().get().getName();
			
			shoppingList.put(shop, tool, 1);
		}
		
		return shoppingList;
	}
}
