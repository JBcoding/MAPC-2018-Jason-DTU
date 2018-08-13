package mapc2017.mas;

import jason.JasonException;
import jason.infra.centralised.RunCentralisedMAS;

public class RunClient {

	public static void main(String[] args) throws JasonException
	{
		RunCentralisedMAS.main(new String[] { "src/mapc2017/mas/multiagent_jason.mas2j" });
	}
}
