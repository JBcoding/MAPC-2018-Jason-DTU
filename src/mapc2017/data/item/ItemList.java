package mapc2017.data.item;

import java.util.HashMap;
import java.util.Map;

public class ItemList extends HashMap<String, Integer> {

	private static final long serialVersionUID = 1L;

	public ItemList() {
		super();
	}
	
	public ItemList(String item, int amount) {
		super();
		this.put(item, amount);
	}
	
	public ItemList(Map<String, Integer> items) {
		super(items);
	}
	
	public int getTotalAmount() {
		return this.values().stream().mapToInt(Integer::intValue).sum();
	}
	
	public void add(String item, int amount) {		
		if (this.containsKey(item))	this.put(item, this.get(item) + amount);
		else 						this.put(item, amount);		
	}
	
	public void subtract(String item, int amount) {
		if (!this.containsKey(item)) return;
		if (amount >= this.get(item)) this.remove(item);
		else 					 	  this.put(item, this.get(item) - amount);
	}
	
	public void add(Map<String, Integer> items) {
		items.entrySet().stream().forEach(e -> add(e.getKey(), e.getValue()));
	}
	
	public void subtract(Map<String, Integer> items) {
		items.entrySet().stream().forEach(e -> subtract(e.getKey(), e.getValue()));
	}
	
	public void retain(Map<String, Integer> items) {
		ItemList temp = new ItemList(this);
		temp.subtract(items);
		this.subtract(temp);
	}
}
