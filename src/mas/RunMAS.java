package mas;

import jason.JasonException;
import jason.infra.centralised.RunCentralisedMAS;
import massim.scenario.city.data.Location;

public class RunMAS {

	public static void main(String[] args) throws JasonException {
		Location.setProximity(5);
		RunCentralisedMAS.main(new String[] { "src/mas/multiagent_jason.mas2j" });
	}
}
