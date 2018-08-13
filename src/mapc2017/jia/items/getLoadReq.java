package mapc2017.jia.items;

import java.util.Map;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.data.item.Item;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.parse.ASLParser;

public class getLoadReq extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{		
		int i = 0;
		
		Map<String, Integer> items 	 = ASLParser.parseMap(args[i++]);		
		int 				 loadReq = items.keySet().stream()
										.map(ItemInfo.get()::getItem)
										.mapToInt(Item::getReqBaseVolume)
										.max().getAsInt();
		
		return un.unifies(args[i], ASSyntax.createNumber(loadReq));
	}

}
