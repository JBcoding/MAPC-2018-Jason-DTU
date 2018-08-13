package mapc2017.logging;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import mapc2017.data.JobStatistics;
import mapc2017.data.facility.Shop;
import mapc2017.data.item.Item;
import mapc2017.data.item.Tool;
import mapc2017.data.job.Job;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.DynamicInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;

public class StatsLogger extends Logger {
	
	private static StatsLogger instance;
	
	public StatsLogger()
	{
		super(getDirectory() + getFileName());
	}
	
	public static void reset() 
	{
		if (instance != null) 
			instance.close();
		instance = new StatsLogger();
	}
	
	public static Logger get() 
	{
		if (instance == null) reset();
		return instance;
	}
	
	public static String getDirectory()
	{
		File path = new File("output/statistics");
		
		if (!path.isDirectory())
			path.mkdir();

		return path.getPath() + "/";
	}

	public static String getFileName()
	{
		return String.format("stats_%d.txt", System.currentTimeMillis());
	}
	
	public static void printItemStats()
	{
		Logger logger = StatsLogger.get();
		
		List<Tool> sortedTools = new ArrayList<>(ItemInfo.get().getTools());		
		List<Item> sortedItems = new ArrayList<>(ItemInfo.get().getItems());
		
		Collections.sort(sortedTools, Comparator.comparingInt(Tool::getNumber));
		Collections.sort(sortedItems, Comparator.comparingInt(Item::getNumber));

		logger.printSeparator();
		
		logger.println(String.format("%-6s%4s%7s %s", "Tool", "Vol", "Avail.", "Roles"));
		
		for (Tool tool : sortedTools)
		{
			logger.println(String.format("%-6s%4d%7.3f %s", tool.getName(), tool.getVolume(), tool.getAvailability(), tool.getRoles()));
		}
		
		logger.printSeparator();
		
		logger.println(String.format("%-6s%4s%7s %s", "Item", "Vol", "Avail.", "Requirements"));
		
		for (Item item : sortedItems)
		{
			logger.println(String.format("%-6s%4d%7.3f %-24s %s", item.getName(), item.getVolume(), item.getAvailability(), item.getReqTools(), item.getReqItems()));
		}
		
		logger.printSeparator();
		logger.println();
	}
	
	public static void printShopStats()
	{
		Logger logger = StatsLogger.get();
		
		List<Shop> sortedShops = new ArrayList<>(FacilityInfo.get().getShops());	
		
		Collections.sort(sortedShops, Comparator.comparingInt(Shop::getNumber));
		
		logger.println(String.format("%-6s %s", "Name", "Items"));
		
		for (Shop shop : sortedShops)
		{
			logger.println(String.format("%-6s %s", shop.getName(), shop.getItems()));
		}
		
		logger.printSeparator();
		logger.println();
	}

	public static void printOverallStats()
	{
		Logger logger = StatsLogger.get();

		logger.println("--- Overall Performance ---");
		logger.println("Money:    					" + DynamicInfo.get().getMoney());
		logger.println("Jobs completed: 			" + JobStatistics.getTotalJobsCompleted());
		logger.println(String.format("Missions: %d %%, \tAuctions: %d %%", 
			JobStatistics.getMissionsCompleted() * 100 / (JobStatistics.getTotalMissions() == 0 ? 1 : JobStatistics.getTotalMissions()),
			JobStatistics.getAuctionsCompleted() * 100 / (JobStatistics.getAuctionsWon() == 0 ? 1 : JobStatistics.getAuctionsWon())
			));
		logger.printSeparator();
	}
	
	public static void printStats() 
	{
		Logger logger = StatsLogger.get();
		
		logger.println(String.format( "%s%3d", "--- Statistics --- Step ", DynamicInfo.get().getStep()			));
		logger.println(String.format( "%s%3d", "Total jobs:             ", JobStatistics.getTotalJobs()			));
		logger.println(String.format( "%s%3d", "Total jobs started:     ", JobStatistics.getTotalJobsStarted()	));
		logger.println(String.format( "%s%3d", "Total jobs completed:   ", JobStatistics.getTotalJobsCompleted()));
		logger.println(String.format( "%s%3d", "Currently active jobs:  ", JobStatistics.getActiveJobs()		));
		logger.println(String.format( "%s%3d", "Total auctions:			", JobStatistics.getTotalAuctions()		));
		logger.println(String.format( "%s%3d", "Auctions active:		", JobStatistics.getAuctionsActive()	));
		logger.println(String.format( "%s%3d", "Bids:					", JobStatistics.getAuctionsBidOn()		));
		logger.println(String.format( "%s%3d", "Auctions won:			", JobStatistics.getAuctionsWon()		));
		logger.println(String.format( "%s%3d", "Auctions started:		", JobStatistics.getAuctionsStarted()	));
		logger.println(String.format( "%s%3d", "Auctions completed:		", JobStatistics.getAuctionsCompleted()	));
		logger.println(String.format( "%s%3d", "Auctions failed:		", JobStatistics.getAuctionsFailed()	));
		logger.println(String.format( "%s%3d", "Total missions:			", JobStatistics.getTotalMissions()		));
		logger.println(String.format( "%s%3d", "Missions started:		", JobStatistics.getMissionsStarted()	));
		logger.println(String.format( "%s%3d", "Missions completed:		", JobStatistics.getMissionsCompleted()	));
		logger.println(String.format( "%s%3d", "Missions failed:		", JobStatistics.getMissionsFailed()	));
		
		logger.printSeparator();
		logger.println();
	}
	
	public static void printJobStepStats() 
	{
		Logger logger = StatsLogger.get();
		
		Map<Job, Integer> jobStepEstimate = JobStatistics.getJobStepEstimate();
		Map<Job, Integer> jobStepStarted  = JobStatistics.getJobStepStarted();
		Map<Job, Integer> jobStepComplete = JobStatistics.getJobStepComplete();
		
		List<Job> sortedJobs = new ArrayList<>(jobStepComplete.keySet());
		Collections.sort(sortedJobs, Comparator.comparingInt(Job::getNumber));
		
		logger.println(String.format("%-6s%6s%6s%6s%6s", "Job", "Start", "Done", "Used", "Est"));
		
		for (Job job : sortedJobs)
		{
			int started  = jobStepStarted .get(job);
			int complete = jobStepComplete.get(job);
			int estimate = jobStepEstimate.get(job);
			
			logger.println(String.format("%-6s%6d%6d%6d%6d", job, started, complete, complete - started, estimate));
		}
		
		logger.printSeparator();
		logger.println();
	}
	
	public static void printAgentInventoryStats()
	{
		Logger logger = StatsLogger.get();
		
		List<AgentInfo> sortedAgents = new ArrayList<>(AgentInfo.get());
		Collections.sort(sortedAgents, Comparator.comparingInt(AgentInfo::getNumber));
		
		logger.println(String.format("%-8s%6s%6s %s", "Agent", "Load", "Max", "Inventory"));
		
		for (AgentInfo agent : sortedAgents)
		{
			logger.println(String.format("%-8s%6d%6d %s", agent, agent.getLoad(), agent.getRole().getLoad(), agent.getInventory()));
		}
		
		logger.printSeparator();
		logger.println();
	}
}
