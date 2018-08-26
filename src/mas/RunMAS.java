package mas;

import jason.JasonException;
import jason.infra.centralised.RunCentralisedMAS;

public class RunMAS {

	public static void main(String[] args)
	{
		try {
			RunCentralisedMAS.main(new String[] { "src/mas/multiagent_jason.mas2j" });
		} catch (JasonException e) {
			e.printStackTrace();
		}
	}
}
