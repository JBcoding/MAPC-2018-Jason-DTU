package mapc2017.jia.agent;

import java.util.Map;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.parse.ASLParser;

public class hasTools extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;

		String 	 agent	= ASLParser.parseFunctor(args[i++]);
		String[] tools  = ASLParser.parseArray	(args[i++]);
		
		if (tools.length == 0) return true;
		
		Map<String, Integer> inventory = AgentInfo.get(agent).getInventory();

		if (inventory.isEmpty()) return false;
		
		for (Object tool : tools)
		{
			if (!inventory.containsKey(tool)) return false;
		}
		
		return true;
	}

}
