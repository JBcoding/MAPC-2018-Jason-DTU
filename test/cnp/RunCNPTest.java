package cnp;

import jason.JasonException;
import jason.infra.centralised.RunCentralisedMAS;

public class RunCNPTest {
	
	public static void main(String[] args) throws JasonException 
	{	
		RunCentralisedMAS.main(new String[] { "test/cnp/cnp_test.mas2j" });	
	}
	
}
