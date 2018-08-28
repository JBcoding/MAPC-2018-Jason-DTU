package mapc2017.jia.facility;

import java.util.Set;
import java.util.stream.Collectors;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.data.facility.Shop;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.parse.ASLParser;

public class getAlternativeShop extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;
		
		String 	shop 	= ASLParser.parseString	(args[i++]);
		String 	item 	= ASLParser.parseString	(args[i++]);
		int		amount 	= ASLParser.parseInt	(args[i++]);
				
		Set<String> alternativeShops = ItemInfo.get().getItemLocations(item).stream()
											.filter(s -> s.getAmount(item) >= amount)
											.map(Shop::getName)
											.collect(Collectors.toSet());
		
		alternativeShops.remove(shop);
		
		if (alternativeShops.isEmpty()) return false;
		
		return un.unifies(args[i], ASSyntax.createString(alternativeShops.iterator().next()));
	}

}
