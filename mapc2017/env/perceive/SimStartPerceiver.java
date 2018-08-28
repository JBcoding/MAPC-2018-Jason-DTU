package mapc2017.env.perceive;

import java.util.Collection;

import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import eis.iilang.Percept;
import mapc2017.data.Entity;
import mapc2017.data.Role;
import mapc2017.data.item.Item;
import mapc2017.env.EISHandler;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.info.StaticInfo;
import mapc2017.env.job.JobEvaluator;
import mapc2017.env.parse.IILParser;

public class SimStartPerceiver extends Artifact {

	// DYNAMIC (but only used initially)
	private static final String ENTITY 				= "entity";
	// ITEM
	private static final String ITEM 				= "item";		
	// STATIC
	private static final String ID 					= "id";
	private static final String MAP 				= "map";
	private static final String CELL_SIZE 			= "cellSize";
	private static final String CENTER_LAT 			= "centerLat";
	private static final String CENTER_LON 			= "centerLon";
	private static final String MAX_LAT 			= "maxLat";
	private static final String MAX_LON 			= "maxLon";	
	private static final String MIN_LAT 			= "minLat";
	private static final String MIN_LON 			= "minLon";
	private static final String PROXIMITY 			= "proximity";
	private static final String ROLE	 			= "role";
	private static final String SEED_CAPITAL 		= "seedCapital";
	private static final String STEPS 				= "steps";
	private static final String TEAM 				= "team";
	
	// Adopts the singleton pattern
	private static SimStartPerceiver instance;
	
	private boolean hasPerceived = false;
	
	// Holds sim-start related info
	private FacilityInfo	fInfo;
	private ItemInfo		iInfo;
	private StaticInfo		sInfo;
	
	void init()
	{
		instance = this;
		
		fInfo = FacilityInfo.get();
		iInfo = ItemInfo	.get();
		sInfo = StaticInfo	.get();
	}
	
	public static void perceive(Collection<Percept> percepts) 
	{
		instance.process(percepts);
	}
	
	@INTERNAL_OPERATION
	private void process(Collection<Percept> percepts)
	{
		preprocess();
		
		for (Percept p : percepts)
		{
			switch (p.getName())
			{			
			case ENTITY 			: sInfo.addEntity		(IILParser.parseEntity	(p)); break;
			case ITEM 			    : iInfo.addItem			(IILParser.parseItem	(p)); break;
			case CELL_SIZE			: sInfo.setCellSize		(IILParser.parseDouble	(p)); break;
			case CENTER_LAT			: sInfo.setCenterLat	(IILParser.parseDouble	(p)); break;
			case CENTER_LON			: sInfo.setCenterLon	(IILParser.parseDouble	(p)); break;
			case ID 				: sInfo.setId			(IILParser.parseString	(p)); break;
			case MAP 			    : sInfo.setMap			(IILParser.parseString	(p)); break;
			case MIN_LAT            : sInfo.setMinLat       (IILParser.parseDouble  (p)); break;
			case MAX_LAT            : sInfo.setMaxLat       (IILParser.parseDouble  (p)); break;
			case MIN_LON            : sInfo.setMinLon       (IILParser.parseDouble  (p)); break;
			case MAX_LON            : sInfo.setMaxLon       (IILParser.parseDouble  (p)); break;
			case PROXIMITY			: sInfo.setProximity	(IILParser.parseInt		(p)); break;
			case ROLE	 		    : sInfo.addRole			(IILParser.parseRole	(p)); break;
			case SEED_CAPITAL 	    : sInfo.setSeedCapital	(IILParser.parseLong	(p)); break;
			case STEPS 			    : sInfo.setSteps		(IILParser.parseInt		(p)); break;
			case TEAM 			    : sInfo.setTeam			(IILParser.parseString	(p)); break;
			}
		}

		postprocess();
	}
	
	private void preprocess()
	{
		fInfo.clearFacilities();
		iInfo.clearItems();
	}

	private void postprocess()
	{
		for (Entity entity : sInfo.getTeamEntities())
		{			
			String 	agent 	= EISHandler.getAgent(entity.getName());	
			Role 	role 	= sInfo.getRole(entity.getRole());
			
			AgentInfo.get(agent).setRole(role);
			AgentPerceiver.updateRole(agent);
		}
		
		for (Role role : sInfo.getRoles())
			for (String tool : role.getTools())
				iInfo.getTool(tool).addRole(role.getName());
		
		sInfo.initCityMap();
		
		for (Item item : iInfo.getItems())
			item.calculateBaseRequirements();
		
		JobEvaluator.get().init();
		
		hasPerceived = true;
		
		execInternalOp("update");
	}
	
	@INTERNAL_OPERATION
	private void update() 
	{
		signal("start");
	}
	
	public static boolean hasPerceived() {
		return instance.hasPerceived;
	}
	
	public static void setPerceived(boolean hasPerceived) {
		instance.hasPerceived = hasPerceived;
	}
}
