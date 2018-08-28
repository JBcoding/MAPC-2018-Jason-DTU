package mapc2017.env.parse;

import java.util.*;
import java.util.Map.Entry;

import eis.iilang.*;
import mapc2017.data.*;
import mapc2017.data.facility.*;
import mapc2017.data.item.Item;
import mapc2017.data.item.Tool;
import mapc2017.data.job.*;

public class IILParser {
	
	/***********************/
	/** PARAMETER METHODS **/
	/***********************/
	
	////////////
	// IILANG //
	////////////
	
	public static Identifier parseIdentifier(Parameter p) {
		return (Identifier) p;
	}
	
	public static Numeral parseNumeral(Parameter p) {
		return (Numeral) p;
	}
	
	public static ParameterList parseParams(Parameter p) {
		return (ParameterList) p;
	}
	
	public static Function parseFunc(Parameter p) {
		return (Function) p;
	}
	
	public static ParameterList parseFuncParams(Parameter p) {
		return parseParams(parseFunc(p).getParameters().getFirst());
	}
	
	//////////////
	// JAVALANG //
	//////////////
	
	public static String parseString(Parameter p) {
		return parseIdentifier(p).getValue();
	}
	
	public static int parseInt(Parameter p) {
		return (int) parseNumeral(p).getValue();
	}
	
	public static long parseLong(Parameter p) {
		return (long) parseNumeral(p).getValue();
	}
	
	public static double parseDouble(Parameter p) {
		return (double) parseNumeral(p).getValue();
	}
	
	public static Set<String> parseSet(Parameter p) {
		Set<String> set = new HashSet<>();
		for (Parameter pm : parseParams(p)) {
			set.add(parseString(pm));
		}
		return set;
	}
	
	public static List<String> parseList(Parameter p) {
		List<String> list = new ArrayList<>();
		for (Parameter pm : parseParams(p)) {
			list.add(parseString(pm));
		}
		return list;
	}
	
	public static String[] parseArray(Parameter p) {
		List<String> list = new ArrayList<>();
		for (Parameter pm : parseParams(p)) {
			list.add(parseString(pm));
		}
		return list.toArray(new String[list.size()]);		
	}
	
	public static Set<String> parseSetFunc(Parameter p) {
		Set<String> set = new HashSet<>();
		for (Parameter pm : parseFuncParams(p)) {
			set.add(parseString(pm));
		}
		return set;
	}
	
	public static Map<String, Integer> parseMapFunc(Parameter p) {
		Map<String, Integer> map = new HashMap<>();
		for (Parameter pm : parseFuncParams(p)) {
			ParameterList 	entry 	= parseParams(pm);
			String 			key 	= parseString(entry.get(0));
			int 			value 	= parseInt(entry.get(1));
			map.put(key, value);
		}	
		return map;		
	}
	
	public static Map<String, Integer> parseMap1(Parameter p) {
		Map<String, Integer> map = new HashMap<>();
		for (Parameter pm : parseParams(p)) {
			List<Parameter> entry 	= parseFunc(pm).getParameters();
			String 			key 	= parseString(entry.get(0));
			int 			value 	= parseInt(entry.get(1));
			map.put(key, value);
		}	
		return map;
	}
	
	public static Map<String, Integer> parseMap2(Parameter p) {
		Map<String, Integer> map = new HashMap<>();
		for (Parameter pm : parseParams(p)) {
			List<Parameter> entry 	= parseFunc(pm).getParameters();
			String 			key 	= parseString(entry.get(0));
			int 			value 	= parseInt(entry.get(2));
			map.put(key, value);
		}	
		return map;
	}
	
	/*********************/
	/** PERCEPT METHODS **/
	/*********************/
	
	public static String parseString(Percept p) {
		return parseString(p.getParameters().getFirst());
	}
	
	public static int parseInt(Percept p) {
		return parseInt(p.getParameters().getFirst());
	}
	
	public static long parseLong(Percept p) {
		return parseLong(p.getParameters().getFirst());
	}
	
	public static double parseDouble(Percept p) {
		return parseDouble(p.getParameters().getFirst());
	}
	
	public static List<String> parseList(Percept p) {
		return parseList(p.getParameters().getFirst());
	}
	
	public static String[] parseArray(Percept p) {
		return parseArray(p.getParameters().getFirst());
	}
	
	/////////////
	// OBJECTS //
	/////////////
	
	public static Entry<String, Integer> parseEntry(Percept p) {
		List<Parameter> params 	= p.getParameters();
		String 			name 	= parseString(params.get(0));
		int				amount 	= parseInt(params.get(1));
		return new AbstractMap.SimpleEntry<String, Integer>(name, amount);
	}
	
	public static Role parseRole(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		String 					name 	= parseString	(params.get(0));
		int						speed 	= parseInt		(params.get(1));
		int						load	= parseInt		(params.get(2));
		int						battery = parseInt		(params.get(3));
		Set<String> 			tools 	= parseSet		(params.get(4));
		return new Role(name, speed, load, battery, tools);
	}
	
	public static Item parseItem(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		String 					name 	= parseString	(params.get(0));
		int						volume 	= parseInt		(params.get(1));
		if (name.startsWith("tool")) return new Tool(name, volume);
		Set<String> 			tools 	= parseSetFunc	(params.get(2));
		Map<String, Integer> 	parts 	= parseMapFunc	(params.get(3));
		return new Item(name, volume, tools, parts);
	}
	
	public static Entity parseEntity(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		String 					name 	= parseString	(params.get(0));
		String 					team 	= parseString	(params.get(1));
		double					lat 	= parseDouble	(params.get(2));
		double					lon 	= parseDouble	(params.get(3));
		String					role 	= parseString	(params.get(4));
		return new Entity(name, team, lat, lon, role);		
	}
	
	////////////////
	// FACILITIES //
	////////////////
	
	private static Facility parseFacility(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		String 					name 	= parseString	(params.get(0));
		double					lat 	= parseDouble	(params.get(1));
		double					lon 	= parseDouble	(params.get(2));
		return new Facility(name, lat, lon);
	}
	
	public static ChargingStation parseChargingStation(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		int						rate	= parseInt		(params.get(3));
		return new ChargingStation(parseFacility(p), rate);
	}
	
	public static Dump parseDump(Percept p) {
		return new Dump(parseFacility(p));
	}
	
	public static Shop parseShop(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		int						restock	= parseInt		(params.get(3));
		Map<String, Integer>	price	= parseMap1		(params.get(4));
		Map<String, Integer>	amount	= parseMap2		(params.get(4));
		return new Shop(parseFacility(p), restock, price, amount);
	}
	
	public static Storage parseStorage(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		int						cap		= parseInt		(params.get(3));
		int						used	= parseInt		(params.get(4));
		Map<String, Integer>	stored	= parseMap1		(params.get(5));
		Map<String, Integer>	delivrd	= parseMap2		(params.get(5));
		return new Storage(parseFacility(p), cap, used, stored, delivrd);
	}
	
	public static Workshop parseWorkshop(Percept p) {
		return new Workshop(parseFacility(p));
	}
	
	public static ResourceNode parseResourceNode(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		String					resrce	= parseString	(params.get(3));
		return new ResourceNode(parseFacility(p), resrce);
	}
	
	//////////
	// JOBS //
	//////////
	
	private static Job parseJob(Percept p) {
		LinkedList<Parameter> 	params 	= p.getParameters();
		String					id		= parseString	(params.get(0));
		String					storage	= parseString	(params.get(1));
		int						reward	= parseInt		(params.get(2));
		int						start	= parseInt		(params.get(3));
		int						end		= parseInt		(params.get(4));
		Map<String, Integer>	items	= parseMap1		(params.getLast());
		return new Job(id, storage, reward, start, end, items);
	}
	
	public static SimpleJob parseSimple(Percept p) {
		return new SimpleJob(parseJob(p));
	}
	
	public static PostedJob parsePosted(Percept p) {
		return new PostedJob(parseJob(p));
	}
	
	public static AuctionJob parseAuction(Percept p) {
		List<Parameter> 		params 	= p.getParameters();
		int						fine	= parseInt		(params.get(5));
		int						bid		= parseInt		(params.get(6));
		int						steps	= parseInt		(params.get(7));
		return new AuctionJob(parseJob(p), fine, bid, steps);
	}
	
	public static MissionJob parseMission(Percept p) {
		return new MissionJob(parseAuction(p));
	}
	
}
