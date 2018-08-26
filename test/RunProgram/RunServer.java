package RunProgram;

import massim.Server;

public class RunServer  {

	public static void main(String[] args) {
//		new Thread(new Runnable() {
//			public void run() {
//				GraphMonitor.main(new String[] { "-rmihost", "localhost", "-rmiport", "1099" });
//			}
//		}).start();

		new Thread(new Runnable() {
			public void run() {
				Server.main(new String[0]);
			}
		}).start();
		
		// Run the our program, which then can connect to the server.
		ConnectToServer.main(new String[0]);
	}

}
