package cnp;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import cartago.Artifact;
import cartago.ArtifactConfig;
import cartago.ArtifactId;
import cartago.OPERATION;
import env.Translator;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.parser.ParseException;
import massim.scenario.city.data.AuctionJob;
import massim.scenario.city.data.facilities.Shop;

public class TaskArtifact extends Artifact {

	private static final Logger logger = Logger.getLogger(TaskArtifact.class.getName());

	private static TaskArtifact instance;
	private static int 			cnpId;
	
	void init()
	{
		instance = this;
	}
	
	public static void announceJob(String taskId, String type) 
	{		
		instance.execInternalOp("announceJobOp", taskId, type); 
	}
	
	public static void announceAuction(String taskId, AuctionJob auction) 
	{
		instance.execInternalOp("announceAuction", taskId);		
	}
	
	public static void announceShops(Collection<Shop> shops)
	{
		Object shopNames = shops.stream().map(Shop::getName).toArray(String[]::new);
				
		instance.execInternalOp("announceShops", shopNames);
	}
	
	@OPERATION
	void announceJobOp(String taskId, String type)
	{
		instance.define("task", taskId, type);
	}
	
	@OPERATION
	void announceAuction(String taskId)
	{
		instance.announce("auction", taskId);
	}
	
	@OPERATION
	void announceShops(Object shops)
	{
		instance.announce("shops", shops);
	}
	
	@OPERATION
	void announceBuy(String item, String amount, String shop)
	{
		instance.announce("buyRequest", item, amount, shop);
	}
	
	@OPERATION
	void announceAssemble(Object items, String workshop, String taskId, String deliveryLocation, String type)
	{
		instance.announce("assembleRequest", items, workshop, taskId, deliveryLocation, type);
	}
	
	@OPERATION
	void announceRetrieve(String agent, Object shoppingList, String workshop)
	{
		instance.announce("retrieveRequest", agent, toItemMap(shoppingList), workshop);
	}
	
	private void define(String property, Object... args)
	{
		defineObsProperty(property, args);
	}
	

	private void announce(String property, Object... args)
	{
		try 
		{
			String cnpName = "CNPArtifact" + (++cnpId);
			
			ArtifactId id = makeArtifact(cnpName, "cnp.CNPArtifact", ArtifactConfig.DEFAULT_CONFIG);
			
			List<Object> properties = new LinkedList<Object>(Arrays.asList(args));
			
			properties.add(id);
			
			defineObsProperty(property, properties.toArray());
		} 
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in announce: " + e.getMessage(), e);
		}		
	}
	
	@OPERATION
	void clearTask(String taskId)
	{
		removeObsPropertyByTemplate("task", taskId, null); 
	}
	
	@OPERATION
	void clearShops(String cnpName)
	{
		removeObsPropertyByTemplate("shops", null, cnpName);
	}
	
	@OPERATION
	void clearAssemble(Object cnpId)
	{
		instance.clear("assembleRequest", null, null, null, null, null, cnpId);
	}
	
	@OPERATION
	void clearRetrieve(Object cnpId)
	{
		instance.clear("retrieveRequest", null, null, null, cnpId);
	}
	
	@OPERATION
	private void clear(String property, Object... args)
	{
		try 
		{
			ArtifactId cnpId = (ArtifactId) args[args.length - 1];
			
			execLinkedOp(cnpId, "disposeArtifact");
			
			removeObsPropertyByTemplate(property, args);			
		} 
		catch (Throwable e) 
		{
			logger.log(Level.SEVERE, "Failure in clear: " + e.getMessage(), e);
		}
	}
	
	private static Object toItemMap(Object items)
	{
		if (items instanceof Map<?, ?>) return items;
		else
		{
			try 
			{
				ListTerm terms = ASSyntax.createList();
				
				for (Object item : (Object[]) items)
				{
					terms.add(ASSyntax.parseLiteral((String) item));
				}
				
				return Translator.termToObject(terms);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
				return null;
			}
		}
	}
}
