package info;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cartago.Artifact;
import cartago.GUARD;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import cnp.TaskArtifact;
import data.CEntity;
import eis.iilang.Percept;
import env.Translator;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.ItemBox;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Route;
import massim.scenario.city.data.facilities.*;

public class FacilityArtifact extends Artifact {
	
	private static final Logger logger = Logger.getLogger(FacilityArtifact.class.getName());	

	public static final String CHARGING_STATION 	= "chargingStation";
	public static final String DUMP 				= "dump";
	public static final String SHOP 				= "shop";
	public static final String STORAGE 				= "storage";
	public static final String WORKSHOP 			= "workshop";
	public static final String RESOURCE_NODE		= "resourceNode";
	public static final String WELL	 			    = "well";

	private static final double STOP_GATHER_RATIO = 0.85;

	public static final Set<String>	STATIC_PERCEPTS = Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(CHARGING_STATION, DUMP, SHOP, STORAGE, WORKSHOP)));

	public static final Set<String>	DYNAMIC_PERCEPTS = Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(RESOURCE_NODE, WELL)));
	
	private static Map<String, ChargingStation> chargingStations 	= new HashMap<>();
	private static Map<String, Dump> 			dumps 			 	= new HashMap<>();
	private static Map<String, Shop> 			shops 				= new HashMap<>();
	private static Map<String, Storage> 		storages 			= new HashMap<>();
	private static Map<String, Workshop> 		workshops 			= new HashMap<>();
	private static Map<String, ResourceNode>	resourceNodes		= new HashMap<>();
	static Map<String, Well>			wells				= new HashMap<>();
	static Map<String, Integer> wellsIntegrityDiff = new HashMap<>();

	public static List<Map<String, ? extends Facility>> getAllFacilities() {
		return allFacilities;
	}

	private static List<Map<String, ? extends Facility>> allFacilities = new ArrayList<>(
			Arrays.asList(chargingStations, dumps, shops, storages, resourceNodes, workshops, resourceNodes, wells));
	
	void init()
	{
		logger.setLevel(Level.WARNING);
	}

	@OPERATION
	void getClosestFacility(String facilityType, OpFeedbackParam<String> ret) {
		Location agentLoc = AgentArtifact.getEntity(getOpUserName()).getLocation();

		Collection<? extends Facility> facilities = Collections.emptySet();

		switch (facilityType) {
			case CHARGING_STATION:
				facilities = chargingStations.values();
				break;
			case DUMP:
				facilities = dumps.values();
				break;
			case SHOP:
				facilities = shops.values();
				break;
			case STORAGE:
				facilities = storages.values();
				break;
			case WORKSHOP:
				facilities = workshops.values();
				break;
			case RESOURCE_NODE:
				facilities = resourceNodes.values();
				break;
			case WELL:
				facilities = wells.values();
				break;
		}

		// What happens if the feedback parameter is null?
		if (facilities.isEmpty()) {
			ret.set("none");
		} else {
			ret.set(getClosestFacility(agentLoc, facilities));
		}
	}

    static Map<String, ResourceNode> getResourceNodes() {
	    return new HashMap<>(resourceNodes);
    }

	static Map<String, Storage> getStorages() {
		return new HashMap<>(storages);
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
	void wellHasFullIntegrity(String wellName, OpFeedbackParam<Boolean> hasFullIntegrity)
	{
		if (wells.containsKey(wellName)) {
			Well well = wells.get(wellName);
			hasFullIntegrity.set(well.getIntegrity() == well.getMaxIntegrity());
		} else {
			hasFullIntegrity.set(false);
		}
	}
	
	@OPERATION
	void distanceToFacility(String facilityName, OpFeedbackParam<Integer> distance)
	{
	    Facility fac = getFacility(facilityName);
	    if (fac == null) {
	        distance.set(Integer.MAX_VALUE);
        } else {
            Route route = StaticInfoArtifact.getRoute(getOpUserName(),fac.getLocation());
            if (route != null) {
                distance.set(route.getRouteLength());
            } else {
                distance.set(Integer.MAX_VALUE);
            }
        }
	}

	@OPERATION
	void durationToFacility(String facilityName, OpFeedbackParam<Integer> duration)
	{
		CEntity agent = AgentArtifact.getEntity(getOpUserName());

		duration.set(StaticInfoArtifact.getRoute(getOpUserName(), getFacility(facilityName).getLocation()).getRouteDuration(agent.getCurrentSpeed()));
	}
	
	@OPERATION 
	void distanceToClosestFacility(String facilityType, OpFeedbackParam<Integer> distance)
	{
		OpFeedbackParam<String> facility = new OpFeedbackParam<>();
		
		getClosestFacility(facilityType, facility);
		
		distanceToFacility(facility.get(), distance);
	}

    @OPERATION
    void getResource(String node, OpFeedbackParam<String> item) {
        item.set(resourceNodes.get(node).getResource().getName());
    }

	@OPERATION
	void getLocation(String node, OpFeedbackParam<Double> lat, OpFeedbackParam<Double> lon) {
		Location loc = resourceNodes.get(node).getLocation();
		lat.set(loc.getLat());
		lon.set(loc.getLon());
	}

	@OPERATION
	synchronized void getEnemyWell(OpFeedbackParam<String> name, OpFeedbackParam<Double> lat, OpFeedbackParam<Double> lon) {
		Location agentLoc = AgentArtifact.getEntity(getOpUserName()).getLocation();
		try {
			Well w = wells.values().stream()
					.filter(well -> !well.getTeam().equals(StaticInfoArtifact.getTeam()))
					.min(Comparator.comparingDouble(well -> euclideanDistance(well.getLocation(), agentLoc))).get();
			name.set(w.getName());
			lat.set(w.getLocation().getLat());
			lon.set(w.getLocation().getLon());
		} catch (NoSuchElementException | NullPointerException e) {
			name.set("none");
			//lat.set(loc.getLat());
			//lon.set(loc.getLon());
		}
	}

    public static void destroyWell(AgentArtifact agent) {
        try {
            Well well = wells.values().stream().filter(w -> agent.canSee(w.getLocation())).findFirst().get();
            destroyWell(well);
        } catch (NoSuchElementException | NullPointerException e) {
            // Already destroyed
            System.out.println("Already destroyed well");
        }
    }

    public static void destroyWell(Well well) {
        try {
            wells.remove(well.getName());
            allFacilities = new ArrayList<>(Arrays.asList(chargingStations, dumps, shops, storages, resourceNodes, workshops, resourceNodes, wells));
        } catch (NoSuchElementException | NullPointerException e) {
            // Already destroyed
            System.out.println("Already destroyed well");
        }
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
	
	public static double euclideanDistance(Location l1, Location l2)
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
			case WELL: 				perceiveWell			(percept);	break;
			}
		}

		logger.info("Perceived facilities");
		logFacilities("Charging station perceived:"	, chargingStations	.values());
		logFacilities("Dumps perceived:"			, dumps				.values());
		logFacilities("Shops perceived:"			, shops				.values());
		logFacilities("Storages perceived:"			, storages			.values());
		logFacilities("Workshops perceived:"		, workshops			.values());
		logFacilities("Resource nodes perceived:"	, resourceNodes		.values());
		logFacilities("Wells perceived:"	, wells		.values());
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
		
		Shop shop = new Shop(name, new Location(lon, lat), 1);
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
		int usedCap = (int) args[4];
        Object[] items = (Object[]) args[5];

        if (StaticInfoArtifact.getStorage() != null && name.equals(StaticInfoArtifact.getStorage().getMainStorageFacility().getName())) {
            StaticInfoArtifact.getStorage().clearItemsCount();
        }

        int c = 0;

        ItemBox itemBox = new ItemBox();
        for (Object tuple : items) {
            Object[] tup = (Object[])tuple;
            itemBox.store(ItemArtifact.getItem((String)tup[0]), (int) tup[1]);
            if (StaticInfoArtifact.getStorage() != null && name.equals(StaticInfoArtifact.getStorage().getMainStorageFacility().getName())) {
                StaticInfoArtifact.getStorage().updateItemCount((String)tup[0], (Integer) tup[1]);
                c ++;
            }
        }


        if (c > 1 && ((double)usedCap) / capacity > STOP_GATHER_RATIO) {
            System.out.println("STOPPING GATHERING!");
            StaticInfoArtifact.getStorage().gatherEnabled = false;
        }

        // Set<String> teamNames?

        Storage s = new Storage(name, new Location(lon, lat), capacity, Collections.emptySet());
        s.addDelivered(itemBox, StaticInfoArtifact.getTeam());

		storages.put(name, s);

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

	private static void perceiveResourceNode(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);

		String name = (String) args[0];
		double lat = (double) args[1];
		double lon = (double) args[2];
		String resource = (String) args[3];

		resourceNodes.put(name, 
				new ResourceNode(name, new Location(lon, lat), ItemArtifact.getItem(resource), 0));
	}

	private static void perceiveWell(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);

		String name = (String) args[0];
		double lat = (double) args[1];
		double lon = (double) args[2];
		String type = (String) args[3];
		String team = (String) args[4];
		int integrity = (int) args[5];

        wellsIntegrityDiff.put(name, 0);

		if (wells.containsKey(name)) {
			Well well = wells.get(name);
			well.build(integrity - well.getIntegrity());
		} else {
			Well well = new Well(name, team, new Location(lon, lat),
					StaticInfoArtifact.getWellTypes().stream().filter(w -> w.getName().equals(type)).findFirst().get());
			wells.put(name, well);
			allFacilities = new ArrayList<>(
					Arrays.asList(chargingStations, dumps, shops, storages, resourceNodes, workshops, resourceNodes, wells));
//		allFacilities.get(allFacilities.size()-1).put(name, well);
		}
	}

	public static Facility getFacility(String facilityName)
	{
		if (facilityName.equals("none")) return null;

		try {
			return allFacilities.stream().filter(facilities -> facilities.containsKey(facilityName))
					.findFirst().get().get(facilityName);
		} catch (NoSuchElementException e) {
			return null;
		}
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
		case WELL:				facilities = wells				;  break;
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
		case WELL:				return wells			.values().stream().map(x -> (Facility) x).collect(Collectors.toList());
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
		wells				= new HashMap<>();
	}

	public static Set<Item> calculateMissingResourceNodes() {
	    Set<Item> baseItems = ItemArtifact.getLevel0Items();
	    Set<Item> itemsWeCanLocate = resourceNodes.values().stream().map(ResourceNode::getResource).collect(Collectors.toSet());
        Set<Item> itemsWeStillNeedToFindAResourceNodeFor = new HashSet<>(baseItems);
	    itemsWeStillNeedToFindAResourceNodeFor.removeAll(itemsWeCanLocate);

	    return itemsWeStillNeedToFindAResourceNodeFor;

    }
}
