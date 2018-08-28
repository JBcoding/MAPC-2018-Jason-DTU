package mapc2017.mas;

import jason.infra.centralised.RunCentralisedMAS;
import massim.Server;

public class Runner {

	public static void main(String[] args) throws Exception 
	{
		new Thread(new Runnable() {
			public void run() {
//				 Server.main(new String[] { "-conf", "conf/SampleConfig.json", "--monitor" });
				//  Server.main(new String[] { "-conf", "conf/Tokyo.json", "--monitor" });
//				 Server.main(new String[] { "-conf", "conf/Mexico-City.json", "--monitor" });
				Server.main(new String[] { "-conf", "conf/ConfigMatch.json", "--monitor" });
			}
		}).start();
				
		RunCentralisedMAS.main(new String[] { "src/mapc2017/mas/multiagent_jason.mas2j" });
	}
}
