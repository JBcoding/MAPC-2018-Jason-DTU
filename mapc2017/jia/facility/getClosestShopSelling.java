package mapc2017.jia.facility;

import java.util.Comparator;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.info.StaticInfo;
import mapc2017.env.parse.ASLParser;

public class getClosestShopSelling extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;

		String agent 	= ASLParser.parseFunctor(args[i++]);
		String item	 	= ASLParser.parseString	(args[i++]);
		String shop  	= ItemInfo.get().getItemLocations(item).stream()
							.min(Comparator.comparingInt(f -> StaticInfo.get()
									.getRouteDuration(AgentInfo.get(agent), f.getLocation())))
							.get().getName();
		
		return un.unifies(args[i], ASSyntax.createString(shop));
	}

}
