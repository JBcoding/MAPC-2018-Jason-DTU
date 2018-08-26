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
import massim.protocol.scenario.city.data.ItemAmountData;
import massim.scenario.city.data.AuctionJob;
import massim.scenario.city.data.Item;
import massim.scenario.city.data.Job;
import massim.scenario.city.data.Mission;
import massim.scenario.city.data.Role;
import massim.scenario.city.data.Route;
import massim.scenario.city.data.facilities.Facility;
import massim.scenario.city.data.facilities.Shop;
import massim.scenario.city.data.facilities.Storage;
import massim.scenario.city.util.GraphHopperManager;

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
		for (ItemAmountData itemData : job.getRequiredItems().toItemAmountData())
		{
			int currentPrice = 0;
			
			for (Entry<Item, Integer> entry : ItemArtifact.getBaseItems(itemData.getName()).entrySet())
			{				
				currentPrice += ItemArtifact.itemPrice(entry.getKey()) * entry.getValue();
			}
			
			price += currentPrice * itemData.getAmount();
		}
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
		Map<Item, Integer> items = job.getRequiredItems()
				.toItemAmountData().stream()
				.collect(Collectors.toMap(x -> ItemArtifact.getItem(x.getName()), x -> x.getAmount()));
		Map<Shop, Map<Item, Integer>> shoppingList = ItemArtifact.getShoppingList(ItemArtifact.getBaseItems(items));
		
		// Use the car role to estimate
		Role role = StaticInfoArtifact.getRole("car");
		
		Set<String> permissions = new HashSet<>();
		permissions.add(GraphHopperManager.PERMISSION_ROAD);
		
		Shop lastShop = null;
		int length = 0;
		
		// Find distance between all shops
		for (Shop shop : shoppingList.keySet())
		{
			if (lastShop != null) 
			{
				Route route = StaticInfoArtifact.getMap().findRoute(lastShop.getLocation(), shop.getLocation(), permissions);
				length += route.getRouteLength();
			}
			lastShop = shop;
		}
		
		Facility workshop = FacilityArtifact.getFacilities("workshop").stream().findAny().get();
		length += StaticInfoArtifact.getMap().findRoute(lastShop.getLocation(), workshop.getLocation(), permissions).getRouteLength();
		
		length += StaticInfoArtifact.getMap().findRoute(workshop.getLocation(), job.getStorage().getLocation(), permissions).getRouteLength();
		
		int steps = length / role.getSpeed();
		
		return steps;
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
		
		Storage storage = (Storage) FacilityArtifact.getFacility(FacilityArtifact.STORAGE, storageId);
		
		AuctionJob auction = new AuctionJob(reward, storage, start, end, time, fine);
		
		auction.bid(null, bid);

		for (Object part : (Object[]) args[8])
		{
			Object[] partArgs = (Object[]) part;
			
			String 	itemId   = (String) partArgs[0];
			int    	quantity = (int)    partArgs[1];
			
			auction.addRequiredItem(ItemArtifact.getItem(itemId), quantity);
		}
		
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
		
		Storage storage = (Storage) FacilityArtifact.getFacility(FacilityArtifact.STORAGE, storageId);
		
		Job job = new Job(reward, storage, start, end, "");

		for (Object part : (Object[]) args[5])
		{
			Object[] partArgs = (Object[]) part;
			
			String 	itemId   = (String) partArgs[0];
			int    	quantity = (int)    partArgs[1];
			
			job.addRequiredItem(ItemArtifact.getItem(itemId), quantity);
		}
		
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
//		int 	time		= (int)    args[7];
		String	mId			= (String) args[8];
		
		Storage storage = (Storage) FacilityArtifact.getFacility(FacilityArtifact.STORAGE, storageId);
		
		Mission mission = new Mission(reward, storage, start, end, fine, null, mId);
		
		mission.bid(null, bid);

		for (Object part : (Object[]) args[9])
		{
			Object[] partArgs = (Object[]) part;
			
			String itemId   = (String) partArgs[0];
			int    quantity = (int)    partArgs[1];
			
			mission.addRequiredItem(ItemArtifact.getItem(itemId), quantity);
		}
		
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
		
		Storage storage = (Storage) FacilityArtifact.getFacility(FacilityArtifact.STORAGE, storageId);
		
		Job job = new Job(reward, storage, start, end, "");

		for (Object part : (Object[]) args[5])
		{
			Object[] partArgs = (Object[]) part;
			
			String 	itemId   = (String) partArgs[0];
			int    	quantity = (int)    partArgs[1];
			
			job.addRequiredItem(ItemArtifact.getItem(itemId), quantity);
		}

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
