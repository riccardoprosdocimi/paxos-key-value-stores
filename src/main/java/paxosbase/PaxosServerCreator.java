package paxosbase;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * The PaxosServerCreator class is responsible for creating and binding the Paxos servers
 * within the RMI registry. It also configures the acceptors and learners for each server.
 */
public class PaxosServerCreator {

  /**
   * The main method to launch the creation and binding process of the Paxos servers.
   *
   * @param args Command-line arguments (unused in this context).
   */
  public static void main(String[] args) {
    try {
      int numServers = 5; // Total number of servers
      int basePort = 5000; // Starting port number

      Server[] servers = new Server[numServers];

      // Create and bind servers
      for (int serverId = 0; serverId < numServers; serverId++) {
        int port = basePort + serverId; // Increment port for each server

        // Create RMI registry at the specified port
        LocateRegistry.createRegistry(port);

        // Create server instance
        servers[serverId] = new Server(serverId, numServers);

        // Bind the server to the RMI registry
        Registry registry = LocateRegistry.getRegistry(port);
        registry.rebind("KVServer" + serverId, servers[serverId]);

        System.out.println("Server " + serverId + " is ready at port " + port);
      }

      // Set acceptors and learners for each server
      for (int serverId = 0; serverId < numServers; serverId++) {
        AcceptorInterface[] acceptors = new AcceptorInterface[numServers];
        LearnerInterface[] learners = new LearnerInterface[numServers];
        for (int i = 0; i < numServers; i++) {
          if (i != serverId) {
            acceptors[i] = servers[i];
            learners[i] = servers[i];
          }
        }
        servers[serverId].setAcceptors(acceptors);
        servers[serverId].setLearners(learners);
      }

    } catch (Exception e) {
      System.err.println("Server exception: " + e.toString());
      e.printStackTrace();
    }
  }
}
