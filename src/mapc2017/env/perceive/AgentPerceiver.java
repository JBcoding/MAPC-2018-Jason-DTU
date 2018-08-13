package mapc2017.env.perceive;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import eis.iilang.Percept;
import mapc2017.data.JobStatistics;
import mapc2017.data.facility.ChargingStation;
import mapc2017.data.facility.Shop;
import mapc2017.data.job.AuctionJob;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.DynamicInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.JobInfo;
import mapc2017.env.parse.IILParser;
import mapc2017.logging.ErrorLogger;

public class AgentPerceiver extends Artifact {
	
	// AGENT
	private static final String ACTION_ID				= "actionID";
	private static final String CHARGE 					= "charge";
	private static final String FACILITY				= "facility";
	private static final String HAS_ITEM				= "hasItem";
	private static final String LAST_ACTION 			= "lastAction";
	private static final String LAST_ACTION_PARAMS 		= "lastActionParams";
	private static final String LAST_ACTION_RESULT 		= "lastActionResult";
	private static final String LAT 					= "lat";
	private static final String LON 					= "lon";
	private static final String LOAD					= "load";
	private static final String ROUTE 					= "route";
	private static final String ROUTE_LENGTH 			= "routeLength";
	
	// ACTION
//	private static final String ASSEMBLE				= "assemble";
	private static final String ASSIST_ASSEMBLE			= "assist_assemble";
	private static final String BID_FOR_JOB				= "bid_for_job";
	private static final String BUY						= "buy";
//	private static final String CHARGE 					= "charge";
	private static final String DELIVER_JOB				= "deliver_job";
	private static final String GATHER					= "gather";
	private static final String GOTO					= "goto";
//	private static final String NO_ACTION				= "noAction";
	private static final String RECHARGE				= "recharge";
	private static final String SKIP					= "skip";
	
	// ACTION RESULT
	private static final String SUCCESSFUL				= "successful";
	private static final String FAILED					= "failed";
	private static final String FAILED_FACILITY_STATE	= "failed_facility_state";
	
	// The artifact's observable properties
	private static final String[] PROPERTIES = new String[] {
			CHARGE,
			LOAD,
			FACILITY,
			LAST_ACTION,
			LAST_ACTION_RESULT,
			LAST_ACTION_PARAMS
	};
	
	private static final String[] NOT_LOGGED = new String[] {
			ASSIST_ASSEMBLE,
			CHARGE,
			GATHER,
			GOTO,
			RECHARGE,
			SKIP
	};
	
	private static Map<String, AgentPerceiver> instances = new HashMap<>();
	
	private AgentInfo 		aInfo;
	private DynamicInfo		dInfo;
	private FacilityInfo 	fInfo;
	private JobInfo			jInfo;

	public void init()
	{
		instances.put(getId().getName(), this);
		
		aInfo = AgentInfo	.get(getId().getName());
		dInfo = DynamicInfo	.get();
		fInfo = FacilityInfo.get();
		jInfo = JobInfo		.get();
		
		for (String property : PROPERTIES)
		{
			defineObsProperty(property, "");
		}
	}
	
	public static void perceive(String agent, Collection<Percept> percepts)
	{
		instances.get(agent).process(percepts);
	}
	
	@INTERNAL_OPERATION
	private void process(Collection<Percept> percepts)
	{		
		preprocess();
		
		for (Percept p : percepts)
		{
			switch (p.getName())
			{
			case ACTION_ID			:                                                   	  break; 
			case CHARGE 			: aInfo.setCharge			(IILParser.parseInt		(p)); break; 
			case FACILITY			: aInfo.setFacility			(IILParser.parseString	(p)); break; 
			case HAS_ITEM			: aInfo.addItem				(IILParser.parseEntry	(p)); break; 
			case LAST_ACTION 		: aInfo.setLastAction		(IILParser.parseString	(p)); break; 
			case LAST_ACTION_RESULT : aInfo.setLastActionResult	(IILParser.parseString	(p)); break; 
			case LAST_ACTION_PARAMS : aInfo.setLastActionParams	(IILParser.parseArray	(p)); break; 
			case LAT 				: aInfo.setLat				(IILParser.parseDouble	(p)); break; 
			case LON 				: aInfo.setLon				(IILParser.parseDouble	(p)); break; 
			case LOAD				: aInfo.setLoad				(IILParser.parseInt		(p)); break; 
			case ROUTE 				:                                                   	  break; 
			case ROUTE_LENGTH 		:                                                   	  break;
			}			
		}

		postprocess();
	}
	
	private void preprocess()
	{
		aInfo.clearInventory();
		aInfo.setLastFacility();
	}

	private void postprocess()
	{				
		String	 lastAction 		= aInfo.getLastAction();
		String 	 lastActionResult 	= aInfo.getLastActionResult();
		String[] lastActionParams 	= aInfo.getLastActionParams();
		
//			 if (lastAction.equals(DELIVER_JOB) &&	lastActionResult.equals(SUCCESSFUL)) 
//		{
//			JobStatistics.completeJob(jInfo.getJob(lastActionParams[0]), dInfo.getStep() - 1);
//		}
//		else 
			 if (lastAction.equals(CHARGE) 		&& lastActionResult.equals(FAILED_FACILITY_STATE)) 
		{
			((ChargingStation) fInfo.getFacility(aInfo.getFacility())).blackout();
		}		
		else if (lastAction.equals(BUY) 		&& lastActionResult.equals(SUCCESSFUL))
		{
			((Shop) fInfo.getFacility(aInfo.getLastFacility())).remReserved(lastActionParams[0], Integer.parseInt(lastActionParams[1]));
		}		
		else if (lastAction.equals(BID_FOR_JOB) && lastActionResult.equals(SUCCESSFUL))
		{
			String 	id 	= lastActionParams[0]; 
			int 	bid = Integer.parseInt(lastActionParams[1]);
			
			AuctionJob auction = (AuctionJob) jInfo.getJob(id);
			auction.setIsHighestBidder(true);
			auction.setBid(bid);
		}
		
		execInternalOp("update");
		
//		if (lastActionResult.startsWith(FAILED))
//		{
//			ErrorLogger.get().println(String.format("%-7s %s %s(%s)", 
//					aInfo.getName(), lastActionResult, lastAction, 
//					Arrays.toString(aInfo.getLastActionParams())));
//		}
//		else 
//		{
//			for (String action : NOT_LOGGED)
//				if (lastAction.equals(action))
//					return;
//			
//			ErrorLogger.get().println(String.format("%-7s %s %s(%s)", 
//					aInfo.getName(), lastActionResult, lastAction, 
//					Arrays.toString(aInfo.getLastActionParams())));
//		}
	}
	
	@INTERNAL_OPERATION
	private void update()
	{
		for (String property : PROPERTIES)
		{
			getObsProperty(property).updateValue(this.getValue(property));
		}
	}
	
	@INTERNAL_OPERATION
	private void updateRole() 
	{		
		defineObsProperty("role", aInfo.getRole().getData());
	}
	
	private Object getValue(String property)
	{
		switch (property)
		{
		case CHARGE            	: return aInfo.getCharge			();
		case LOAD              	: return aInfo.getLoad     			();
		case FACILITY         	: return aInfo.getFacility 			();
		case LAST_ACTION		: return aInfo.getLastAction		();
		case LAST_ACTION_RESULT	: return aInfo.getLastActionResult	();
		case LAST_ACTION_PARAMS	: return aInfo.getLastActionParams	();
		}
		throw new UnsupportedOperationException("Unsupported property: " + property);
	}
	
	public static void updateRole(String agent) 
	{		
		instances.get(agent).execInternalOp("updateRole");
	}

}
