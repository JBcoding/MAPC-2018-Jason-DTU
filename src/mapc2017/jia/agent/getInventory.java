package mapc2017.jia.agent;

import java.util.Map;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.parse.ASLParser;

public class getInventory extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args)
	{
		int i = 0;

		String 				 agent		= ASLParser.parseFunctor(args[i++]);
		Map<String, Integer> inventory 	= AgentInfo.get(agent).getInventory();
		
		return un.unifies(args[i], ASLParser.createMap(inventory));
	}

}
