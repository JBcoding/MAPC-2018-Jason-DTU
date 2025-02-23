package info;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import data.*;
import eis.iilang.Percept;
import env.EIArtifact;
import env.Translator;
import massim.protocol.scenario.city.data.RoleData;
import massim.scenario.city.data.*;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.data.facilities.WellType;
import massim.util.RNG;

public class StaticInfoArtifact extends Artifact {
	
	private static final Logger logger = Logger.getLogger(StaticInfoArtifact.class.getName());

	private static final String ENTITY 				= "entity";
	private static final String ID 					= "id";
	private static final String MAP 				= "map";
	private static final String ROLE	 			= "role";
	private static final String SEED_CAPITAL 		= "seedCapital";
	private static final String STEPS 				= "steps";
    private static final String TEAM 				= "team";
    private static final String UPGRADE 			= "upgrade";
    private static final String WELL_TYPE 			= "wellType";
    private static final String PROXIMITY 			= "proximity";

    private static final String CELL_SIZE 			= "cellSize";
    private static final String MIN_LAT 			= "minLat";
    private static final String MIN_LON 			= "minLon";
    private static final String MAX_LAT 			= "maxLat";
    private static final String MAX_LON 			= "maxLon";
    private static final String CENTER_LAT 			= "centerLat";
    private static final String CENTER_LON 			= "centerLon";
	
	public static final Set<String>	PERCEPTS = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(ENTITY, ID, MAP, ROLE, SEED_CAPITAL, STEPS, TEAM, UPGRADE, WELL_TYPE, PROXIMITY,
				CELL_SIZE, MIN_LAT, MIN_LON, MAX_LAT, MAX_LON, CENTER_LAT, CENTER_LON)));
	
	// Entities are stored in DynamicInfoArtifact
	private static String 				    id;
	private static String 			    	map;
	private static Map<String, Role>    	roles = new HashMap<>();
	private static int				    	seedCapital;
	private static int				    	steps;
	private static String			    	team;
	private static Map<String, Upgrade>     upgrades = new HashMap<>();
	private static Map<String, WellType>    wellTypes = new HashMap<>();
	private static int                      proximity;
	private static CCityMap				    cityMap;
    private static CExploredMap exploredMap;

    private static CStorage storage;
    private static CBuildTeam buildTeam;
	private static CDeliverTeam deliverTeam;
	private static int currentStep = 0;

	public static CStorage getStorage() {
        return storage;
    }

    public static void donePerceiving() {
        storage = new CStorage();
    }

    public static CBuildTeam getBuildTeam() {
        if (buildTeam == null) {
            buildTeam = new CBuildTeam();
        }
        return buildTeam;
    }

	public static void incrementStep() {
		currentStep ++;
		/*if (currentStep == 10) {
		    getStorage().gatherEnabled = false;
        }*/
	}

	public static int getCurrentStep() {
		return currentStep;
	}

	@OPERATION
	public static CDeliverTeam getDeliverTeam() {
		if (deliverTeam == null) {
			deliverTeam = new CDeliverTeam();
		}
		return deliverTeam;
	}

    @OPERATION
	void getSimulationData(OpFeedbackParam<String> id, OpFeedbackParam<String> map,
			OpFeedbackParam<Integer> seedCapital, OpFeedbackParam<Integer> steps, 
			OpFeedbackParam<String> team)
	{
		id			.set(StaticInfoArtifact.id);
		map			.set(StaticInfoArtifact.map);
		seedCapital	.set(StaticInfoArtifact.seedCapital);
		steps		.set(StaticInfoArtifact.steps);
		team		.set(StaticInfoArtifact.team);
	}
	
	/**
	 * @param type of the role to get
	 * @return The role associated with the type
	 */
	public static Role getRole(String type)
	{
		return roles.get(type);
	}
	
	/**
	 * @return The map of the city, which is able to calculate route between points
	 */
	public static CCityMap getMap()
	{
		return cityMap;
	}

    public static CExploredMap getExploredMap() {
        return exploredMap;
    }

    /**
	 * Get the route from an agent to any location
	 * @param agentName Name of the agent
	 * @param to The location
	 * @return The route to the location
	 */
	public static Route getRoute(String agentName, Location to) {
		CEntity agent = AgentArtifact.getEntity(agentName);
		
		return StaticInfoArtifact.getMap().findRoute(agent.getLocation(), to, agent.getPermissions());
	}

	public static Route getRoute(String agentName, Location from, Location to) {
		CEntity agent = AgentArtifact.getEntity(agentName);

		return StaticInfoArtifact.getMap().findRoute(from, to, agent.getPermissions());
	}

	public static void perceiveInitial(Collection<Percept> percepts) {

		// Roles and team are used when perceiving entities
		percepts.stream().filter(percept -> percept.getName().equals(ROLE))
						 .forEach(role -> perceiveRole(role));

		percepts.stream().filter(percept -> percept.getName().equals(TEAM))
						 .forEach(team -> perceiveTeam(team));

        perceiveMap(
            new Percept[]{
                percepts.stream().filter(percept -> percept.getName().equals(MAP))
                        .findFirst().get(),
                percepts.stream().filter(percept -> percept.getName().equals(CELL_SIZE))
                        .findFirst().get(),
                percepts.stream().filter(percept -> percept.getName().equals(MIN_LAT))
                        .findFirst().get(),
                percepts.stream().filter(percept -> percept.getName().equals(MIN_LON))
                        .findFirst().get(),
                percepts.stream().filter(percept -> percept.getName().equals(MAX_LAT))
                        .findFirst().get(),
                percepts.stream().filter(percept -> percept.getName().equals(MAX_LON))
                        .findFirst().get(),
                percepts.stream().filter(percept -> percept.getName().equals(CENTER_LAT))
                        .findFirst().get(),
                percepts.stream().filter(percept -> percept.getName().equals(CENTER_LON))
                        .findFirst().get()
            }
        );

		for (Percept percept : percepts)
		{
			switch (percept.getName())
			{
                case ENTITY: 		perceiveEntity		(percept);	break;
                case ID:			perceiveId			(percept);  break;
                case SEED_CAPITAL:	perceiveSeedCapital	(percept);  break;
                case STEPS:			perceiveSteps		(percept);  break;
                case TEAM:			perceiveTeam		(percept); 	break;
                case UPGRADE:       perceiveUpgrade     (percept);  break;
                case WELL_TYPE:     perceiveWellType    (percept);  break;
                case PROXIMITY:     perceiveProximity   (percept);  break;
			}
		}

		upgrades.forEach((k, v) -> System.out.println("Upgrade available: " + k + " : " + v.getName() + " : " + v.getCost() + " : " + v.getStep()));

		if (EIArtifact.LOGGING_ENABLED)
		{
			logger.info("Perceived static info");
			logger.info("Perceived roles: " 		+ roles.keySet());
			logger.info("Perceived team:\t" 		+ team);
			logger.info("Perceived id:\t\t" 		+ id);
			logger.info("Perceived map:\t" 			+ map);
			logger.info("Perceived seedCapital:\t" 	+ seedCapital);
            logger.info("Perceived steps:\t" 		+ steps);
            logger.info("Perceived upgrades:\t" 		+ upgrades);
            logger.info("Perceived well types:\t" 		+ wellTypes);
            logger.info("Perceived proximity:\t" 		+ proximity);
		}
	}
	
	// Literal(String, String, double, double, String)
	private static void perceiveEntity(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);

		String team = (String) args[1];
		String name = ((String) args[0]); // This is team + number
		double lat 	= (double) args[2];
		double lon 	= (double) args[3];
		String role = (String) args[4];

		// Entity has not been made public
		if (team.equals(StaticInfoArtifact.team))
		{
		    name = "agent" + name.replace(team, ""); // This now matches what Jason names artifacts
			AgentArtifact.addEntity(name, new CEntity(roles.get(role), new Location(lon, lat)));
		}
	}
	
	// Literal(String)
	private static void perceiveId(Percept percept)
	{
		//Object[] args = Translator.perceptToObject(percept);
		//[] args = Translator.perceptToObject(percept);

		id = "hej";
	}

	// Literal(String)
	private static void perceiveMap(Percept[] mapPercepts)
	{
	    Object[] args = new Object[mapPercepts.length];
	    for (int i = 0; i < mapPercepts.length; i++) {
            // There is only one argument for each of these percepts
	        args[i] = Translator.perceptToObject(mapPercepts[i])[0];
        }

        map = (String) args[0];
        int cellSize = (int) args[1];
        double minLat = (double) args[2];
        double minLon = (double) args[3];
        double maxLat = (double) args[4];
        double maxLon = (double) args[5];
        Location center = new Location((double)args[7], (double)args[6]);

        System.out.println("AAAAA " + minLat + "|" + maxLat + "|" + minLon + "|" + maxLon);
        System.out.println(center.getLat() + "||" + center.getLon());

		cityMap = new CCityMap(map, cellSize, minLat, maxLat, minLon, maxLon, center);
		exploredMap = new CExploredMap(cityMap);
	}

	// Literal(String,)
	private static void perceiveRole(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);

        String name = (String) args[0];
        int baseSpeed = (int) args[1];
        int maxSpeed = (int) args[2];
        int baseLoad = (int) args[3];
        int maxLoad = (int) args[4];
        int baseSkill = (int) args[5];
        int maxSkill = (int) args[6];
        int baseVision = (int) args[7];
        int maxVision = (int) args[8];
        int baseBattery = (int) args[9];
        int maxBattery = (int) args[10];


        RoleData rd = new RoleData(name, baseSpeed, maxSpeed, baseBattery, maxBattery, baseLoad, maxLoad, baseSkill, maxSkill, baseVision, maxVision);

        Set<String> permissions = new HashSet<>();
        if (name.equals("drone")) {
            permissions.add("air");
        } else {
            permissions.add("road");
        }

        Role role = RoleGetter.getRole(rd, permissions);
		roles.put(name, role);
	}

	// Literal(int)
	private static void perceiveSeedCapital(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		seedCapital = (int) args[0];
	}

    // Literal(int)
    private static void perceiveSteps(Percept percept)
    {
        Object[] args = Translator.perceptToObject(percept);

        steps = (int) args[0];
    }

    // Literal(int)
    private static void perceiveProximity(Percept percept)
    {
        Object[] args = Translator.perceptToObject(percept);

        proximity = (int) args[0];
		Location.setProximity(proximity);
    }

    // Literal(String)
    private static void perceiveTeam(Percept percept)
    {
        Object[] args = Translator.perceptToObject(percept);

        team = (String) args[0];
        //System.out.println(team);
    }

    private static void perceiveUpgrade(Percept percept)
    {
        Object[] args = Translator.perceptToObject(percept);

        String name = (String) args[0];
        int cost = (int) args[1];
        int step = (int) args[2];

        upgrades.put(name, new Upgrade(name, cost, step));
    }

    private static void perceiveWellType(Percept percept)
    {
        Object[] args = Translator.perceptToObject(percept);

        String name = (String) args[0];
        int cost = (int) args[1];
        int efficiency = (int) args[2];
        int initialIntegrity = (int) args[3];
        int integrity = (int) args[4];

        wellTypes.put(name, new WellType(name, initialIntegrity, integrity, cost, efficiency));
    }

    /**
     * @return The roles in this simulation
     */
    public static Collection<Role> getRoles()
    {
        return roles.values();
    }

    /**
     * @return The upgrades available in this simulation
     */
    public static Collection<Upgrade> getUpgrades()
    {
        return upgrades.values();
    }

    /**
     * @return The well types in this simulation
     */
    public static Collection<WellType> getWellTypes()
    {
        return wellTypes.values();
    }

    /**
	* @return The name of the team
	*/
	public static String getTeam()
	{
		return team;
	}

    /**
     * @return Number of steps in the simulation
     */
    public static int getSteps()
    {
        return steps;
    }

    /**
     * @return The proximity used in the simulation
     */
    public static int getProximity()
    {
        return proximity;
    }

    public static WellType getBestWellType(int money) {
		try {
    		return wellTypes.values().stream().filter(wt -> wt.getCost() <= money).max((w1, w2) -> new WellComparer().compare(w1, w2)).get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	private static class WellComparer implements Comparator<WellType> {

		@Override
		public int compare(WellType w1, WellType w2) {
			return getValue(w1) - getValue(w2);
		}

		private int getValue(WellType wt) {
			double eff = (double)wt.getEfficiency();
			double cost = (double)wt.getCost();
			double inte = (double)wt.getCost();
			return (int)(eff/cost * (inte * (eff/cost)));
		}
	}

    @OPERATION
    void bestWellType(int money, OpFeedbackParam<String> wellType)
    {
    	WellType wt = getBestWellType(money);
            wellType.set(wt == null ? "none" : wt.getName());
    }

    @OPERATION
	void getUpgradePrice(String type, OpFeedbackParam<Integer> price) {
		price.set(upgrades.get(type).getCost());
	}

	public static void reset()
	{
		id 			= "";
		map 		= "";
		roles 		= new HashMap<>();
		seedCapital = 0;
		steps 		= 0;
		team 		= "";
		cityMap 	= null;
        upgrades    = new HashMap<>();
        wellTypes   = new HashMap<>();
        proximity   = 0;
	}
}
