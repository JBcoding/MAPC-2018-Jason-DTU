package mapc2017.data.facility;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mapc2017.data.item.ItemList;

public class Shop extends Facility {
	
	private int 		restock;
	private ItemList 	price;
	private ItemList 	amount;
	private ItemList 	reserved;
	
	private Set<String> itemsExcludingTools;

	public Shop(Facility facility, int restock, 
			Map<String, Integer> price, Map<String, Integer> amount) {
		super(facility);
		this.restock	= restock;
		this.price		= new ItemList(price);
		this.amount		= new ItemList(amount);
		this.reserved	= new ItemList();
		
		this.itemsExcludingTools = amount.keySet().stream()
									.filter(s -> s.startsWith("item"))
									.collect(Collectors.toSet());
	}
	
	public void update(Shop s) {
		this.price  = s.price;
		this.amount = s.amount;
	}
	
	public int getRestock() {
		return restock;
	}
	
	public int getPrice(String item) {
		return price.get(item);
	}
	
	public int getAmount(String item) {
		return amount.get(item);
	}
	
	public int getAvailableAmount(String item) {
		Integer x = reserved.get(item);
		return amount.get(item) - (x != null ? x : 0);
	}
	
	public Set<String> getItems() {
		return amount.keySet();
	}
	
	public Set<String> getItemsExcludingTools() {
		return itemsExcludingTools;
	}
	
	public ItemList getAmount() {
		return new ItemList(amount);
	}
	
	public ItemList getReserved() {
		return new ItemList(reserved);
	}
	
	public void addReserved(String item, int amount) {
		this.reserved.add(item, amount);
	}
	
	public void remReserved(String item, int amount) {
		this.reserved.subtract(item, amount);
	}

}
