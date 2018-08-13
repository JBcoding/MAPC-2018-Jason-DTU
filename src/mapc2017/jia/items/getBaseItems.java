package mapc2017.jia.items;

import java.util.Map;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.parse.ASLParser;

public class getBaseItems extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;
		
		Map<String, Integer> items 		= ASLParser.parseMap(args[i++]);		
		Map<String, Integer> baseItems 	= ItemInfo.get().getBaseItems(items);
		
		return un.unifies(args[i], ASLParser.createMap(baseItems));
	}

}
