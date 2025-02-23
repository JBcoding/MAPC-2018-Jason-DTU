package env;
// Environment code for project multiagent_jason

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import eis.AgentListener;
import eis.EnvironmentInterfaceStandard;
import eis.EnvironmentListener;
import eis.iilang.*;
import info.AgentArtifact;
import info.DynamicInfoArtifact;
import info.FacilityArtifact;
import info.ItemArtifact;
import info.JobArtifact;
import info.StaticInfoArtifact;
import logging.LoggerFactory;
import massim.eismassim.EnvironmentInterface;
import massim.scenario.city.data.Location;
import massim.scenario.city.data.Role;
import massim.scenario.city.data.facilities.Facility;

public class EIArtifact extends Artifact implements AgentListener, EnvironmentListener {

    private static final Logger logger = Logger.getLogger(EIArtifact.class.getName());

	private static final boolean LOG_TO_FILE = false;
    public static final boolean LOGGING_ENABLED = false;

    private static EnvironmentInterfaceStandard ei;

    private static final String configFilePath = "conf/config_file_path.txt";

    private static String configFile = "";
    //private static String configFile = "conf/eismassimconfig.json";
	//private static String configFile = "conf/eismassimconfig_connection_test.json";
	//private static String configFile = "conf/eismassimconfig_team_B.json";

    private static Map<String, String> connections 	= new HashMap<>();
    private static Map<String, String> entities		= new HashMap<>();
    
    private static String team;

    /**
     * Instantiates and starts the environment interface.
     */
    void init() 
    {
    	logger.setLevel(Level.SEVERE);
		logger.info("init");
		
		try {
			if (configFile.equals("")) {
				BufferedReader br = new BufferedReader(new FileReader(configFilePath));
				configFile = br.readLine();
				team = br.readLine().trim();
				br.close();
			}

			ei = new EnvironmentInterface(configFile);

			if (LOG_TO_FILE) {
				fileLogger = LoggerFactory.createFileLogger(team);
			}
			ei.start();
		}
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in init: " + e.getMessage(), e);
		}
    }
	
	@OPERATION
	void register() {
        String agentName = getOpUserName();
        String id = agentName.replace("agent", "");
        String connection = "connection" + team + id;
        String entity = "agent" + id;

        logger.fine("register " + agentName + " on " + connection);

        try {
            ei.registerAgent(agentName);

            ei.associateEntity(agentName, connection);

            connections.put(agentName, connection);
            entities.put(agentName, entity);

            if (connections.size() == ei.getEntities().size()) {
                // Attach listener for perceiving the following steps
                ei.attachAgentListener(agentName, this);
            }


            if (connections.size() == ei.getEntities().size()) {
                ei.attachEnvironmentListener(this);
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Failure in register: " + e.getMessage(), e);
        }
    }

    @Override
    public void handlePercept(String s, Percept percept) {
        switch (percept.getName()) {
            case "simStart":
                execInternalOp("perceiveInitial");
                break;
            case "simEnd":
                System.out.println("This is the end!");
                System.out.println(percept);
                break;
            case "step":
				StaticInfoArtifact.incrementStep();
                execInternalOp("perceiveUpdate");
                break;
            default:
                logger.warning("Unhandled percept: " + percept.getName());
        }
    }

	public static void performAction(String agentName, Action action) {
		logger.fine("Step " + DynamicInfoArtifact.getStep() + ": " + agentName + " doing " + action);
		
		try {
			if (action.getName().equals("assist_assemble")) {
				String name = PrologVisitor.staticVisit(action.getParameters().get(0));
				
				LinkedList<Parameter> params = new LinkedList<>();

				// The argument to assist_assemble should just be team + id
				String otherAgent = team + EIArtifact.getAgentName(name).replace("agent", "");
				params.add(new Identifier(otherAgent));
				
				action.setParameters(params);
			} else if (action.getName().equals("goto")) {
			    List<Parameter> params = action.getParameters();
			    if (params.size() == 1) {
                    String facName = PrologVisitor.staticVisit(params.get(0));
                    Facility fac = FacilityArtifact.getFacility(facName);
                    AgentArtifact.getAgentArtifact(agentName).setLastGoto(fac.getLocation());
                } else if (params.size() == 2) {
                    PrologVisitor visitor = new PrologVisitor();
                    double lat = (double) params.get(0).accept(visitor, "");
                    double lon = (double) params.get(1).accept(visitor, "");
                    AgentArtifact.getAgentArtifact(agentName).setLastGoto(new Location(lon, lat));
                }
			}

			ei.performAction(agentName, action);
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Failure in performAction: " + e.getMessage(), e);
		}
	}
	
	@INTERNAL_OPERATION
	void perceiveInitial()
	{
		logger.finest("perceiveInitial");
		if (DynamicInfoArtifact.getStep() == StaticInfoArtifact.getSteps() - 1)
		{
			this.reset();
		}
		
		try 
		{
			Set<Percept> allPercepts = new HashSet<>();
			
			Map<String, Collection<Percept>> agentPercepts = new HashMap<>();
	
			for (Entry<String, String> entry : connections.entrySet())
			{
				String agentName = entry.getKey();
				
				Collection<Percept> percepts = ei.getAllPercepts(agentName).get(entry.getValue());
				
				agentPercepts.put(agentName, percepts);
				
				allPercepts.addAll(percepts);
			}
			
			// Perceive static info
			StaticInfoArtifact  .perceiveInitial(allPercepts);
			ItemArtifact        .perceiveInitial(allPercepts);
			// Perceive dynamic info
			FacilityArtifact	.perceiveUpdate(allPercepts);
			DynamicInfoArtifact	.perceiveUpdate(allPercepts);
			JobArtifact			.perceiveUpdate(allPercepts);

			AgentArtifact.setBuilders();
			
			for (Role role : StaticInfoArtifact.getRoles())
			{
				defineObsProperty("role", role.getName(), role.getMaxSpeed(), role.getMaxLoad(),
						role.getMaxBattery(), role.getPermissions().toArray());
			}

			// Perceive agent info
			for (Entry<String, Collection<Percept>> entry : agentPercepts.entrySet()) {
				String agentName = entry.getKey();
				
				AgentArtifact.getAgentArtifact(agentName).perceiveInitial(entry.getValue());
			}
			
			// Define step
			defineObsProperty("step", DynamicInfoArtifact.getStep());
			defineObsProperty("money", DynamicInfoArtifact.getMoney());
			//defineObsProperty("enoughMoneyForWell", !StaticInfoArtifact.getBestWellType(DynamicInfoArtifact.getMoney()).getName().equals("none"));
			
			FacilityArtifact.announceShops();
		}
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in perceive: " + e.getMessage(), e);
		}
		
		logger.finest("Perceive initial done");

		StaticInfoArtifact.donePerceiving();
	}
	
	@INTERNAL_OPERATION
	void perceiveUpdate() 
	{		
		logger.finest("perceiveUpdate");

		try 
		{
			Set<Percept> allPercepts = new HashSet<>();
			Map<String, Collection<Percept>> allPerceptsMap = new HashMap<>();

			for (Entry<String, String> entry : connections.entrySet())
			{
				Collection<Percept> percepts = ei.getAllPercepts(entry.getKey()).get(entry.getValue());

				allPercepts.addAll(percepts);
                allPerceptsMap.put(entry.getKey(), percepts);
			}
			
			FacilityArtifact	.perceiveUpdate(allPercepts);
			DynamicInfoArtifact	.perceiveUpdate(allPercepts);
			JobArtifact			.perceiveUpdate(allPercepts);

			AgentArtifact.setBuilders();

			for (Entry<String, Collection<Percept>> entry : allPerceptsMap.entrySet())
			{
				AgentArtifact.getAgentArtifact(entry.getKey()).perceiveUpdate(entry.getValue());
			}

            getObsProperty("step").updateValue(DynamicInfoArtifact.getStep());
            getObsProperty("money").updateValue(DynamicInfoArtifact.getMoney());
			//getObsProperty("enoughMoneyForWell").updateValue(StaticInfoArtifact.getBestWellType(DynamicInfoArtifact.getMoney()) != null);

			logData();

			JobArtifact.announceJobs();
		} 
		catch (Throwable e) 
		{
			e.printStackTrace();
			logger.log(Level.SEVERE, "Failure in perceive: " + e.getMessage(), e);
		}
	}

	private void reset() 
	{
		defineObsProperty("reset");
		
		DynamicInfoArtifact.reset();
		StaticInfoArtifact.reset();
		FacilityArtifact.reset();
		JobArtifact.reset();
		ItemArtifact.reset();
		
		for (Entry<String, String> entry : connections.entrySet())
		{
			AgentArtifact.getAgentArtifact(entry.getKey()).reset();
		}

		if (LOG_TO_FILE) {
			fileLogger = LoggerFactory.createFileLogger(team);
		}

		removeObsProperty("step");
		for (Role role : StaticInfoArtifact.getRoles())
		{
			removeObsPropertyByTemplate("role", role.getName(), role.getBaseSpeed(), role.getMaxSpeed(), role.getMaxLoad(),
					role.getMaxBattery(), role.getPermissions().toArray());
		}
		
		removeObsProperty("reset");
	}

	/**
	 * @param entity 
	 * @return Get the name of the agent associated with the entity
	 */
	public static String getAgentName(String entity)
	{
		return entities.get(entity);
	}
	
	private static Logger fileLogger;
	
	private void logData()
	{
		if (LOG_TO_FILE) {
			fileLogger.info("Step: " + DynamicInfoArtifact.getStep() + " - Money: " + DynamicInfoArtifact.getMoney());

			if (DynamicInfoArtifact.getStep() == StaticInfoArtifact.getSteps() - 1) {
				fileLogger.info("Completed jobs: " + DynamicInfoArtifact.getJobsCompleted());
			}
		}
	}

    @Override
    public void handleStateChange(EnvironmentState environmentState) {
	    System.out.println("State change.");
    }

    @Override
    public void handleFreeEntity(String s, Collection<String> collection) {

    }

    @Override
    public void handleDeletedEntity(String s, Collection<String> collection) {

    }

    @Override
    public void handleNewEntity(String s) {

    }
}
