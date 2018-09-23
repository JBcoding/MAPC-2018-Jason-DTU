package info;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cartago.Artifact;
import cartago.GUARD;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import data.CBuildTeam;
import data.CCityMap;
import data.CEntity;
import data.CStorage;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import eis.iilang.PrologVisitor;
import env.EIArtifact;
import env.Translator;
import jason.util.Pair;
import massim.protocol.messagecontent.Action;
import massim.scenario.city.data.Entity;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Route;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.data.facilities.ResourceNode;
import massim.scenario.city.data.facilities.Storage;
import massim.scenario.city.data.facilities.Well;
import massim.util.RNG;

import javax.xml.bind.SchemaOutputResolver;

public class AgentArtifact extends Artifact {
	
	private static final Logger logger = Logger.getLogger(AgentArtifact.class.getName());
	
	private static final String ACTION_ID			= "actionID";
	private static final String CHARGE 				= "charge";
	private static final String FACILITY			= "facility";
	private static final String HAS_ITEM			= "hasItem";
	private static final String LAST_ACTION 		= "lastAction";
	private static final String LAST_ACTION_PARAMS 	= "lastActionParams";
	private static final String LAST_ACTION_RESULT 	= "lastActionResult";
	private static final String LAT 				= "lat";
	private static final String LON 				= "lon";
	private static final String LOAD				= "load";
	private static final String ROUTE 				= "route";
	private static final String ROUTE_LENGTH 		= "routeLength";
    private static final String CURRENT_BATTERY     = "maxBattery";
    private static final String CURRENT_CAPACITY 	= "maxLoad";
	private static final String SPEED 				= "speed";
	private static final String VISION 				= "vision";
	private static final String SKILL 				= "skill";

	public static final Set<String>	PERCEPTS = Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(ACTION_ID, CHARGE, FACILITY, HAS_ITEM, LAST_ACTION, LAST_ACTION_PARAMS,
				LAST_ACTION_RESULT, LAT, LON, LOAD, ROUTE, ROUTE_LENGTH, CURRENT_BATTERY, CURRENT_CAPACITY, SPEED, VISION, SKILL)));
	
	private static Map<String, CEntity> entities = new HashMap<>();
	private static Map<String, AgentArtifact> artifacts = new HashMap<>();
	
	public String agentName;

	public static final double EPSILON = 1E-3;

	private static Deque<String> scouts = new ConcurrentLinkedDeque<String>();
	private static Set<String> wellBuilders = ConcurrentHashMap.newKeySet();
	private static Deque<String> destroyers = new ConcurrentLinkedDeque<String>();

	private static final int MAX_DESTROYERS = 3;

	// TODO: replace with synchronized keyword?
	private static Semaphore buildSemaphore = new Semaphore(1);
	private static Semaphore destroySemaphore = new Semaphore(1);

	private int updateRound = -1;

	void init() {
	    // This will be agent + some id, this comes from the Jason config.
		this.agentName = this.getId().getName();

		artifacts.put(this.agentName, this);

		defineObsProperty("inFacility", 		"none");
		defineObsProperty("speed", 	0);
		defineObsProperty("vision", 	0);
		defineObsProperty("skill", 	0);
		defineObsProperty("charge", 			250);
		defineObsProperty("load", 				0);                
		defineObsProperty("routeLength", 		0);                
		defineObsProperty("lastAction", 		"noAction"); 
		defineObsProperty("lastActionResult", 	"successful");
		defineObsProperty("lastActionParam", 	"[]");
		defineObsProperty("atPeriphery", 	false);

		defineObsProperty("scout", false);
        defineObsProperty("gather", false);
		defineObsProperty("builder", false);
		defineObsProperty("deliver", false);

		defineObsProperty("build", false);
		defineObsProperty("destroy", false);

        defineObsProperty("currentBattery", 	250);
        defineObsProperty("currentCapacity", 	0);
		defineObsProperty("inOwnWell", 		false);
    }

	/**
	 * Get the artifact related to the agent
	 * @param agentName Name of the agent
	 * @return 
	 */
	public static AgentArtifact getAgentArtifact(String agentName) 
	{
		return artifacts.get(agentName);
	}
	
	/**
	 * @return All the entities
	 */
	public static Map<String, CEntity> getEntities() {
		return entities;
	}

	/**
	 * @return The names of all the entities
	 */
	public static Set<String> getEntitiesNames() {
		return entities.keySet();
	}
	
	protected static void addEntity(String name, CEntity entity) {
		entities.put(name, entity);

		Artifact artifact = artifacts.get(name);
		if (artifact == null) {
		    throw new IllegalStateException(
		            "Artifact not found under: " + name + "\n" +
                    "Artifacts: " + artifacts.keySet()
            );
        }
		entity.addAgentArtifact(artifacts.get(name));
	}
	
	public static CEntity getEntity(String name)
	{
		return entities.get(name);
	}
	
	/**
	 * @return The entity associated with this agent artifact
	 */
	public CEntity getEntity() {
		return entities.get(this.agentName);
	}

	@OPERATION
	void getPos(OpFeedbackParam<Double> lon, OpFeedbackParam<Double> lat)
	{
		Location l = entities.get(getOpUserName()).getLocation();
		
		lon.set(l.getLon());
		lat.set(l.getLat());
	}

	@OPERATION
	void closestPeriphery(OpFeedbackParam<Double> lat, OpFeedbackParam<Double> lon)
	{
		Location l = this.getEntity().getLocation();
		l = StaticInfoArtifact.getMap().getClosestPeriphery(l, EPSILON);

		lat.set(l.getLat());
		lon.set(l.getLon());
	}

	@OPERATION
	void getRandomPeripheralLocation(OpFeedbackParam<Double> Lat, OpFeedbackParam<Double> Lon) {
		CCityMap cityMap = StaticInfoArtifact.getMap();
		double lat, lon;

		if (RNG.nextInt() % 2 == 0) {
			lat = RNG.nextInt() % 2 == 0 ? cityMap.getMinLat() : cityMap.getMaxLat();
			lon = cityMap.getMinLon() + (cityMap.getMaxLon() - cityMap.getMinLon()) * RNG.nextDouble();
		} else {
			lat = cityMap.getMinLat() + (cityMap.getMaxLat() - cityMap.getMinLat()) * RNG.nextDouble();
			lon = RNG.nextInt() % 2 == 0 ? cityMap.getMinLon() : cityMap.getMaxLon();
		}

        //lat = lat * 0.9 + cityMap.getCenter().getLat() * 0.1;
        //lon = lon * 0.9 + cityMap.getCenter().getLon() * 0.1;

		//Location l = new Location(lon, lat);
		Location l = StaticInfoArtifact.getMap().getClosestPeriphery(new Location(lon, lat), EPSILON);
		Lat.set(l.getLat());
		Lon.set(l.getLon());
	}

	@OPERATION
	void getAgentInventory(OpFeedbackParam<Object> ret)
	{
		ret.set(getAgentInventory(getOpUserName()));
	}
	
	public static Map<String, Integer> getAgentInventory(String agent) 
	{
		return getEntity(agent).getInventory().toItemAmountData().stream()
				.collect(Collectors.toMap(e -> e.getName(), e -> e.getAmount()));
	}
	
	public void perceiveInitial(Collection<Percept> percepts)
	{
		defineObsProperty("myRole", this.getEntity().getRole().getName());
		
		perceiveUpdate(percepts);
	}

	public void perceiveUpdate(Collection<Percept> percepts)
	{
		execInternalOp("update", percepts);
	}
	
	@OPERATION
	private void update(Collection<Percept> percepts) {
		int load = this.getEntity().getCurrentLoad();

		this.getEntity().clearInventory();

		boolean positionChange = true;
		for (Percept percept : percepts) {
		    if (percept.getName().equals(LAT) || percept.getName().equals(LON)) {
		        positionChange = true;
            }

			switch (percept.getName()) {
//			case ACTION_ID: perceiveActionID(percept); break;
			case CHARGE: 				perceiveCharge(percept); break;
			case FACILITY:				perceiveFacility(percept); break;
			case HAS_ITEM:				perceiveHasItem(percept); break;
			case LAST_ACTION : 			perceiveLastAction(percept); break;
			case LAST_ACTION_PARAMS: 	perceiveLastActionParams(percept); break;
			case LAST_ACTION_RESULT: 	perceiveLastActionResult(percept); break;
			case LAT: 					perceiveLat(percept); break;
			case LON: 					perceiveLon(percept); break;
			case ROUTE: 				perceiveRoute(percept); break;
            case ROUTE_LENGTH: 			perceiveRouteLength(percept); break;
            case CURRENT_BATTERY:       perceiveCurrentBattery(percept); break;
            case CURRENT_CAPACITY: perceiveCurrentCapacity(percept); break;
			case SPEED: perceiveSpeed(percept); break;
			case VISION: perceiveVision(percept); break;
			case SKILL: perceiveSkill(percept); break;
			}
		}

		if (positionChange) {
		    StaticInfoArtifact.getExploredMap().updateExplored(this.getEntity().getLocation(), this.getEntity().getRole().getBaseVision(), this.agentName);
        }

        // If agent is where a well should be, but it no longer is.
		if (FacilityArtifact.getFacilities("well").stream().filter(w -> canSee(w.getLocation())).count() > 0 &&
				percepts.stream().filter(p ->
					p.getName().equals(FacilityArtifact.WELL) && canSee(new Location((double)Translator.perceptToObject(p)[2], (double)Translator.perceptToObject(p)[1]))
				).count() == 0) {
			markWellDestroyed();
		}

		if (load != this.getEntity().getCurrentLoad())
		{
			getObsProperty("load").updateValue(this.getEntity().getCurrentLoad());
		}

		getObsProperty("lastActionParam").updateValue(this.getEntity().getLastActionParam());

		updateAtPeriphery();

		updateRound = StaticInfoArtifact.getCurrentStep();

		if (EIArtifact.LOGGING_ENABLED)
		{
			logger.info(agentName + " perceived");
		}
	}

	private void updateAtPeriphery() {
		int vision = getEntity().getCurrentVision();
		CCityMap m = StaticInfoArtifact.getMap();
		Location l = getEntity().getLocation();
		getObsProperty("atPeriphery").updateValue(
			m.isVisible(l, new Location(m.getMinLon(), l.getLat()), vision * 4) ||
			m.isVisible(l, new Location(m.getMaxLon(), l.getLat()), vision * 4) ||
			m.isVisible(l, new Location(l.getLon(), m.getMinLat()), vision * 4) ||
			m.isVisible(l, new Location(l.getLon(), m.getMaxLat()), vision * 4)
		);
	}

	private boolean doubleEquals(double a, double b, double epsilon) {
		return Math.abs(a - b) < epsilon;
	}

	private void perceiveLastActionParams(Percept percept) 
	{
		Object[] args = Translator.perceptToObject(percept);

		this.getEntity().setLastActionParam((Object[]) args[0]);
	}

	/**
	 * Literal(int)
	 * @param percept
	 */
	@OPERATION
	private void perceiveCharge(Percept percept)
	{
		int charge = (int) Translator.perceptToObject(percept)[0];

		if (charge != this.getEntity().getCurrentCharge())
		{
			this.getEntity().setCurrentCharge(charge);
			getObsProperty("charge").updateValue(this.getEntity().getCurrentCharge());
		}
	}

	/**
	 * Literal(int)
	 * @param percept
	 */
	@OPERATION
	private void perceiveSpeed(Percept percept)
	{
		int speed = (int) Translator.perceptToObject(percept)[0];

		if (speed != this.getEntity().getCurrentSpeed())
		{
			this.getEntity().setCurrentSpeed(speed);
			getObsProperty("speed").updateValue(this.getEntity().getCurrentSpeed());
		}
	}

	/**
	 * Literal(int)
	 * @param percept
	 */
	@OPERATION
	private void perceiveVision(Percept percept)
	{
		int vision = (int) Translator.perceptToObject(percept)[0];

		if (vision != this.getEntity().getCurrentVision())
		{
			this.getEntity().setCurrentVision(vision);
			getObsProperty("vision").updateValue(this.getEntity().getCurrentVision());
		}
	}

	/**
	 * Literal(int)
	 * @param percept
	 */
	@OPERATION
	private void perceiveSkill(Percept percept)
	{
		int skill = (int) Translator.perceptToObject(percept)[0];

		if (skill != this.getEntity().getCurrentSkill())
		{
			this.getEntity().setCurrentSkill(skill);
			getObsProperty("skill").updateValue(this.getEntity().getCurrentSkill());
		}
	}

	/**
	 * Literal(int)
	 * Battery is the amount of charge when the agent is fully charged
	 * @param percept
	 */
	@OPERATION
	private void perceiveCurrentBattery(Percept percept)
	{
		int battery = (int) Translator.perceptToObject(percept)[0];

		if (battery != this.getEntity().getCurrentBattery())
		{
			this.getEntity().setCurrentBattery(battery);
			getObsProperty("currentBattery").updateValue(this.getEntity().getCurrentBattery());
		}
	}

    /**
     * Literal(int)
     * @param percept
     */
    @OPERATION
    private void perceiveCurrentCapacity(Percept percept)
    {
        int capacity = (int) Translator.perceptToObject(percept)[0];

        if (capacity != this.getEntity().getCurrentCapacity())
        {
            this.getEntity().setCurrentCapacity(capacity);
            getObsProperty("currentCapacity").updateValue(this.getEntity().getCurrentCapacity());
        }
    }

	@OPERATION
	public void perceiveFacility(Percept percept) {
		Parameter param = percept.getParameters().get(0);

		CEntity entity = this.getEntity();
		if (entity == null) {
		    return;
        }

		if (!PrologVisitor.staticVisit(param).equals("")) {
			Object[] args = Translator.perceptToObject(percept);
			
			Facility facility = FacilityArtifact.getFacility((String) args[0]);

			if (!entity.getFacilityName().equals(facility.getName())) {
				this.getEntity().setFacility(facility);
				getObsProperty("inFacility").updateValue(entity.getFacilityName());

				if (facility instanceof Well) {
					getObsProperty("inOwnWell").updateValue(((Well)facility).getTeam().equals(StaticInfoArtifact.getTeam()));
				}
			}
		}
		else 
		{
			if (!entity.getFacilityName().equals("none"))
			{
				entity.setFacility(null);
				getObsProperty("inFacility").updateValue(entity.getFacilityName());
				getObsProperty("inOwnWell").updateValue(false);
			}
		}
		
	}

	private void perceiveHasItem(Percept percept) 
	{
		Object[] args = Translator.perceptToObject(percept);

		Item 	item 	= ItemArtifact.getItem((String) args[0]);
		int 	amount 	= (int) args[1];
		
		this.getEntity().addItem(item, amount);
	}
	
	/**
	 * Literal(String)
	 * @param percept
	 */
	@OPERATION
	private void perceiveLastAction(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		Action action = new Action((String) args[0]);
		
		if (!this.getEntity().getLastAction().getActionType().equals(action.getActionType()))
		{
			this.getEntity().setLastAction(action);
			getObsProperty("lastAction").updateValue(this.getEntity().getLastAction().getActionType());			
		}
		
	}
	
	/**
	 * Literal(String)
	 * @param percept
	 */
	@OPERATION
	private void perceiveLastActionResult(Percept percept)
	{
		String result = (String) Translator.perceptToObject(percept)[0];
		
		if (!result.equals(this.getEntity().getLastActionResult()))
		{
			this.getEntity().setLastActionResult(result);
			getObsProperty("lastActionResult").updateValue(this.getEntity().getLastActionResult());
		}
	}
	
	/**
	 * Literal(int)
	 * @param percept
	 */
	private void perceiveLat(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		this.getEntity().setLat((double) args[0]);
	}
	
	/**
	 * Literal(int)
	 * @param percept
	 */
	private void perceiveLon(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		this.getEntity().setLon((double) args[0]);	
	}
	
	/**
	 * Literal([wp(int, lat, lon)])
	 * @param percept
	 */
	public void perceiveRoute(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		Route route = new Route();

		for (Object term : args)
		{
			for (Object arg : (Object[]) term)
			{
				Object[] values = (Object[]) arg;
	
				double lat = (double) values[1];
				double lon = (double) values[2];
				route.addPoint(new Location(lon, lat));
			}
		}

		this.getEntity().setRoute(route);
	}

	/**
	 * Literal(int)
	 * @param percept
	 */
	@OPERATION
	private void perceiveRouteLength(Percept percept)
	{
		int length = (int) Translator.perceptToObject(percept)[0];

		if (this.getEntity().getRouteLength() != length)
		{
			this.getEntity().setRouteLength(length);
			getObsProperty("routeLength").updateValue(this.getEntity().getRouteLength());
		}
	}

	@OPERATION
	void getClosestUnexploredPosition(OpFeedbackParam<Double> lat, OpFeedbackParam<Double> lon) {
		Location l = StaticInfoArtifact.getExploredMap().getClosestUnxploredLocation(getEntity().getLocation());
		lat.set(l.getLat());
		lon.set(l.getLon());

		if (agentName.equals("agent1")) {
			System.out.println("Missing " + FacilityArtifact.calculateMissingResourceNodes().size() + " resource nodes");
		}

	    if (FacilityArtifact.calculateMissingResourceNodes().size() == 0) {
            stopScouting();
        }
    }

    public void setToScout() {
        getObsProperty("scout").updateValue(true);
        scouts.add(this.agentName);
    }

    @OPERATION
    void getResourceNode(OpFeedbackParam<Facility> f, OpFeedbackParam<Boolean> success) {
	    Facility facility = StaticInfoArtifact.getStorage().getLowestResourceNode(this);
	    if (facility == null) {
	        success.set(false);
	        getObsProperty("gather").updateValue(false);
	        setToDestroy();
	        f.set(FacilityArtifact.getResourceNodes().values().iterator().next());
        } else {
	        success.set(true);
	        f.set(facility);
        }
	}

    @OPERATION
    void getFacilityName(Object facility, OpFeedbackParam<String> name) {
	    if (facility != null) {
            name.set(((ResourceNode)facility).getName());
        } else {
	        name.set("null");
        }
    }

	@OPERATION
	void getCoords(ResourceNode facility, OpFeedbackParam<Double> lat, OpFeedbackParam<Double> lon) {
		lat.set(facility.getLocation().getLat());
		lon.set(facility.getLocation().getLon());
	}

	@OPERATION
	void getWellCoordsFromName(String well, OpFeedbackParam<Double> lat, OpFeedbackParam<Double> lon) {
		Location loc = FacilityArtifact.getFacility(well).getLocation();
		lat.set(loc.getLat());
		lat.set(loc.getLat());
	}

	@OPERATION
	void getItemVolume(ResourceNode facility, OpFeedbackParam<Integer> v) {
		v.set(facility.getResource().getVolume());
	}

    @OPERATION
    void getMainStorageFacility(OpFeedbackParam<String> v) {
        v.set(StaticInfoArtifact.getStorage().getMainStorageFacility().getName());
    }

    @OPERATION
    void getItemNameAndQuantity(OpFeedbackParam<String> name, OpFeedbackParam<Integer> quantity) {
	    try {
            Item item = getEntity().getInventory().getStoredTypes().iterator().next();
            name.set(item.getName());
            quantity.set(getEntity().getInventory().getItemCount(item));
        } catch (Exception e) {
	        name.set("No go");
	        quantity.set(-1);
        }
    }

    @OPERATION
    void getWorkShop(OpFeedbackParam<String> v) {
        v.set(StaticInfoArtifact.getStorage().getMainWorkShop());
    }

    @OPERATION
    void getMainTruckName(OpFeedbackParam<String> v) {
        v.set(StaticInfoArtifact.getBuildTeam().getTruckName());
    }

    @OPERATION
    void isTruck(OpFeedbackParam<Boolean> v) {
        v.set(this.getEntity().getRole().getName().equals("truck"));
    }

    @OPERATION
    void getMyName(OpFeedbackParam<String> v) {
        v.set(this.agentName);
    }

    @OPERATION
    void somethingToBuild(OpFeedbackParam<Boolean> x) {
	    StaticInfoArtifact.getBuildTeam().iAmDone(agentName);
        x.set(StaticInfoArtifact.getBuildTeam().thingToBuild(agentName) != null);
    }

    @OPERATION
    void getItemToBuild(OpFeedbackParam<String> retItem, OpFeedbackParam<Integer> retQuantity) {
		String itemName = StaticInfoArtifact.getBuildTeam().thingToBuild(this.agentName);

        Item item = ItemArtifact.getItem(itemName);
        boolean level1Item = isLevel1Item(item);

        int volume = level1Item
                ? ItemArtifact.getVolume(item.getRequiredBaseItems())
                : ItemArtifact.getVolume(item.getRequiredItems());

		int quantity = getEntity().getCurrentCapacity() / volume;
        CStorage storage = StaticInfoArtifact.getStorage();

        if (level1Item) {
            for (Map.Entry<Item, Integer> entry : item.getRequiredBaseItems().entrySet()) {
                storage.reserve(entry.getKey().getName(), entry.getValue() * quantity);
            }
        } else {
            for (Item part : item.getRequiredItems()) {
                quantity = Math.min(quantity, storage.getAmount(part.getName()));
            }

            quantity = Math.max(quantity, 1);

            for (Item part : item.getRequiredItems()) {
                storage.reserve(part.getName(), quantity);
            }
        }


        retItem.set(itemName);
		retQuantity.set(quantity);
    }

    @OPERATION
    void itemInStorageIncludingReserved(String itemName, int amount, OpFeedbackParam<Boolean> contained) {
        CStorage storage = StaticInfoArtifact.getStorage();
        contained.set(storage.getActualAmount(itemName) >= amount);
    }

    private boolean isLevel1Item(Item item) {
	    return item.needsAssembly() && item.getRequiredItems().stream().noneMatch(Item::needsAssembly);
    }

    @OPERATION
    void haveItem(String item, int quantity, OpFeedbackParam<Boolean> x) {
        x.set(getEntity().getInventory().getItemCount(ItemArtifact.getItem(item)) >= quantity);
    }

    private boolean doingAction = false;
    @OPERATION
    void doActionStart() {
        doingAction = true;
    }
    @OPERATION
    void doActionEnd() {
        doingAction = false;
    }
    @OPERATION
    void doingAction(OpFeedbackParam<Boolean> x) {
        x.set(doingAction);
    }


    @OPERATION
    void getMissingItemToBuildItem(String item, OpFeedbackParam<String> itemToRetrive, OpFeedbackParam<Integer> quantity) {
        Item mainItem = ItemArtifact.getItem(item);
        Set<Item> parts = mainItem.getRequiredItems();
        for (Item i : parts) {
            if (this.getEntity().getInventory().getItemCount(i) == 0) {
                itemToRetrive.set(i.getName());
                quantity.set(1);
                return;
            }
        }
        itemToRetrive.set("Nothings missing");
        quantity.set(-1);
    }

    @OPERATION
    void doesWellExist(String F, OpFeedbackParam<Boolean> X) {
        X.set(FacilityArtifact.wells.containsKey(F));
    }


    @OPERATION
    void subtractFromWell() {
        try {
            Well w = FacilityArtifact.wells.values().stream()
                    .filter(well -> !well.getTeam().equals(StaticInfoArtifact.getTeam()))
                    .min(Comparator.comparingDouble(well -> FacilityArtifact.euclideanDistance(well.getLocation(), getEntity().getLocation()))).get();

            if (!canSee(w.getLocation())) {
                return;
            }

            FacilityArtifact.wellsIntegrityDiff.put(w.getName(),
                    FacilityArtifact.wellsIntegrityDiff.get(w.getName()) - getEntity().getCurrentSkill());



            if (w.getIntegrity() + FacilityArtifact.wellsIntegrityDiff.get(w.getName()) < 0) {
                try {
                    Thread.sleep(100);
                    // Out of time to make a better fix
                    // But trust me it is gonna work, probably
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                FacilityArtifact.destroyWell(w);
            }
        } catch (Throwable t) {}
    }

    @OPERATION
    void requestHelp() {
        StaticInfoArtifact.getBuildTeam().requestHelp(this.agentName);
    }

	public boolean canSee(Location loc) {
	    return loc.inRange(getEntity().getLocation());
//		return StaticInfoArtifact.getMap().isVisible(getEntity().getLocation(), loc, getEntity().getCurrentVision());
	}

	@OPERATION
	void canSee(double lat, double lon, OpFeedbackParam<Boolean> canSee) {
		canSee.set(canSee(new Location(lon, lat)));
	}

	@OPERATION
	void markWellDestroyed() {
		FacilityArtifact.destroyWell(this);
		getObsProperty("inFacility").updateValue("none");
		getObsProperty("inOwnWell").updateValue(false);
	}

    private synchronized void stopScouting() {
        String role = this.getEntity().getRole().getName();
        long dronesLeft = scouts.stream().filter(a -> getEntity(a).getRole().getName().equals("drone")).count();

        if (role.equals("drone") && dronesLeft == 1) {
            return;
        }

        getObsProperty("scout").updateValue(false);
        scouts.remove(this.agentName);

        if (role.equals("drone")) {
            setToDeliver();
        } else {
            setToGather();
        }
    }

	public static void setBuilders() {
		int wellPrice;
		try {
			wellPrice = StaticInfoArtifact.getBestWellType(DynamicInfoArtifact.getMoney()).getCost();
		} catch (NullPointerException e) {
			// System.out.println("No best well type. Not enough massium");
			return;
		}

		artifacts.values().stream()
			.filter(a ->
				a != null && a.getEntity() != null && a.getEntity().getRole() != null &&
				a.getEntity().getRole().getName().equals("truck") &&
				(boolean)a.getObsProperty("gather").getValue() &&
				!wellBuilders.contains(a))
			.sorted(
				Comparator.comparingDouble(a ->
					FacilityArtifact.euclideanDistance(
						a.getEntity().getLocation(),
						StaticInfoArtifact.getMap().getClosestPeriphery(a.getEntity().getLocation(), EPSILON)
					)
				)
			)
			.limit(DynamicInfoArtifact.getMoney() / wellPrice)
			.forEach(AgentArtifact::setToBuild);

		System.out.println("Current builders: " + wellBuilders.size() + " - " + wellBuilders);
	}

    public void setToBuild() {
		try {
			setToBuild(StaticInfoArtifact.getBestWellType(DynamicInfoArtifact.getMoney()).getName(), new OpFeedbackParam<>());
		} catch (NullPointerException e) {
			// Do nothing
		}
	}

    @OPERATION
    void setToBuild(String wellType, OpFeedbackParam<Boolean> canBuild) {
		try {
			buildSemaphore.acquire();
			try {
				int price = StaticInfoArtifact.getWellTypes().stream().filter(x -> x.getName().equals(wellType)).findFirst().get().getCost();
				if (DynamicInfoArtifact.getMoney() / price > wellBuilders.size()) {
					getObsProperty("build").updateValue(true);
					wellBuilders.add(this.agentName);
				}
			} catch (NoSuchElementException e) {
				System.out.println("Could not set to builder. Error: ");
				// e.printStackTrace();
			}
			buildSemaphore.release();
		} catch (InterruptedException e) {
			logger.warning("Thread interrupted in setToBuild");
		}
		canBuild.set((boolean)getObsProperty("build").getValue()); // Only set false if not already a builder
    }

    @OPERATION
    void stopBuilding() {
		try {
			buildSemaphore.acquire();
			try {
				getObsProperty("build").updateValue(false);
				wellBuilders.remove(this.agentName);
				int price = StaticInfoArtifact.getWellTypes().stream().findFirst().get().getCost();
				buildSemaphore.release();
			} catch (NoSuchElementException e) {
				buildSemaphore.release();
			}
		} catch (InterruptedException e) {
			logger.warning("Thread interrupted in stopBuilding");
		}
    }

	@OPERATION
    public void setToDestroy() {
		try {
			destroySemaphore.acquire();
			if (destroyers.size() < MAX_DESTROYERS) {
				getObsProperty("destroy").updateValue(true);
				destroyers.add(this.agentName);
			}
			destroySemaphore.release();
		} catch (InterruptedException e) {
			logger.warning("Thread interrupted in setToDestroy");
		}
    }

    public static boolean needMoreDestroyers() {
		return destroyers.size() < MAX_DESTROYERS;
	}

    @OPERATION
    void stopDestroying() {
		try {
			destroySemaphore.acquire();
			getObsProperty("destroy").updateValue(false);
			destroyers.remove(this.agentName);
			destroySemaphore.release();
		} catch (InterruptedException e) {
			logger.warning("Thread interrupted in stopDestroying");
		}
    }
/*
    private void setGlobalProperty(String property, Object value) {
		for (AgentArtifact artifact : artifacts.values()) {
			artifact.getObsProperty(property).updateValue(value);
		}
	}
	*/

    /**
	 * Resets the agent artifact
	 */
	public void reset()
	{
		
	}

	public void setToGather() {
		getObsProperty("gather").updateValue(true);
	}

    public void setToBuilder() {
        getObsProperty("builder").updateValue(true);
    }

	public void setToDeliver() {
		getObsProperty("deliver").updateValue(true);
	}
}
