/**
 * 
 */
package envTests;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.*;
import eis.iilang.*;
import env.Translator;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;

/**
 *
 */
public class TranslatorTests {

	@Test
	public void stringToActionTest() {
		Action action = Translator.stringToAction("goto(shop1)");
		
		assertEquals("goto", action.getName());
		assertEquals(1, action.getParameters().size());
		assertEquals(new Identifier("shop1"), action.getParameters().get(0)); 
		
		action = Translator.stringToAction("buy(item0, 10)");
		assertEquals("buy", action.getName());
		assertEquals(2, action.getParameters().size());
		assertEquals(new Identifier("item0"), action.getParameters().get(0));
		assertEquals(new Numeral(10.0), action.getParameters().get(1));
	}
	
	
	@Test
	public void parametersToArgumentTest()
	{
		// Direct known subclass of Parameter:
		// Function, Identifier, Numeral, ParameterList, TruthValue
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Identifier("shop0"));
		parameters.add(new Numeral(10));
		parameters.add(new Function("fun1", new Numeral(10)));
		parameters.add(new TruthValue(true));
//		item(tool1,155,9),item(tool3,133,13)
		parameters.add(new ParameterList(new Numeral(10), new Numeral(20)));
		
		Object[] arguments = Translator.parametersToArguments(parameters);
		
		assertEquals(5, arguments.length);
		assertEquals("shop0", arguments[0]);
		assertEquals(10, arguments[1]);
		assertEquals("fun1(10)", arguments[2]);
		assertEquals("true", arguments[3]);
		assertEquals("[10,20]", arguments[4]);
	}
	
	@Test
	public void perceptToLiteralTest()
	{
		Percept percept = new Percept("charge", new Numeral(100));
		Literal literal = Translator.perceptToLiteral(percept);	
		
		assertEquals(1, literal.getArity());
		assertEquals("charge", literal.getFunctor());
		assertEquals(new NumberTermImpl(100), literal.getTerm(0));
		
		percept = new Percept("step", new Numeral(15));
		literal = Translator.perceptToLiteral(percept);
		
		assertEquals(1, literal.getArity());
		assertEquals("step", literal.getFunctor());
		assertEquals(new NumberTermImpl(15), literal.getTerm(0));
		
		percept = new Percept("string", new Identifier("something"), new Identifier("Var"));
		literal = Translator.perceptToLiteral(percept);
		
		assertEquals("string", literal.getFunctor());
		assertEquals(new Atom("something"), literal.getTerm(0));
		assertEquals("something", literal.getTerm(0).toString());
		
		assertFalse(literal.getTerm(0).isVar());
		assertTrue(literal.getTerm(1).isVar());
	}
}
