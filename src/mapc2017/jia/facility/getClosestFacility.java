package mapc2017.jia.facility;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import mapc2017.data.facility.Facility;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.StaticInfo;
import mapc2017.env.parse.ASLParser;

public class getClosestFacility extends DefaultInternalAction {

	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) 
	{
		int i = 0;
		
		String from 	= ASLParser.parseFunctor(args[i++]);
		String type 	= ASLParser.parseString	(args[i++]);
		
		FacilityInfo 	fInfo = FacilityInfo.get();
		StaticInfo		sInfo = StaticInfo	.get();
		
		Collection<? extends Facility> facilities = type.equals("chargingStation") ?
				fInfo.getActiveChargingStations() : fInfo.getFacilities(type);
		
		Optional<? extends Facility> opt;
				
		synchronized (facilities) {
			opt = from.startsWith("agent") ?
				facilities.stream().min(Comparator.comparingInt(f -> sInfo
						.getRouteDuration(AgentInfo.get(from), f.getLocation()))) :
				facilities.stream().min(Comparator.comparingInt(f -> sInfo
						.getRouteLength(FacilityInfo.get().getFacility(from).getLocation(), f.getLocation())));
		}
		
		if (!opt.isPresent()) return false;
		
		return un.unifies(args[i], ASSyntax.createString(opt.get().getName()));
	}

}
