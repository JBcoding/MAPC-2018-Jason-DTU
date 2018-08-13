package mapc2017.jia.facility;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import mapc2017.data.facility.Storage;
import mapc2017.data.item.ItemList;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.parse.ASLParser;

public class getDeliveredItems extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;
		
		String storageName = ASLParser.parseString(args[i++]);
		
		Storage storage = (Storage) FacilityInfo.get().getFacility(storageName);
		
		ItemList items = storage.getDelivered();
		
		return un.unifies(args[i], ASLParser.createMap(items));
	}

}
