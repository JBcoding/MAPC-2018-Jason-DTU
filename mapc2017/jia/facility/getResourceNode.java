package mapc2017.jia.facility;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import mapc2017.data.facility.ResourceNode;
import mapc2017.data.item.Item;
import mapc2017.env.info.AgentInfo;
import mapc2017.env.info.FacilityInfo;
import mapc2017.env.info.ItemInfo;
import mapc2017.env.info.StaticInfo;
import mapc2017.env.parse.ASLParser;

public class getResourceNode extends DefaultInternalAction {
	
	private static final long serialVersionUID = 1L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args)
	{
		int i = 0;
		
		String agent = ASLParser.parseFunctor(args[i++]);
		
		Set<ResourceNode> resourceNodes = FacilityInfo.get()
				.getFacilities(FacilityInfo.RESOURCE_NODE)
				.stream().map(ResourceNode.class::cast)
				.collect(Collectors.toSet());
		
		Optional<Item> item = resourceNodes.stream()
				.map(ResourceNode::getResource)
				.map(ItemInfo.get()::getItem)
				.min(Comparator.comparingDouble(Item::getAvailability));
		
		if (!item.isPresent()) return false;
		
		String resource = item.get().getName();
		
		Optional<ResourceNode> node = resourceNodes.stream()
				.filter(f -> f.getResource().equals(resource))
				.min(Comparator.comparingInt(f -> StaticInfo.get()
						.getRouteDuration(AgentInfo.get(agent), f.getLocation())));
		
		if (!node.isPresent()) return false;
		
		return un.unifies(args[i], ASSyntax.createString(node.get().getName()));
	}

}
