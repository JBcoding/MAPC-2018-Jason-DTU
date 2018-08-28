package mapc2017.jia.util;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.env.info.StaticInfo;
import massim.scenario.city.data.Location;

public class getRandomCenterLocation extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;
		
		Location l = StaticInfo.get().getRandomCenterLocation();
		
		return un.unifies(args[i++], ASSyntax.createNumber(l.getLat())) 
			&& un.unifies(args[i++], ASSyntax.createNumber(l.getLon()));
	}

}
