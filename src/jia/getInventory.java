package jia;

import java.util.Map;

import env.Translator;
import info.AgentArtifact;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;

public class getInventory extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] terms) throws Exception {
		
		String agentName = (String) Translator.termToObject(terms[0]);

		Map<String, Integer> inventory = AgentArtifact.getAgentInventory(agentName);
		
		ListTerm list = ASSyntax.createList();
		
		inventory.entrySet().forEach(e -> list.add(ASSyntax.createLiteral("map", 
													ASSyntax.createAtom(e.getKey()),
													ASSyntax.createNumber(e.getValue()))));
		
		return un.unifies(terms[1], list);
	}

}
