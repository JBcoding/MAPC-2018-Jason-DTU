package mas;
import jason.JasonException;
import jason.infra.centralised.RunCentralisedMAS;
import massim.Server;
import massim.scenario.city.data.Location;

public class Runner {

	public static void main(String[] args) throws JasonException {
		new Thread(new Runnable() {
			public void run() {
				Server.main(new String[] { "-conf", "conf/CompLikeConfig.json", "--monitor" });
			}
		}).start();

		Location.setProximity(5);
		RunCentralisedMAS.main(new String[] { "src/mas/multiagent_jason.mas2j" });
	}
}
