package mapc2017.jia.facility;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.data.facility.Shop;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.parse.ASLParser;

public class getAvailableAmount extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;
		
		String 	shop 	= ASLParser.parseString(args[i++]);
		String 	item 	= ASLParser.parseString(args[i++]);
		int		amount	= ((Shop) FacilityInfo.get().getFacility(shop)).getAmount(item);
		
		return un.unifies(args[i], ASSyntax.createNumber(amount));
	}

}
