package mapc2017.env.job;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mapc2017.data.Role;
import mapc2017.data.facility.Facility;
import mapc2017.data.item.Item;
import mapc2017.data.item.ItemList;
import mapc2017.data.item.ShoppingList;
import mapc2017.data.job.Job;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.info.StaticInfo;
import massim.scenario.city.data.Location;

public class JobEvaluator {
	
	private static JobEvaluator instance;	
	public  static JobEvaluator get() { return instance; }
	
	private Set<JobEvaluation> 	evals 	= new HashSet<>();
	private LinkedList<Role> 	roles;
	
	private Collection<AgentInfo> 	aInfos;
	private FacilityInfo			fInfo;
	private ItemInfo				iInfo;
	private StaticInfo 				sInfo;
	
	private Location 	center;
	private int			avgSpeed;
	
	public JobEvaluator() 
	{
		instance = this;
		
		aInfos 	= AgentInfo		.get();
		fInfo 	= FacilityInfo	.get();
		iInfo	= ItemInfo		.get();
		sInfo 	= StaticInfo	.get();
	}
	
	public void init()
	{		
		roles = sInfo.getRoles().stream()
				.sorted(Comparator.comparingInt(Role::getLoad))
				.collect(Collectors.toCollection(LinkedList::new));
		
		center = sInfo.getCenter();
		
		avgSpeed 	= (int) aInfos.stream()
							.map(AgentInfo::getRole)
							.mapToInt(Role::getSpeed)
							.average().getAsDouble();
	}
	
	public void evaluate(Job job)
	{
		int price = job.getItems().entrySet().stream()
						.mapToInt(Item::getAvgPrice).sum();
		
		int profit = job.getReward() - price;
				
		int 			baseVolume 		= iInfo.getBaseVolume(job.getItems());
		ItemList		baseItems		= iInfo.getBaseItems(job.getItems());
		ShoppingList 	shoppingList 	= ShoppingList.getShoppingList(baseItems);
		
//		int baseItemCount	= baseItems.values().stream()
//								.mapToInt(Integer::intValue)
//								.sum();
//		
//		double avgItemAvail	= baseItems.entrySet().stream()
//								.mapToDouble(entry -> iInfo
//										.getItem(entry.getKey())
//										.getAvailability() * entry.getValue())
//								.sum() / baseItemCount;
//		
//		double difficulty	= (3 - avgItemAvail > 1) ? 3 - avgItemAvail : 1;
		
		int reqAssemblers	= getReqAgents(baseVolume);
		int reqAgents		= shoppingList.values().stream()
								.map(iInfo::getVolume)
								.mapToInt(this::getReqAgents)
								.sum();
		
		Facility workshop 	= fInfo.getFacilities(FacilityInfo.WORKSHOP).stream()
								.min(Comparator.comparingInt(f -> sInfo.getRouteLength(
									fInfo.getFacility(job.getStorage()).getLocation(), 
									f.getLocation()))).get();
		
		int maxDistance 	= shoppingList.keySet().stream().map(fInfo::getFacility)
								.mapToInt(shop -> sInfo.getRouteLength(center, shop.getLocation())
												+ sInfo.getRouteLength(shop.getLocation(), workshop.getLocation()))
								.max().getAsInt();
		
		int maxPurchases 	= shoppingList.values().stream().mapToInt(Map::size).max().getAsInt();
		
		int reqAssemblies 	= job.getItems().entrySet().stream()
								.mapToInt(e -> iInfo.getItem(e.getKey()).reqAssembly() ? e.getValue() : 0)
								.sum();
		
		int stepEstimate 	= (int) ((maxDistance / avgSpeed + maxPurchases + reqAssemblies / reqAssemblers) * 1.1);
		
		evals.add(new JobEvaluation(job, profit, stepEstimate, reqAgents, workshop.getName(), baseItems));
	}
	
	private int getReqAgents(int volume) {
		int agents = 0;		
		while (volume > 0) {
			Role role = getReqRole(volume);
			agents++;
			volume -= role.getLoad();
		}		
		return agents;
	}
	
	private Role getReqRole(int volume) {
		for (Role role : roles)
			if (volume <= role.getLoad())
				return role;		
		return roles.getLast();
	}
	
	public LinkedList<JobEvaluation> getEvaluations() {
		return new LinkedList<>(evals);
	}
	
	public synchronized void removeEvaluation(JobEvaluation eval) {
		evals.remove(eval);
	}
	
	public synchronized void removeEvaluation(Job job) 
	{
		Iterator<JobEvaluation> it = evals.iterator();
		
		while (it.hasNext())
		{
			JobEvaluation eval = it.next();
			
			if (eval.getJob().getId().equals(job.getId()))
			{
				it.remove();
				return;
			}
		}
	}
}
