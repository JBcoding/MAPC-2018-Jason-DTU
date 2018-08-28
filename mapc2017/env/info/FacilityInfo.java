package mapc2017.env.info;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import mapc2017.data.facility.ChargingStation;
import mapc2017.data.facility.Dump;
import mapc2017.data.facility.Facility;
import mapc2017.data.facility.ResourceNode;
import mapc2017.data.facility.Shop;
import mapc2017.data.facility.Storage;
import mapc2017.data.facility.Workshop;

public class FacilityInfo {
	
	public static final String CHARGING_STATION 	= "chargingStation";
	public static final String DUMP 				= "dump";
	public static final String SHOP 				= "shop";
	public static final String STORAGE 				= "storage";
	public static final String WORKSHOP 			= "workshop";
	public static final String RESOURCE_NODE		= "resourceNode";	
	
	private static FacilityInfo instance;	
	public  static FacilityInfo get() { return instance; }
	
	private Map<String, ChargingStation> chargingStations 	= new HashMap<>();
	private Map<String, Dump> 			 dumps 			 	= new HashMap<>();
	private Map<String, Shop> 			 shops 				= new HashMap<>();
	private Map<String, Storage> 		 storages 			= new HashMap<>();
	private Map<String, Workshop> 		 workshops 			= new HashMap<>();
	private Map<String, ResourceNode>	 resourceNodes		= new HashMap<>();
	
	public FacilityInfo() {	
		instance = this; 
	}
	
	/////////////
	// GETTERS //
	/////////////

	public Facility getFacility(String name) {		
			 if (name.startsWith(CHARGING_STATION)) return chargingStations.get(name);
		else if (name.startsWith(DUMP            )) return dumps           .get(name);
		else if (name.startsWith(SHOP            )) return shops           .get(name);
		else if (name.startsWith(STORAGE         )) return storages        .get(name);
		else if (name.startsWith(WORKSHOP        )) return workshops       .get(name);
		else if (name.startsWith(RESOURCE_NODE   )) return resourceNodes   .get(name);
		else throw new UnsupportedOperationException("Unknown facility: " + name);
	}
	
	public synchronized Collection<? extends Facility> getFacilities(String type) {
			 if (type.equals(CHARGING_STATION)) return chargingStations.values();	
		else if (type.equals(DUMP			 ))	return dumps	       .values();        
		else if (type.equals(SHOP			 ))	return shops	       .values();           
		else if (type.equals(STORAGE		 )) return storages	       .values();
		else if (type.equals(WORKSHOP		 ))	return workshops       .values();
		else if (type.equals(RESOURCE_NODE	 ))	return resourceNodes   .values();
		else throw new UnsupportedOperationException("Unknown type: " + type);
	}
	
	public synchronized Collection<Shop> getShops() {
		return shops.values();
	}
	
	public Collection<Storage> getUnretrievedStorages() {
		return storages.values().stream()
				.filter(s -> !s.isRetrieving())
				.filter(s -> !s.getDelivered().isEmpty())
				.collect(Collectors.toSet());
	}
	
	public Collection<ChargingStation> getChargingStations() {
		return chargingStations.values();
	}
	
	public Collection<ChargingStation> getActiveChargingStations() {
		return getChargingStations().stream()
				.filter(ChargingStation::isActive)
				.collect(Collectors.toSet());
	}
	
	/////////////
	// SETTERS //
	/////////////

	public synchronized void putFacility(Facility f) {
			 if (f instanceof ChargingStation) chargingStations.put(f.getName(), (ChargingStation) f);
		else if (f instanceof Dump           ) dumps           .put(f.getName(), (Dump           ) f);
		else if (f instanceof Shop           ) shops           .put(f.getName(), (Shop           ) f);
		else if (f instanceof Storage        ) storages        .put(f.getName(), (Storage        ) f);
		else if (f instanceof Workshop       ) workshops       .put(f.getName(), (Workshop       ) f);
		else if (f instanceof ResourceNode   ) resourceNodes   .put(f.getName(), (ResourceNode   ) f);
		else throw new UnsupportedOperationException("Unsupported facility: " + f.getName());
	}
	
	public synchronized void setShop(Shop f) {
		shops.get(f.getName()).update(f);
	}
	
	public synchronized void setStorage(Storage f) {
		storages.get(f.getName()).update(f);
	}
	
	public void clearFacilities() {
		chargingStations.clear();
		dumps 			.clear();
		shops 			.clear();
		storages 		.clear();
		workshops 		.clear();
		resourceNodes	.clear();
	}
}
