package info;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import cnp.TaskArtifact;
import data.CUtil;
import eis.iilang.Percept;
import env.EIArtifact;
import env.Translator;
import massim.scenario.city.data.*;
import massim.scenario.city.data.facilities.Storage;

public class JobArtifact extends Artifact {
	
	private static final Logger logger = Logger.getLogger(JobArtifact.class.getName());

	private static final String AUCTION = "auction";
	private static final String JOB 	= "job";
	private static final String MISSION = "mission";
	private static final String POSTED 	= "posted";
	
	public static final Set<String>	PERCEPTS = Collections.unmodifiableSet(
		new HashSet<String>(Arrays.asList(AUCTION, JOB, MISSION, POSTED)));

	private static Map<String, AuctionJob> 	auctions 		= new HashMap<>();
	private static Map<String, Job> 		jobs 			= new HashMap<>();
	private static Map<String, Job> 		missions 		= new HashMap<>();
	private static Map<String, Job> 		postedJobs 		= new HashMap<>();
	private static Map<String, String>		toBeAnnounced	= new HashMap<>();
	
	@OPERATION
	void getJob(String jobId, OpFeedbackParam<String> storage, 
			OpFeedbackParam<Object> items)
	{
		Job job = getJob(jobId);
		
		storage.set(job.getStorage().getName());

		items.set(CUtil.extractItems(job));
	}
	
	@OPERATION
	void getAuctionBid(String auctionId, OpFeedbackParam<Integer> bid)
	{
		AuctionJob auction = auctions.get(auctionId);
		
		if (auction.getLowestBid() != null)
		{
			bid.set(auction.getLowestBid().intValue() - 1);
		}
		else 
		{
			bid.set(Integer.MAX_VALUE);
		}
	}
	
	@OPERATION
	void completeJob(String jobId)
	{
		activeJobs.remove(jobId);
	}
	
	/**
	 * Computes the price to by all the necessary items needed to complete this job
	 * @param job
	 * @return The price to buy all the items needed for the job
	 */
	public static int priceForItems(Job job)
	{
		int price = 0;
		return price;
	}
	
	/**
	 * @param job
	 * @return The possible amount of money one can earn from this job
	 */
	public static int possibleEarning(Job job)
	{
		return job.getReward() - priceForItems(job);
	}
	
	/**
	 * @param job 
	 * @return The estimate of how many steps it is going to take to complete this job
	 */
	public static int estimateSteps(Job job)
	{
		return 1;
	}
	
	@OPERATION
	void getBid(String taskId, OpFeedbackParam<Integer> bid)
	{
		AuctionJob auction = auctions.get(taskId);
		
		if (auction.getLowestBid() == 0)
		{
			bid.set(auction.getReward());
		}
		else if (priceForItems(auction) < auction.getLowestBid() - 1)
		{
			bid.set(auction.getLowestBid() - 1);
		}
		else
		{
			bid.set(0);
		}
	}
	
	public static void perceiveUpdate(Collection<Percept> percepts)
	{
		
		for (Percept percept : percepts)
		{
			switch (percept.getName())
			{
			case AUCTION: perceiveAuction	(percept); break;
			case JOB:     perceiveJob		(percept); break;
			case MISSION: perceiveMission	(percept); break;
			case POSTED:  perceivePosted	(percept); break;
			}
		}

		if (EIArtifact.LOGGING_ENABLED)
		{
			logger.info("Perceived jobs");
			logJobs("Auctions perceived:"	, auctions	.values());
			logJobs("Jobs perceived:"		, jobs		.values());
			logJobs("Missions perceived:"	, missions	.values());
			logJobs("Posted jobs perceived:", postedJobs.values());
		}		
	}
	
	private static void logJobs(String msg, Collection<? extends Job> jobs)
	{
		if (jobs.size() != 0)
			logger.info(msg);
			for (Job job : jobs)
				logger.info(job.toString());
	}
	
	private static void perceiveAuction(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	id 			= (String) args[0];
		String 	storageId	= (String) args[1];
		int 	reward 		= (int)    args[2];
		int 	start 		= (int)    args[3];
		int 	end 		= (int)    args[4];
		int 	fine		= (int)    args[5];
		int 	bid 		= (int)    args[6];
		int 	time		= (int)    args[7];
		Object[] itemsToBuild = (Object[]) args[8];

        ItemBox itemBox = new ItemBox();
        for (Object tuple : itemsToBuild) {
        	Object[] tup = (Object[])tuple;
            itemBox.store(ItemArtifact.getItem((String)tup[0]), (int) tup[1]);
        }
		
		Storage storage = (Storage) FacilityArtifact.getFacility(FacilityArtifact.STORAGE, storageId);
		
		AuctionJob auction = new AuctionJob(reward, storage, start, end, itemBox, time, fine);
		
		auction.bid(0,null, bid);
		
		if (auction.getBeginStep() + auction.getAuctionTime() > DynamicInfoArtifact.getStep())
		{
			if (!auctions.containsKey(id))
				toBeAnnounced.put(id, "auction");		
			
			auctions.put(id, auction);
		}
		else 
		{
			// The team has won, and it can be considered a normal job
			if (!missions.containsKey(id))
				toBeAnnounced.put(id, "mission");

			missions.put(id, auction);
		}

	}
	
	private static void perceiveJob(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	id 			= (String) args[0];
		String 	storageId	= (String) args[1];
		int 	reward 		= (int)    args[2];
		int 	start 		= (int)    args[3];
		int 	end 		= (int)    args[4];

		ItemBox itemBox = new ItemBox();
		for (Object part : (Object[]) args[5])
        {
            Object[] partArgs = (Object[]) part;

            String 	itemId   = (String) partArgs[0];
            int    	quantity = (int)    partArgs[1];

            itemBox.store(ItemArtifact.getItem(itemId), quantity);
        }

        Storage storage = (Storage) FacilityArtifact.getFacility(FacilityArtifact.STORAGE, storageId);
		
		Job job = new Job(reward, storage, start, end, itemBox, "");


		
		if (!jobs.containsKey(id))
			toBeAnnounced.put(id, "job");
		
		jobs.put(id, job); 		
	}
	
	private static void perceiveMission(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	id 			= (String) args[0];
		String 	storageId	= (String) args[1];
		int 	reward 		= (int)    args[2];
		int 	start 		= (int)    args[3];
		int 	end 		= (int)    args[4];
		int 	fine		= (int)    args[5];
		int 	bid 		= (int)    args[6];
		int 	time		= (int)    args[7]; // TODO: This is the same as auction, but what is its purpose here?
        Object[] itemsToBuild = (Object[]) args[8];

		ItemBox itemBox = new ItemBox();
        for (Object tuple : itemsToBuild) {
            Object[] tup = (Object[])tuple;
            itemBox.store(ItemArtifact.getItem((String)tup[0]), (int) tup[1]);
        }

		Storage storage = (Storage) FacilityArtifact.getFacility(FacilityArtifact.STORAGE, storageId);
		
		Mission mission = new Mission(reward, storage, start, end, fine, itemBox, null, id);
		
		mission.bid(0,null, bid);
		
		if (!missions.containsKey(id))
			toBeAnnounced.put(id, "mission");	
		
		missions.put(id, mission);
		
	}
	
	private static void perceivePosted(Percept percept)
	{
		Object[] args = Translator.perceptToObject(percept);
		
		String 	id 			= (String) args[0];
		String 	storageId	= (String) args[1];
		int 	reward 		= (int)    args[2];
		int 	start 		= (int)    args[3];
		int 	end 		= (int)    args[4];

		ItemBox itemBox = new ItemBox();
		for (Object part : (Object[]) args[5])
        {
            Object[] partArgs = (Object[]) part;

            String 	itemId   = (String) partArgs[0];
            int    	quantity = (int)    partArgs[1];

            itemBox.store(ItemArtifact.getItem(itemId), quantity);
        }

		Storage storage = (Storage) FacilityArtifact.getFacility(FacilityArtifact.STORAGE, storageId);
		
		Job job = new Job(reward, storage, start, end, itemBox, "");



		if (!postedJobs.containsKey(id))
			toBeAnnounced.put(id, "postedJob");
		
		postedJobs.put(id, job);
	}
	
	public static Job getJob(String taskId)
	{
		if (jobs.containsKey(taskId))
		{
			return jobs.get(taskId);
		}
		else if (missions.containsKey(taskId))
		{
			return missions.get(taskId);
		}
		else if (auctions.containsKey(taskId))
		{
			return auctions.get(taskId);
		}
		else
		{
			return postedJobs.get(taskId);			
		}
	}

	/**
	 * @return All the current jobs
	 */
	public static Collection<Job> getJobs() 
	{
		return jobs.values();
	}
	
	public static Map<Job, Integer> getJobsAndEarnings()
	{
		return jobs.values().stream()
				.collect(Collectors.toMap(x -> x, x -> possibleEarning(x)));
	}
	
	private static final int jobThreshold = 3000;
	private static Map<String, String> activeJobs = new HashMap<>();
	
	public static void announceJobs()
	{
		for (Entry<String, String> entry: toBeAnnounced.entrySet())
		{
			Job job = getJob(entry.getKey());
			
			int earning 	= possibleEarning(job);
			int duration 	= estimateSteps(job);
			int ratio		= earning / duration;

			logger.info(entry.getKey() + " can earn " + earning + " in " + duration + " steps. Ratio: " + earning / duration + " - Type: " + entry.getValue());
			
			if (ratio >= 30 || (activeJobs.size() < 3 && ratio > 20) || entry.getValue().equals("mission"))
			{
				// If it is an auction, consider bidding on it.
				if (entry.getValue().equals("auction")) 					
				{
					if (earning < ((AuctionJob) job).getLowestBid() || activeJobs.size() > 5) continue;
				}
				
				activeJobs.put(entry.getKey(), entry.getValue());
				
				logger.info("Added " + entry.getKey() + " Reward: " + job.getReward() + ". Number of jobs: " + activeJobs.size());
				
				TaskArtifact.announceJob(entry.getKey(), entry.getValue());
			}
		}

		toBeAnnounced.clear();
	}

	public static void reset() 
	{
		auctions 		= new HashMap<>(); 
		jobs 			= new HashMap<>(); 
		missions 		= new HashMap<>(); 
		postedJobs 		= new HashMap<>(); 
		toBeAnnounced	= new HashMap<>(); 
		activeJobs 		= new HashMap<>();
	}
}
