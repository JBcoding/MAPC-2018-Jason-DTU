package mapc2017.env.job;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cartago.AgentId;
import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import mapc2017.data.facility.Shop;
import mapc2017.data.facility.Storage;
import mapc2017.data.item.Item;
import mapc2017.data.item.ItemList;
import mapc2017.data.item.ShoppingList;
import mapc2017.data.job.AuctionJob;
import mapc2017.data.job.Job;
import mapc2017.data.job.MissionJob;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.DynamicInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.info.StaticInfo;

public class JobDelegator extends Artifact implements Runnable {
	
	private static JobDelegator instance;	
	public  static JobDelegator get() { return instance; }
	
	private Map<AgentInfo, AgentId> agentIds 	= new HashMap<>();
	private LinkedList<AgentInfo> 	freeAgents 	= new LinkedList<>();
	
	private Map<String, Set<AgentInfo>>	taskToAgents	= new HashMap<>();
	private Map<AgentInfo, String> 		agentToTask 	= new HashMap<>();

	private DynamicInfo 	dInfo;
	private FacilityInfo 	fInfo;
	private ItemInfo		iInfo;
	private StaticInfo 		sInfo;
	private JobEvaluator	evaluator;
	
	void init() 
	{
		instance = this;
		
		dInfo 		= DynamicInfo	.get();
		fInfo 		= FacilityInfo	.get();
		iInfo 		= ItemInfo		.get();
		sInfo 		= StaticInfo	.get();
		evaluator 	= JobEvaluator	.get();
	}
	
	public void run()
	{
		LinkedList<JobEvaluation> evals = evaluator.getEvaluations();
		
		if (evals.isEmpty() || freeAgents.isEmpty()) return;
		
		Collections.sort(evals, Comparator.comparingInt(JobEvaluation::getValue).reversed());
		
		synchronized (freeAgents) 
		{
			// Removes duplicates
			Set<AgentInfo> 	distinctAgents 	= new HashSet<>(freeAgents);			
							freeAgents 		= new LinkedList<>(distinctAgents);
			// Sort agents by capacity
			Collections.sort(freeAgents, Comparator.comparingInt(AgentInfo::getCapacity));	
			System.out.println("[JobDelegator] Free: " + freeAgents.size());		
		}
		
		int maxSteps 	= sInfo.getSteps();
		int currentStep = dInfo.getStep();
		
//		Iterator<JobEvaluation> it = evals.iterator();
		
		for (JobEvaluation eval : evals)
		{			
			Job	job	 = eval.getJob();
			
			int stepComplete = eval.getSteps() + currentStep;

			if (stepComplete < maxSteps && stepComplete < job.getEnd())
			{
				if (eval.getReqAgents() > freeAgents.size()) continue;
				
				 	 if (job instanceof MissionJob) { if (!delegate(eval)) continue; }
				else if (job instanceof AuctionJob) 
				{
					AuctionJob auction = (AuctionJob) job;
					
					if (auction.hasWon())
					{
//						JobStatistics.auctionWon(auction);
						System.out.println("Won auction");
						if (!delegate(eval)) return;
					}
					else if (auction.getReward() > 10000) {
						// Too difficult to delegate at this point
					}
					else if ((auction.isLastStep() || !auction.isHighestBidder()) 
							&& eval.getReqAgents() <= freeAgents.size())
					{
//						if (!iInfo.isItemsAvailable(eval.getBaseItems())) continue;
												
						AgentInfo agent;
						
						synchronized (freeAgents) {
							agent = freeAgents.removeFirst();
						}
						
						agentToTask.put(agent, "bid");
						
						execInternalOp("bidForAuction", agent, auction);
						
						continue;
					}
					else continue;
				}
				else 
				{
//					if (!iInfo.isItemsAvailable(eval.getBaseItems())) continue;
					
					if (!delegate(eval)) continue;
				}
			}
//			JobStatistics.addJobEvaluation(eval);
			evaluator.removeEvaluation(eval);
		}
	}
	
	private boolean delegate(JobEvaluation eval) 
	{		
		// Prevent delegating job twice with the same agents
		if (eval.getNrAgents() == freeAgents.size()) return false;
		if (freeAgents.size() < 28)	eval.setNrAgents(freeAgents.size());
		
		Job job = eval.getJob();
		
		// Make a copy of freeAgents to prevent removing agents before assigning them
		LinkedList<AgentInfo> 			agents 		= new LinkedList<>();
		// To prevent ArrayOutOfBounds addAll
		synchronized (freeAgents) {	agents.addAll(freeAgents); }
		// Maps agents to their workload
		Map<AgentInfo, ItemList> 		assemblers 	= new HashMap<>();
		Map<AgentInfo, ShoppingList> 	retrievers 	= new HashMap<>();
		// Maps retrievers to the name of the assembler
		Map<AgentInfo, String>			assistants 	= new HashMap<>();		
		
		ItemList itemsToAssemble = job.getItems();
		
//		if (itemsToAssemble.size() > 1) {
//			String item = itemsToAssemble.keySet().stream().findAny().get();
//			itemsToAssemble.remove(item);
//		}
		
		while (!itemsToAssemble.isEmpty())
		{
			if (agents.isEmpty()) return false;
			
			List<AgentInfo> usedAgents = new LinkedList<>();

			for (AgentInfo agent : agents)
			{				
				int beforeAmount = itemsToAssemble.getTotalAmount();
				
				itemsToAssemble.subtract(agent.getInventory());
				
				int afterAmount	= itemsToAssemble.getTotalAmount();
				
				if (afterAmount < beforeAmount) {
					usedAgents.add(agent);
					retrievers.put(agent, new ShoppingList());
					assemblers.put(agent, new ItemList()); 
				}
			}
			
			usedAgents.stream().forEach(agents::remove);
			
			// Find best suited agent to assemble items based on their volume
			AgentInfo assembler = getAssembler(agents, iInfo.getBaseVolume(itemsToAssemble));
			
//			agents.remove(assembler);
			usedAgents.add(assembler);
			retrievers.put(assembler, new ShoppingList());
			
			// Find actual items to deliver
			ItemList toAssemble = assembler.getItemsToCarry(itemsToAssemble);
			
			if (toAssemble.isEmpty()) return false;
			
			itemsToAssemble.subtract(toAssemble);
			
			assemblers.put(assembler, toAssemble);
			
			// Create shopping list for the given items
			ShoppingList 	shoppingList  = ShoppingList.getShoppingList(toAssemble);
			
//			String 			assemblerShop = getShop(assembler, shoppingList);
//			
//			ItemList assemblerRetrieve = assembler.getItemsToCarry(shoppingList.get(assemblerShop));
//			
//			shoppingList.get(assemblerShop).subtract(assemblerRetrieve);
//			
//			retrievers.put(assembler, new ShoppingList(assemblerShop, assemblerRetrieve));

			for (AgentInfo agent : agents)
			{
				ItemList inventory = agent.getInventory();
				
				for (String shop : shoppingList.keySet())
				{					
					if (inventory.isEmpty()) break;
					
					ItemList itemsToRetrieve = shoppingList.get(shop);
					
					if (itemsToRetrieve.isEmpty()) continue;
					
					int beforeAmount = itemsToRetrieve.getTotalAmount();
					
					ItemList temp = new ItemList(inventory);
					inventory.subtract(itemsToRetrieve);
					itemsToRetrieve.subtract(temp);
					
					int afterAmount	= itemsToRetrieve.getTotalAmount();
					
					if (afterAmount < beforeAmount) {
						usedAgents.add(agent);
						retrievers.put(agent, new ShoppingList("shop0", agent.getInventory()));
						assistants.put(agent, assembler.getName());
					}
				}
			}

			for (String shop : shoppingList.keySet())
			{
				ItemList itemsToRetrieve = shoppingList.get(shop);
				
				while (!itemsToRetrieve.isEmpty())
				{
					if (agents.isEmpty()) return false;

					// Find best suited agent to retrieve items
					AgentInfo retriever = getRetriever(agents, shop, itemsToRetrieve);
					
					agents.remove(retriever);
					
					ItemList toRetrieve = retriever.getItemsToCarry(itemsToRetrieve);
					
					if (toRetrieve.isEmpty()) continue;
					
					itemsToRetrieve.subtract(toRetrieve);
					
					retrievers.put(retriever, new ShoppingList(shop, toRetrieve, retriever.getInventory()));
					
					if (!retriever.equals(assembler)) assistants.put(retriever, assembler.getName());
				}
			}
			
			// Make sure used agents are removed from available agents
			for (AgentInfo agent : usedAgents) agents.remove(agent);
		}
		synchronized (freeAgents) {
			retrievers.keySet().stream().forEach(freeAgents::remove);			
		}
		retrievers.keySet().stream().forEach(agent -> agentToTask.put(agent, job.getId()));
		retrievers.values().stream().forEach(shoppingList -> shoppingList.entrySet().forEach(entry -> {
			Shop shop = (Shop) fInfo.getFacility(entry.getKey());
			for (Entry<String, Integer> item : entry.getValue().entrySet())
				shop.addReserved(item.getKey(), item.getValue());
		}));
		
		taskToAgents.put(job.getId(), retrievers.keySet());
		
		execInternalOp("assign", assemblers, retrievers, assistants, job, eval);
		
//		JobStatistics.startJob(job, dInfo.getStep());
		
		return true;
	}
	
	private AgentInfo getAssembler(LinkedList<AgentInfo> agents, int volume) {
		for (AgentInfo agent : agents)
			if (volume < agent.getCapacity())
				return agent;
		return agents.getLast();
	}
	
//	private String getShop(AgentInfo agent, ShoppingList shoppingList) {
//		return shoppingList.keySet().stream().findAny().get();
//	}

//	private String getShop(AgentInfo agent, ShoppingList shoppingList) {
//		return shoppingList.entrySet().stream().max(Comparator.comparingInt(e -> agent.getVolumeToCarry(iInfo.stringToItemMap(e.getValue()))
//		{
//			int steps = sInfo.getRouteDuration(agent, fInfo.getFacility(e.getKey()).getLocation());
//			int volume = agent.getVolumeToCarry(iInfo.stringToItemMap(e.getValue()));
//			return volume - steps;
//		}
//		)).get().getKey();
//	}
	
//	private AgentInfo getRetriever(LinkedList<AgentInfo> agents, String shop, Map<String, Integer> items) {
//		return agents.stream().findAny().get();
//	}
	
	private AgentInfo getRetriever(LinkedList<AgentInfo> agents, String shop, Map<String, Integer> items) {
//		Facility facility = fInfo.getFacility(shop);
		Map<Item, Integer> itemMap = iInfo.stringToItemMap(items);
		return agents.stream().max(Comparator.comparingInt(agent -> agent.getVolumeToCarry(itemMap)	
//		{
//			int steps = sInfo.getRouteDuration(agent, facility.getLocation());
//			int volume = agent.getVolumeToCarry(itemMap);
//			return volume - steps;
//		}
		)).get();
	}
	
	@OPERATION
	void free()
	{
		AgentInfo agent = AgentInfo.get(getOpUserName());
		
		synchronized (freeAgents) { freeAgents.add(agent); }
		
		agentToTask.put(agent, "");
		
		agentIds.put(agent, getOpUserId());
	}
	
	void assign(AgentInfo agent, Object... args)
	{
//		ErrorLogger.get().println(String.format("%s task(%s)", agent, Arrays.toString(args)));
		
		signal(agentIds.get(agent), "task", args);
	}
	
	@INTERNAL_OPERATION
	void assign(Map<AgentInfo, ItemList> assemblers, Map<AgentInfo, ShoppingList> retrievers, Map<AgentInfo, String> assistants, Job job, JobEvaluation eval)
	{
		for (AgentInfo agent : assemblers.keySet())
			assign(agent, job.getId(), assemblers.get(agent), job.getStorage(), retrievers.get(agent), eval.getWorkshop());

		for (AgentInfo agent : assistants.keySet())
			assign(agent, assistants.get(agent), retrievers.get(agent), eval.getWorkshop());
	}
	
	@INTERNAL_OPERATION
	void release(Job job)
	{
		String jobId = job.getId();
		
		if (taskToAgents.get(jobId) == null) return;
		
		for (AgentInfo agent : taskToAgents.get(jobId))
		{
			if (agentToTask.get(agent).equals(jobId)) 
			{
				assign(agent, "release");
			}
		}
	}
	
	public void releaseAgents(Job job) {
		execInternalOp("release", job);
	}
	
	@INTERNAL_OPERATION
	void bidForAuction(AgentInfo agent, AuctionJob auction) 
	{
		int bid = auction.getReward() - 2;
		
		assign(agent, auction.getId(), bid);
		
//		JobStatistics.bidOnAuction(auction);
	}
	
	public void retrieveDelivered(Storage storage) {		
		execInternalOp("retrieve", storage);
	}

	@INTERNAL_OPERATION
	void retrieve(Storage storage) 
	{	
		synchronized (freeAgents) 
		{			
			if (freeAgents.isEmpty()) return;
			
			if (storage.isRetrieving()) return;
			
			AgentInfo agent = freeAgents.getLast();
			
			if (agent.getCapacity() < iInfo.getVolume(storage.getDelivered())) return;
			
			freeAgents.remove(agent);
			
			storage.setRetrieving(true);
			
			assign(agent, storage.getName());
		}
	}
}
