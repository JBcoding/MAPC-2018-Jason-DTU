package mapc2017.mas;

import massim.Server;

public class RunServer {

	public static void main(String[] args)
	{
		Server.main(new String[] { "-conf", "conf/SampleConfig2Teams.json", "--monitor" });
	}
}
