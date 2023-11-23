package main;

import client.Client;

/**
 * The type ClientMain represents the entry point of the RMI client using the two phase commit protocol.
 */
public class ClientMain {
  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    try {
      if (args.length != 2) {
        System.err.println("Usage: java main.ClientMain <hostname> <port>");
        System.exit(1);
      } else {
        int portNumber = Integer.parseInt(args[1]);
        if (portNumber < 5000 || portNumber > 5004) {
          System.err.println("Invalid port number.\nPlease provide one of the following port numbers: 5000, 5001, 5002, 5003, 5004");
          System.exit(1);
        } else {
          Client client = new Client(args[0], portNumber);
          //client.prePopulate(); // pre-populate the replicas
          client.execute(); // start interactive mode
        }
      }
    } catch (NumberFormatException e) {
      System.err.println("Invalid port number.\nPlease provide one of the following port numbers: 5000, 5001, 5002, 5003, 5004");
      System.exit(1);
    }
  }
}
