package jia;

import java.util.Map;
import java.util.Map.Entry;

import env.Translator;
import info.AgentArtifact;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class hasItems extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] terms) throws Exception {
		
		String agentName = (String) Translator.termToObject(terms[0]);
		
		@SuppressWarnings("unchecked")
		Map<String, Integer> items = (Map<String, Integer>) Translator.termToObject(terms[1]);
		
		if (items.isEmpty()) return true;
		
		Map<String, Integer> inventory = AgentArtifact.getAgentInventory(agentName);
		
		if (inventory.isEmpty()) return false;
		
		for (Entry<String, Integer> item : items.entrySet())
		{
			Integer hasAmount = inventory.get(item.getKey());
			
			if (hasAmount == null) return false;
			
			if (hasAmount < item.getValue()) return false;
		}

		return true;
	}

}
