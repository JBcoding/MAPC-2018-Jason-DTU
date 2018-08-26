package info;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import cnp.TaskArtifact;
import data.CEntity;
import eis.iilang.Percept;
import env.Translator;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Route;
import massim.scenario.city.data.facilities.ChargingStation;
import massim.scenario.city.data.facilities.Dump;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.data.facilities.ResourceNode;
import massim.scenario.city.data.facilities.Shop;
import massim.scenario.city.data.facilities.Storage;
import massim.scenario.city.data.facilities.Workshop;

public class FacilityArtifact extends Artifact {
	
	private static final Logger logger = Logger.getLogger(FacilityArtifact.class.getName());	

	public static final String CHARGING_STATION 	= "chargingStation";
	public static final String DUMP 				= "dump";
	public static final String SHOP 				= "shop";
	public static final String STORAGE 				= "storage";
	public static final String WORKSHOP 			= "workshop";
	public static final String RESOURCE_NODE		= "resourceNode";
	
	public static final Set<String>	STATIC_PERCEPTS = Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(CHARGING_STATION, DUMP, SHOP, STORAGE, WORKSHOP)));

	public static final Set<String>	DYNAMIC_PERCEPTS = Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(RESOURCE_NODE)));
	
	private static Map<String, ChargingStation> chargingStations 	= new HashMap<>();
	private static Map<String, Dump> 			dumps 			 	= new HashMap<>();
	private static Map<String, Shop> 			shops 				= new HashMap<>();
	private static Map<String, Storage> 		storages 			= new HashMap<>();
	private static Map<String, Workshop> 		workshops 			= new HashMap<>();
	private static Map<String, ResourceNode>	resourceNodes		= new HashMap<>();
	
	private static List<Map<String, ? extends Facility>> allFacilities = new ArrayList<>(
			Arrays.asList(chargingStations, dumps, shops, storages, workshops, resourceNodes));
	
	void init()
	{
		logger.setLevel(Level.WARNING);
	}
	
	@OPERATION
	void getClosestFacility(String facilityType, OpFeedbackParam<String> ret)
	{		
		Location agentLoc = AgentArtifact.getEntity(getOpUserName()).getLocation();
		
		Collection<? extends Facility> facilities = Collections.emptySet();

		switch (facilityType)
		{
		case CHARGING_STATION: 	facilities = chargingStations	.values();	break;	
		case DUMP:				facilities = dumps				.values();  break;        
		case SHOP:				facilities = shops				.values();  break;           
		case STORAGE:			facilities = storages			.values();  break;
		case WORKSHOP:			facilities = workshops			.values();  break;
		case RESOURCE_NODE:		facilities = resourceNodes		.values();  break;
		}
		
		// What happens if the feedback parameter is null?
		if (facilities.isEmpty())
		{
			ret.set("none");
		}
		else
		{
			ret.set(getClosestFacility(agentLoc, facilities));
		}
	}
	
	@OPERATION
	void getClosestShop(Object[] shopNames, OpFeedbackParam<String> ret)
	{
		Location agentLoc = AgentArtifact.getEntity(getOpUserName()).getLocation();
		
		Collection<? extends Facility> facilities = Arrays.stream(shopNames)
														.map(String.class::cast)
														.map(FacilityArtifact::getShop)
														.collect(Collectors.toSet());
		
		ret.set(getClosestFacility(agentLoc, facilities));
	}
	
	@OPERATION
	void getClosestWorkshopToStorage(String storage, OpFeedbackParam<String> workshop)
	{
		Location storageLoc = ((Storage) getFacility("storage", storage)).getLocation();
		
		workshop.set(getClosestFacility(storageLoc, workshops.values()));
	}
	
	@OPERATION
	void distanceToFacility(String facilityName, OpFeedbackParam<Integer> distance)
	{
		Route route = StaticInfoArtifact.getRoute(getOpUserName(), getFacility(facilityName).getLocation());
		if (route != null)	distance.set(route.getRouteLength());
		else 				distance.set(0);
	}
	
	@OPERATION
	void durationToFacility(String facilityName, OpFeedbackParam<Integer> duration)
	{
		CEntity agent = AgentArtifact.getEntity(getOpUserName());
		
		duration.set(StaticInfoArtifact.getRoute(getOpUserName(), getFacility(facilityName).getLocation()).getRouteDuration(agent.getRole().getSpeed()));
	}
	
	@OPERATION 
	void distanceToClosestFacility(String facilityType, OpFeedbackParam<Integer> distance)
	{
		OpFeedbackParam<String> facility = new OpFeedbackParam<>();
		
		getClosestFacility(facilityType, facility);
		
		distanceToFacility(facility.get(), distance);
	}
	
	/**
	 * @param l location to search from
	 * @param facilities to search
	 * @return The name of the nearest facility
	 */
	public static String getClosestFacility(Location l, Collection<? extends Facility> facilities)
	{		
		return facilities.stream().min(Comparator
				.comparingDouble(f -> euclideanDistance(f.getLocation(), l))).get().getName();
	}
	
	private static double euclideanDistance(Location l1, Location l2)
	{
		double dLon = l1.getLon() - l2.getLon();
		double dLat = l1.getLat() - l2.getLat();
		
		return Math.sqrt(dLon * dLon + dLat * dLat);
	}
	
	public static void perceiveUpdate(Collection<Percept> percepts)
	{		
		for (Percept percept : percepts)
		{
			switch (percept.getName())
			{
			case CHARGING_STATION: 	perceiveChargingStation	(percept);	break;	
			case DUMP:				perceiveDump			(percept);  break;             
			case SHOP:				perceiveShop			(percept);  break;             
			case STORAGE:			perceiveStorage			(percept);  break;          
			case WORKSHOP:			perceiveWorkshop		(percept);  break;
			case RESOURCE_NODE:		perceiveResourceNode	(percept);	break;
			}
		}

		logger.info("Perceived facilities");
		logFacilities("Charging station perceived:"	, chargingStations	.values());
		logFacilities("Dumps perceived:"			, dumps				.values());
		logFacilities("Shops perceived:"			, shops				.values());
		logFacilities("Storages perceived:"			, storages			.values());
		logFacilities("Workshops perceived:"		, workshops			.values());
		logFacilities("Resource nodes perceived:"	, resourceNodes		.values());
	}
	
	private static void logFacilities(String msg, Collection<? extends Facility> facilities)
	{
		logger.info(msg);
		for (Facility facility : facilities)
			logger.info(facility.toString());
	}
	
	public static void logShops() 
	{
		logFacilities("Shops perceived:", shops.values());
	}
	
	public static void logShop(String id)
	{
		logger.warning(shops.get(id).toString());
	}
	
	// Literal(String, double, double, int)
	private static void perceiveChargingStation(Percept percept) 
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	name 	= (String) args[0];
		double 	lat 	= (double) args[1];
		double 	lon 	= (double) args[2];
		int 	rate 	= (int)    args[3];
		
		chargingStations.put(name, new ChargingStation(name, new Location(lon, lat), rate));
	}

	// Literal(String, double, double)
	private static void perceiveDump(Percept percept) 
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	name 	= (String) args[0];
		double 	lat 	= (double) args[1];
		double 	lon 	= (double) args[2];
		
		dumps.put(name, new Dump(name, new Location(lon, lat)));
	}

	// Literal(String, double, double, int, List<Literal(String, int, int)>)
	private static void perceiveShop(Percept percept) 
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	name	= (String) args[0];
		double 	lat		= (double) args[1];
		double 	lon		= (double) args[2];
		int    	restock	= (int)    args[3];
		
		Shop shop = new Shop(name, new Location(lon, lat), restock);

		for (Object item : (Object[]) args[4]) 
		{
			Object[] itemArgs = (Object[]) item;
			
			String 	itemId 		= (String) itemArgs[0];
			int 	price 	  	= (int)    itemArgs[1];
			int 	quantity  	= (int)    itemArgs[2];
			
			shop.addItem(ItemArtifact.getItem(itemId), quantity, price);	
			ItemArtifact.addItemLocation(itemId, shop);
		}
		shops.put(name, shop);
	}

	// Literal(String, double, double, int)
	private static void perceiveStorage(Percept percept) 
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	name  		= (String) args[0];
		double 	lat			= (double) args[1];
		double 	lon			= (double) args[2];
		int 	capacity	= (int)    args[3];
		// Set<String> teamNames?
		
		storages.put(name, 
				new Storage(name, new Location(lon, lat), capacity, Collections.emptySet()));
	}

	// Literal(String, double, double)
	private static void perceiveWorkshop(Percept percept) 
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	name 	= (String) args[0];
		double 	lat 	= (double) args[1];
		double 	lon 	= (double) args[2];
		
		workshops.put(name, new Workshop(name, new Location(lon, lat)));
	}
	
	// Literal(String, double, double, String)
	private static void perceiveResourceNode(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	name   	= (String) args[0];
		double 	lat		= (double) args[1];
		double 	lon		= (double) args[2];
		String 	itemId 	= (String) args[3];
		
		resourceNodes.put(name, 
				new ResourceNode(name, new Location(lon, lat), ItemArtifact.getItem(itemId), 0));
	}
	
	public static Facility getFacility(String facilityName)
	{
		if (facilityName == "none") return null;
		
		return allFacilities.stream().filter(facilities -> facilities.containsKey(facilityName))
				.findFirst().get().get(facilityName);
	}
	
	private static Facility getShop(String shopName)
	{
		return getFacility("shop", shopName);
	}

	protected static Facility getFacility(String facilityType, String facilityName) 
	{		
		Map<String, ? extends Facility> facilities = new HashMap<>();
		
		switch (facilityType)
		{
		case CHARGING_STATION: 	facilities = chargingStations	;  break;	
		case DUMP:				facilities = dumps				;  break;        
		case SHOP:				facilities = shops				;  break;           
		case STORAGE:			facilities = storages			;  break;
		case WORKSHOP:			facilities = workshops			;  break;
		case RESOURCE_NODE:		facilities = resourceNodes		;  break;
		}
		
		return facilities.get(facilityName);
	}
	
	public static Collection<Facility> getFacilities(String facilityType)
	{
		switch (facilityType)
		{
		case CHARGING_STATION: 	return chargingStations	.values().stream().map(x -> (Facility) x).collect(Collectors.toList()); 
		case DUMP:				return dumps			.values().stream().map(x -> (Facility) x).collect(Collectors.toList());        
		case SHOP:				return shops			.values().stream().map(x -> (Facility) x).collect(Collectors.toList());           
		case STORAGE:			return storages			.values().stream().map(x -> (Facility) x).collect(Collectors.toList()); 
		case WORKSHOP:			return workshops		.values().stream().map(x -> (Facility) x).collect(Collectors.toList()); 
		case RESOURCE_NODE:		return resourceNodes	.values().stream().map(x -> (Facility) x).collect(Collectors.toList()); 
		}
		return null;
	}

	public static void announceShops() {
		TaskArtifact.announceShops(shops.values());
	}

	public static void reset() 
	{
		chargingStations 	= new HashMap<>();
		dumps 			 	= new HashMap<>();
		shops 				= new HashMap<>();
		storages 			= new HashMap<>();
		workshops 			= new HashMap<>();
		resourceNodes		= new HashMap<>();
	}
}
