package mapc2017.env.perceive;

import java.util.Collection;

import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import eis.iilang.Percept;
import mapc2017.data.JobStatistics;
import mapc2017.data.facility.ChargingStation;
import mapc2017.data.facility.Shop;
import mapc2017.data.facility.Storage;
import mapc2017.data.item.Item;
import mapc2017.data.job.Job;
import mapc2017.env.info.DynamicInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.info.JobInfo;
import mapc2017.env.info.StaticInfo;
import mapc2017.env.job.JobDelegator;
import mapc2017.env.job.JobEvaluator;
import mapc2017.env.parse.IILParser;
import mapc2017.logging.ErrorLogger;
import mapc2017.logging.StatsLogger;

public class ReqActionPerceiver extends Artifact {

	// DYNAMIC
	private static final String DEADLINE			= "deadline";
	private static final String MONEY 				= "money";
	private static final String STEP 				= "step";
	private static final String TIMESTAMP 			= "timestamp";	
	// FACILITY
	private static final String CHARGING_STATION 	= "chargingStation";
	private static final String DUMP 				= "dump";
	private static final String RESOURCE_NODE		= "resourceNode";	
	private static final String SHOP 				= "shop";
	private static final String STORAGE 			= "storage";
	private static final String WORKSHOP 			= "workshop";
	// JOB
	private static final String AUCTION 			= "auction";
	private static final String JOB 				= "job";
	private static final String MISSION 			= "mission";
	private static final String POSTED 				= "posted";	
	
	// Adopts the singleton pattern
	private static ReqActionPerceiver instance;
	
	// Holds request-action related info
	private DynamicInfo 	dInfo;
	private FacilityInfo 	fInfo;
	private ItemInfo		iInfo;
	private JobInfo			jInfo;
	private StaticInfo		sInfo;
	private JobEvaluator 	evaluator;
	private JobDelegator	delegator;
	
	private Thread delegationThread;
	
	void init()
	{
		instance = this;
		
		dInfo 		= DynamicInfo	.get();
		fInfo 		= FacilityInfo	.get();
		iInfo 		= ItemInfo		.get();
		jInfo 		= JobInfo		.get();
		sInfo		= StaticInfo	.get();
		evaluator 	= JobEvaluator	.get();
		delegator 	= JobDelegator	.get();
		
		delegationThread = new Thread(delegator);
	}
	
	public static void perceiveInitial(Collection<Percept> percepts)
	{
		instance.processInitial(percepts);
	}
	
	private void processInitial(Collection<Percept> percepts)
	{
		iInfo.clearItemLocations();
		
		for (Percept p : percepts)
		{
			switch (p.getName())
			{
			case CHARGING_STATION	: fInfo.putFacility	(IILParser.parseChargingStation	(p)); break;
			case DUMP 			    : fInfo.putFacility	(IILParser.parseDump			(p)); break;
			case SHOP 			    : fInfo.putFacility	(IILParser.parseShop            (p)); break;
			case STORAGE 		    : fInfo.putFacility	(IILParser.parseStorage         (p)); break;
			case WORKSHOP 		    : fInfo.putFacility	(IILParser.parseWorkshop        (p)); break;
			}
		}
		
		for (Shop shop : fInfo.getShops())
			for (String item : shop.getItems())
				iInfo.addItemLocation(item, shop);
		
		for (Item item : iInfo.getItems())
			item.calculateItemAvailability();
		
//		StatsLogger.printItemStats();		
//		StatsLogger.printShopStats();
	}
	
	public static void perceive(Collection<Percept> percepts) 
	{
		instance.process(percepts);
	}

	private synchronized void process(Collection<Percept> percepts) 
	{		
		preprocess();
		
		for (Percept p : percepts)
		{
			switch (p.getName())
			{
			case DEADLINE		    : dInfo.setDeadline	(IILParser.parseLong			(p)); break;
			case MONEY 			    : dInfo.setMoney	(IILParser.parseLong			(p)); break;
			case STEP 			    : dInfo.setStep		(IILParser.parseInt				(p)); break;
			case TIMESTAMP 		    : dInfo.setTimestamp(IILParser.parseLong			(p)); break;
			case RESOURCE_NODE	    : fInfo.putFacility	(IILParser.parseResourceNode    (p)); break;
			case SHOP 			    : fInfo.setShop		(IILParser.parseShop            (p)); break;
			case STORAGE			: fInfo.setStorage	(IILParser.parseStorage			(p)); break;
			case AUCTION 		    : jInfo.addJob		(IILParser.parseAuction			(p)); break;
			case JOB 			    : jInfo.addJob		(IILParser.parseSimple			(p)); break;
			case MISSION 		    : jInfo.addJob		(IILParser.parseMission			(p)); break;
			case POSTED 			: jInfo.addJob		(IILParser.parsePosted			(p)); break;
			}
		}
	
		postprocess();
	}
	
	private void preprocess()
	{
		jInfo.setRemovedJobs();
	}

	private void postprocess()
	{		
//		if (dInfo.getStep() == sInfo.getSteps() - 1)
//		{			
//			StatsLogger.printStats();
//			StatsLogger.printOverallStats();
//			StatsLogger.printJobStepStats();
//			StatsLogger.printAgentInventoryStats();
//			StatsLogger.get().println(String.format("Final result: %d", dInfo.getMoney()));
//			ErrorLogger.get().println(String.format("Step: %4d Money: %6d", dInfo.getStep(), dInfo.getMoney()));
//			StatsLogger.reset();
//			ErrorLogger.reset();
//			JobStatistics.resetStats();
//		}
//		else if (dInfo.getStep() % 25 == 0) 
//		{
//			ErrorLogger.get().println(String.format("Step: %4d - Money: %6d", dInfo.getStep(), dInfo.getMoney()));
//			StatsLogger.printStats();
//		}
		
		for (Item item : iInfo.getItems())
			item.calculateItemAvailability();
		
		for (ChargingStation chargingStation : fInfo.getChargingStations())
			chargingStation.step();
		
		for (Job job : jInfo.getRemovedJobs()) 
		{
			jInfo.removeJob(job);
			evaluator.removeEvaluation(job);
			delegator.releaseAgents(job);
		}
		
		for (Storage storage : fInfo.getUnretrievedStorages())
		{			
			delegator.retrieveDelivered(storage);
		}
		
		for (Job job : jInfo.getNewJobs())
		{
			evaluator.evaluate(job);
			
//			JobStatistics.addJob(job);
		}
		
//		delegator.select(evaluator.getEvaluations());
		
		if (dInfo.getStep() == sInfo.getSteps() - 1)
		{
			delegationThread.interrupt();
		}		
		else if (!delegationThread.isAlive()) 
		{
			delegationThread = new Thread(delegator);
			delegationThread.start();
		}
		
//		try 
//		{			
//			int count = 0;
//			
//			while (delegationThread.isAlive() && count <= 2000)
//			{
//				Thread.sleep(100);
//				count += 100;
//			}
//		} 
//		catch (InterruptedException e) 
//		{
//			e.printStackTrace();
//		}
		
		execInternalOp("update");
	}
	
	@INTERNAL_OPERATION
	private void update()
	{
		signal("step", dInfo.getStep());
	}

}
