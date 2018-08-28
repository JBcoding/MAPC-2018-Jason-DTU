package mapc2017.jia.facility;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.StaticInfo;
import mapc2017.env.parse.ASLParser;

public class getDurationToFacility extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;

		String agent	= ASLParser.parseFunctor(args[i++]);
		String facility = ASLParser.parseString	(args[i++]);		
		int    duration = StaticInfo.get().getRouteDuration(AgentInfo.get(agent), 
				FacilityInfo.get().getFacility(facility).getLocation());
		
		return un.unifies(args[i], ASSyntax.createNumber(duration));
	}
}
