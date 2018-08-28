package mapc2017.env;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import cartago.Artifact;
import cartago.ArtifactConfig;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import eis.AgentListener;
import eis.EnvironmentInterfaceStandard;
import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.DynamicInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.info.JobInfo;
import mapc2017.env.info.StaticInfo;
import mapc2017.env.job.JobEvaluator;
import mapc2017.env.parse.IILParser;
import mapc2017.env.parse.IILTranslator;
import mapc2017.env.perceive.AgentPerceiver;
import mapc2017.env.perceive.ReqActionPerceiver;
import mapc2017.env.perceive.SimStartPerceiver;
import mapc2017.logging.ErrorLogger;
import massim.eismassim.EnvironmentInterface;

public class EISHandler extends Artifact implements AgentListener {

    private static final Logger logger = Logger.getLogger(EISHandler.class.getName());
	    
    private static final String CONFIG = "conf/client/eismassimconfig_A.json";
//    private static final String CONFIG = "conf/client/eismassimconfig_B.json";
//    private static final String CONFIG = "conf/client/eismassimconfig_C.json";
//    private static final String CONFIG = "conf/client/eismassimconfig_mapc2017_ac1.json";
//    private static final String CONFIG = "conf/client/eismassimconfig_mapc2017_ac2.json";
    
    private Map<String, String> agentsToEntities 	= new HashMap<>();    
    private Set<String> 		hasPerformedAction	= new HashSet<>(),
    							hasQueuedAction		= new HashSet<>();

    private EnvironmentInterfaceStandard ei;

    void init() 
    {		
		try 
		{
			ei = new EnvironmentInterface(CONFIG);
			
			registerAgents();
			
			instantiateInfo();
						
			execInternalOp("makeArtifacts");
		} 
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in init: " + e.getMessage(), e);
		}
    }
    
    private void registerAgents()
    {
    	try
    	{
    		for (String entity : ei.getEntities())
    		{
    			String agent = getAgent(entity);
    			
        		ei.registerAgent(agent);
        		
        		ei.associateEntity(agent, entity);
        		
        		agentsToEntities.put(agent, entity);
    		}
    	}
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in register: " + e.getMessage(), e);
		}
    }
    
    private void instantiateInfo()
    {
    	for (String agent : agentsToEntities.keySet())
    		new AgentInfo(agent);
    	
    	new DynamicInfo();
    	new FacilityInfo();
    	new ItemInfo();
    	new JobInfo();
    	new StaticInfo();
    	
    	new JobEvaluator();
    }
    
    @INTERNAL_OPERATION
    private void makeArtifacts()
    {
    	try 
    	{			
    		makeArtifact("JobDelegator",		"mapc2017.env.job.JobDelegator", 			ArtifactConfig.DEFAULT_CONFIG);
    		makeArtifact("SimStartPerceiver", 	"mapc2017.env.perceive.SimStartPerceiver", 	ArtifactConfig.DEFAULT_CONFIG);
    		makeArtifact("ReqActionPerceiver", 	"mapc2017.env.perceive.ReqActionPerceiver", ArtifactConfig.DEFAULT_CONFIG);

    		for (String agent : agentsToEntities.keySet())
				makeArtifact(agent, 			"mapc2017.env.perceive.AgentPerceiver", 	ArtifactConfig.DEFAULT_CONFIG);
			
    		ei.attachAgentListener("agent1", this);
			
			ei.start();
    	}
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in makeArtifacts: " + e.getMessage(), e);
		}
    }
	
	@INTERNAL_OPERATION
	void perceive()
	{
		Set<Percept> allPercepts = new HashSet<>();

		for (String agent : agentsToEntities.keySet())
		{
			Collection<Percept> percepts = getPercepts(agent);
			
			AgentPerceiver.perceive(agent, percepts);

			allPercepts.addAll(percepts);
		}
		
		hasPerformedAction.clear();
		hasPerformedAction.addAll(hasQueuedAction);
		hasQueuedAction	  .clear();
		
		if (SimStartPerceiver.hasPerceived() == false)
		{
			SimStartPerceiver .perceive		  (allPercepts);
			ReqActionPerceiver.perceiveInitial(allPercepts);
		}
		
		try
		{
			ReqActionPerceiver.perceive(allPercepts);			
		}
		catch (Throwable e) 
		{
			e.printStackTrace();
			ErrorLogger.get().println("*********** Failure in perceive: " + e.getMessage());
		}
	}
    
    @OPERATION
    void performAction(String action)
    {    	
    	String agent = getOpUserName();
    	
		if (hasQueuedAction.contains(agent))
		{
			System.out.println(String.format("[%s] Has queued action: %s", agent, action));
			return;
		}		
		else if (DynamicInfo.isDeadlinePassed() && !DynamicInfo.isLastStep())
		{
//			System.out.println(String.format("[%s] Queued action: %s", agent, action));
			hasQueuedAction.add(agent);
		}
		else if (hasPerformedAction.contains(agent))
		{
			System.out.println(String.format("[%s] Has performed action: %s", agent, action));
			return;
		}		
		else
		{			
			hasPerformedAction.add(agent);
		}
		
		try 
		{	    	
			ei.performAction(agent, getAction(action));
		}
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in performAction: " + e.getMessage(), e);
		}
    }
	
	private Collection<Percept> getPercepts(String agent)
	{
		try
		{
			return ei.getAllPercepts(agent).get(getEntity(agent));
		}
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in getPercepts: " + e.getMessage(), e);
			
			return Collections.emptySet();
		}
	}
	
	private Action getAction(String literal)
	{
		Action action = IILTranslator.literalToAction(literal);
		
		if (action.getName().equals("assist_assemble"))
		{
			String agent = IILParser.parseString(action.getParameters().getFirst());
			
			Identifier agentToAssist = new Identifier(getEntity(agent));
			
			action.setParameters(new LinkedList<Parameter>(Arrays.asList(agentToAssist)));
		}
		
		return action;
	}
	
	public static String getAgent(String entity)
	{
		return "agent" + entity.replaceAll("[^0-9]", "");
	}
	
	private String getEntity(String agent)
	{
		return agentsToEntities.get(agent);
	}

	@Override
	public void handlePercept(String arg0, Percept arg1)
	{
		if (arg1.getName().equals("step"))
		{
			execInternalOp("perceive");
		}
		else if (arg1.getName().equals("simEnd"))
		{
			SimStartPerceiver.setPerceived(false);
		}
	}
}
