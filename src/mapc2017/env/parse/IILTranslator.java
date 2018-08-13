package mapc2017.env.parse;

import java.util.ArrayList;
import java.util.Collection;

import eis.iilang.*;
import jason.*;
import jason.asSyntax.*;

/**
 * Translator methods provided by massim starter kit.
 *
 */
public class IILTranslator {

	public static Literal perceptToLiteral(Percept per) throws JasonException {
		Literal l = ASSyntax.createLiteral(per.getName());
		for (Parameter par : per.getParameters())
			l.addTerm(parameterToTerm(par));
		return l;
	}

	public static Term parameterToTerm(Parameter par) throws JasonException {
		if (par instanceof Numeral) {
			return ASSyntax.createNumber(((Numeral) par).getValue().doubleValue());
		} else if (par instanceof Identifier) {
			try {
				Identifier i = (Identifier) par;
				String a = i.getValue();
				if (!Character.isUpperCase(a.charAt(0)))
					return ASSyntax.parseTerm(a);
			} catch (Exception ignored) {
			}
			return ASSyntax.createString(((Identifier) par).getValue());
		} else if (par instanceof ParameterList) {
			ListTerm list = new ListTermImpl();
			ListTerm tail = list;
			for (Parameter p : (ParameterList) par)
				tail = tail.append(parameterToTerm(p));
			return list;
		} else if (par instanceof Function) {
			Function f = (Function) par;
			Structure l = ASSyntax.createStructure(f.getName());
			for (Parameter p : f.getParameters())
				l.addTerm(parameterToTerm(p));
			return l;
		}
		throw new JasonException("The type of parameter " + par + " is unknown!");
	}

	public static Action literalToAction(String literal) {
		return literalToAction(Literal.parseLiteral(literal));
	}

	public static Action literalToAction(Literal action) {
		Parameter[] pars = new Parameter[action.getArity()];
		for (int i = 0; i < action.getArity(); i++)
			pars[i] = termToParameter(action.getTerm(i));
		return new Action(action.getFunctor(), pars);
	}

	public static Parameter termToParameter(Term t) {
		if (t.isNumeric()) {
			try {
				double d = ((NumberTerm) t).solve();
				if ((d == Math.floor(d)) && !Double.isInfinite(d))
					return new Numeral((int) d);
				return new Numeral(d);
			} catch (NoValueException e) {
				e.printStackTrace();
			}
			return new Numeral(null);
		} else if (t.isList()) {
			Collection<Parameter> terms = new ArrayList<>();
			for (Term listTerm : (ListTerm) t)
				terms.add(termToParameter(listTerm));
			return new ParameterList(terms);
		} else if (t.isString()) {
			return new Identifier(((StringTerm) t).getString());
		} else if (t.isLiteral()) {
			Literal l = (Literal) t;
			if (!l.hasTerm()) {
				return new Identifier(l.getFunctor());
			} else {
				Parameter[] terms = new Parameter[l.getArity()];
				for (int i = 0; i < l.getArity(); i++)
					terms[i] = termToParameter(l.getTerm(i));
				return new Function(l.getFunctor(), terms);
			}
		}
		return new Identifier(t.toString());
	}

}
