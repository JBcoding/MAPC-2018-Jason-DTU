package info;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cartago.Artifact;
import cartago.GUARD;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import data.CCityMap;
import data.CEntity;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import eis.iilang.PrologVisitor;
import env.EIArtifact;
import env.Translator;
import massim.protocol.messagecontent.Action;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Route;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.data.facilities.Well;

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
	public static boolean isScouting = true;

	private static final double EPSILON = 1E-3;

	private static ConcurrentLinkedDeque<String> scouts = new ConcurrentLinkedDeque<>();
	
	void init()
	{
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
	
	protected static void addEntity(String name, CEntity entity)
	{
		entities.put(name, entity);
		entity.addAgentArtifact(artifacts.get(name));
	}
	
	public static CEntity getEntity(String name)
	{
		return entities.get(name);
	}
	
	/**
	 * @return The entity associated with this agent artifact
	 */
	private CEntity getEntity()
	{
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
	private void update(Collection<Percept> percepts)
	{

		int load = this.getEntity().getCurrentLoad();

		this.getEntity().clearInventory();

		boolean positionChange = true;
		for (Percept percept : percepts)
		{
		    if (percept.getName().equals(LAT) || percept.getName().equals(LON)) {
		        positionChange = true;
            }
			switch (percept.getName())
			{
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
		

		if (load != this.getEntity().getCurrentLoad())
		{
			getObsProperty("load").updateValue(this.getEntity().getCurrentLoad());
		}

		getObsProperty("lastActionParam").updateValue(this.getEntity().getLastActionParam());

		updateAtPeriphery();

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
			m.isVisible(l, new Location(m.getMinLon(), l.getLat()), vision) ||
			m.isVisible(l, new Location(m.getMaxLon(), l.getLat()), vision) ||
			m.isVisible(l, new Location(l.getLon(), m.getMinLat()), vision) ||
			m.isVisible(l, new Location(l.getLon(), m.getMaxLat()), vision)
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
		if (!PrologVisitor.staticVisit(param).equals(""))
		{
			Object[] args = Translator.perceptToObject(percept);
			
			Facility facility = FacilityArtifact.getFacility((String) args[0]);

			if (!this.getEntity().getFacilityName().equals(facility.getName())) {
				this.getEntity().setFacility(facility);
				getObsProperty("inFacility").updateValue(this.getEntity().getFacilityName());

				if (facility instanceof Well) {
					getObsProperty("inOwnWell").updateValue(((Well)facility).getTeam().equals(StaticInfoArtifact.getTeam()));
				}
			}
		}
		else 
		{
			if (!this.getEntity().getFacilityName().equals("none"))
			{
				this.getEntity().setFacility(null);
				getObsProperty("inFacility").updateValue(this.getEntity().getFacilityName());
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
	        /*
	        for (String scout : scouts) {
                AgentArtifact.getAgentArtifact(scout).stopScouting();
            }
            scouts.clear();
            */
	        stopScouting();

	        isScouting = false;
        }
    }

    private void stopScouting() {
        // System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + this.agentName);
        getObsProperty("scout").updateValue(false);
        // TODO: remove !scout(t) intention??
    }

    /**
	 * Resets the agent artifact
	 */
	public void reset()
	{
		
	}

    public void setToScout() {
        getObsProperty("scout").updateValue(true);
        scouts.add(this.agentName);
    }
}
