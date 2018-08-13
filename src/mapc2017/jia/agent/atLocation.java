package mapc2017.jia.agent;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.parse.ASLParser;
import massim.scenario.city.data.Location;

public class atLocation extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args)
	{
		int i = 0;
		
		String agent = ASLParser.parseFunctor(args[i++]);
		double lat 	 = ASLParser.parseDouble (args[i++]);
		double lon 	 = ASLParser.parseDouble (args[i++]);
		
		return AgentInfo.get(agent).getLocation().equals(new Location(lon, lat));
	}

}
