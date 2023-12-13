package main;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import node.IAcceptor;
import node.ILearner;
import node.Node;

/**
 * The PaxosServerCreator class is responsible for creating and binding the Paxos servers within the RMI registry.
 * It also configures the acceptors and learners for each server.
 */
public class PaxosServerCreator {
  /**
   * The main method to launch the creation and binding process of the Paxos servers.
   *
   * @param args command-line arguments (unused in this context)
   */
  public static void main(String[] args) {
    try {
      int numNodes = 5; // total number of nodes
      int basePort = 5000; // starting port number

      Node[] nodes = new Node[numNodes];

      // Create and bind nodes
      for (int nodeId = 0; nodeId < numNodes; nodeId++) {
        int port = basePort + nodeId; // increment port number for each node

        // Create node instance
        nodes[nodeId] = new Node(nodeId, port, numNodes - 1);

        // Create RMI registry at the specified port
        Registry registry = LocateRegistry.createRegistry(port);
        // Bind the node to the RMI registry
        registry.rebind("KVStore" + nodeId, nodes[nodeId]);

        nodes[nodeId].setRegistry(registry);
      }
      // Set acceptors and learners for each node
      for (int nodeId = 0; nodeId < numNodes; nodeId++) {
        List<IAcceptor> acceptors = new ArrayList<>(4);
        List<ILearner> learners = new ArrayList<>(4);
        for (int i = 0; i < numNodes; i++) {
          if (i != nodeId) {
            acceptors.add(nodes[i]);
            learners.add(nodes[i]);
          }
        }
        nodes[nodeId].setAcceptors(acceptors);
        nodes[nodeId].setLearners(learners);
      }
      System.out.println("Nodes ready...");
    } catch (RemoteException e) {
      System.err.println("The registry could not be exported or\nThe reference could not be created or\nThe remote communication with the registry failed:\n" + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}
