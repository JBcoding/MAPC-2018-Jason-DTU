package jia;

import java.util.Map;
import java.util.Map.Entry;

import env.Translator;
import info.AgentArtifact;
import info.ItemArtifact;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import massim.scenario.city.data.Item;

public class hasBaseItems extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] terms) throws Exception {
		
		String agentName = (String) Translator.termToObject(terms[0]);
		
		@SuppressWarnings("unchecked")
		Map<String, Integer> map = (Map<String, Integer>) Translator.termToObject(terms[1]);
		
		Map<Item, Integer> items = ItemArtifact.getBaseItems(ItemArtifact.getItemMap(map));
		
		if (items.isEmpty()) return true;
		
		Map<String, Integer> inventory = AgentArtifact.getAgentInventory(agentName);
		
		if (inventory.isEmpty()) return false;
		
		for (Entry<Item, Integer> item : items.entrySet())
		{
			Integer hasAmount = inventory.get(item.getKey().getName());
			
			if (hasAmount == null) return false;
			
			if (hasAmount < item.getValue()) return false;
		}

		return true;
	}

}
