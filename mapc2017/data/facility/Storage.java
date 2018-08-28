package mapc2017.data.facility;

import java.util.Map;

import mapc2017.data.item.ItemList;

public class Storage extends Facility {

	private int			cap, used;
	private ItemList	stored;
	private ItemList	delivered;
	
	private boolean		isRetrieving;
	
	public Storage(Facility facility, int cap, int used, 
			Map<String, Integer> stored, Map<String, Integer> delivered) {
		super(facility);
		this.cap 		= cap;
		this.used		= used;
		this.stored		= new ItemList(stored);
		this.delivered	= new ItemList(delivered);
		
		this.isRetrieving = false;
	}
	
	public void update(Storage s) {
		this.used		= s.used;
		this.stored 	= s.stored;
		this.delivered	= s.delivered;
		
		if (s.delivered.isEmpty()) 
			this.isRetrieving = false;
	}

	public int getCap() {
		return cap;
	}

	public int getUsed() {
		return used;
	}

	public ItemList getStored() {
		return stored;
	}

	public ItemList getDelivered() {
		return delivered;
	}
	
	public boolean isRetrieving() {
		return isRetrieving;
	}
	
	public void setRetrieving(boolean isRetrieving) {
		this.isRetrieving = isRetrieving;
	}

}
