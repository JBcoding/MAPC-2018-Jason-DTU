package RunProgram;

import jason.infra.centralised.RunCentralisedMAS;

public class ConnectToServer extends RunCentralisedMAS {

	public static void main(String[] args) {
		try {
//			EnvironmentInterface ei = new EnvironmentInterface();
//			ei.registerAgent("agentA1");
//			ei.associateEntity("agentA1", "connectionA1");
//			ei.attachAgentListener("agentA1", new TestListener());
//			
//			// Setup done, start running the environment interface
//			ei.start();
			
//			while (true)
//			{
//				ei.performAction("agentA1", new Action("goto", new Identifier("facility=shop2")));
//			}
//			
			// Code to start the Jason program
			ConnectToServer runner = new ConnectToServer();
			runner.init(new String[] { "multiagent_jason.mas2j" });
			runner.getProject().addSourcePath("./src/asl");
			runner.create();
			runner.start();
			runner.waitEnd();
			runner.finish();
			
		}
		catch (Exception e) {}
	}
	
}

