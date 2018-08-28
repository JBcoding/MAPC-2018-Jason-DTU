package mapc2017.jia.items;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import mapc2017.data.facility.Shop;
import mapc2017.data.item.ItemList;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.parse.ASLParser;

public class getLeastAvailableItems extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args)
	{
		int i = 0;
		
		String agentName = ASLParser.parseFunctor(args[i++]);
		String shopName  = ASLParser.parseString (args[i++]);
		
		AgentInfo agent = AgentInfo.get(agentName);
		
		Shop shop = (Shop) FacilityInfo.get().getFacility(shopName);
		
		ItemList items = ItemInfo.get().getLeastAvailableItems(agent, shop);
		
		if (items == null) return false;
		
		return un.unifies(args[i], ASLParser.createMap(items));
	}

}
