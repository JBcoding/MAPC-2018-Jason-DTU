package mapc2017.jia.facility;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.parse.ASLParser;
import massim.scenario.city.data.Location;

public class getFacilityLocation extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;
		
		String facility = ASLParser.parseString(args[i++]);
		
		Location location = FacilityInfo.get().getFacility(facility).getLocation();
		
		return un.unifies(args[i++], ASSyntax.createNumber(location.getLat())) 
			&& un.unifies(args[i++], ASSyntax.createNumber(location.getLon()));
	}
}
