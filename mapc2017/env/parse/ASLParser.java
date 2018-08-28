package mapc2017.env.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jason.NoValueException;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;

public class ASLParser {
	
	/*******************/
	/** PARSE METHODS **/
	/*******************/
	
	////////////
	// ASLANG //
	////////////
	
	public static Atom parseAtom(Term t) {
		return (Atom) t;
	}
	
	public static StringTerm parseStringTerm(Term t) {
		return (StringTerm) t;
	}
	
	public static NumberTerm parseNumberTerm(Term t) {
		return (NumberTerm) t;
	}
	
	public static ListTerm parseListTerm(Term t) {
		return (ListTerm) t;
	}
	
	public static Literal parseLiteral(Term t) {
		return (Literal) t;
	}
	
	//////////////
	// JAVALANG //
	//////////////
	
	public static String parseFunctor(Term t) {
		return parseAtom(t).getFunctor();
	}
	
	public static String parseString(Term t) {
		return parseStringTerm(t).getString();
	}
	
	public static double parseDouble(Term t) {
		try {
			return parseNumberTerm(t).solve();
		} catch (NoValueException e) {
			e.printStackTrace();
			return Double.NaN;
		}
	}
	
	public static int parseInt(Term t) {
		return (int) parseDouble(t);
	}
	
	public static long parseLong(Term t) {
		return (long) parseDouble(t);
	}
	
	public static String[] parseArray(Term t) {
		List<String> list = new ArrayList<>();
		for (Term tm : parseListTerm(t)) {
			list.add(parseString(tm));
		}
		return list.toArray(new String[list.size()]);
	}
	
	public static Map<String, Integer> parseMap(Term t) {
		Map<String, Integer> map = new HashMap<>();
		for (Term tm : parseListTerm(t)) {
			Literal entry 	= parseLiteral(tm);
			String 	key 	= parseString(entry.getTerm(0));
			int		value	= parseInt(entry.getTerm(1));
			map.put(key, value);
		}
		return map;
	}
	
	/********************/
	/** CREATE METHODS **/
	/********************/
	
	public static ListTerm createArray(String[] array) {
		ListTerm list = ASSyntax.createList();
		for (String s : array) {
			list.add(ASSyntax.createString(s));
		}
		return list;
	}
	
	public static Literal createEntry(Entry<String, Integer> entry) 
	{
		return ASSyntax.createLiteral("map", 
				  ASSyntax.createString(entry.getKey()),
				  ASSyntax.createNumber(entry.getValue()));
	}
	
	public static ListTerm createMap(Map<String, Integer> map) 
	{		
		return map.entrySet().stream()
				.map(ASLParser::createEntry)
				.collect(Collectors.toCollection(ASSyntax::createList));
	}

}
