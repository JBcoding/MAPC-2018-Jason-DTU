package mapc2017.jia.items;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.parse.ASLParser;

public class getVolume extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;
		
		String 	item 	= ASLParser.parseString(args[i++]);
		int		volume 	= ItemInfo.get().getItem(item).getVolume();
		
		return un.unifies(args[i], ASSyntax.createNumber(volume));
	}
	
}
