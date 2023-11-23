package client;

import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import node.IKVStore;
import utils.ILogger;
import utils.Logger;

public class Client implements IClient {
  private static final String LOGGER_NAME = "ClientLogger";
  private static final String LOG_FILE_NAME = "ClientLog.log";
  private static final int NUM_NODES = 5; // total number of nodes
  private static final int BASE_PORT = 5000; // starting port number
  private final ILogger logger;
  private final Scanner scanner;
  private IKVStore server;

  public Client(String host, int port) {
    // Timeout mechanism
    System.setProperty("sun.rmi.transport.tcp.responseTimeout", "2000");
    System.setProperty("sun.rmi.transport.proxy.connectTimeout", "5000");
    this.logger = new Logger(LOGGER_NAME, LOG_FILE_NAME); // instantiate a logging system that already is thread-safe
    this.scanner = new Scanner(System.in); // instantiate an object to get user input
    String portStr = Integer.toString(port);
    String url = "rmi://" + host + ":" + port + "/KVStore" + portStr.charAt(portStr.length() - 1);
    try {
      this.server = (IKVStore) Naming.lookup(url);
    } catch (NotBoundException nbe) { // service not bound in registry
      this.logger.log("Couldn't connect to node at port " + port + ": " + url + " not bound");
      System.err.println("Couldn't connect to node at port " + port + ": " + url + " not bound.\n" + nbe.getMessage());
      this.logger.close();
      this.scanner.close();
      System.exit(1);
    } catch (MalformedURLException mue) { // malformed URL
      this.logger.log(url + " is not an appropriately formatted URL: " + mue.getMessage());
      System.err.println(url + " is not an appropriately formatted URL" + mue.getMessage());
      this.logger.close();
      this.scanner.close();
      System.exit(1);
    } catch (ConnectException ce) { // connection times out
      this.logger.log("Connection to server timed out: " + ce.getMessage());
      System.err.println("Connection to server timed out: " + ce.getMessage());
      this.logger.close();
      this.scanner.close();
      System.exit(1);
    } catch (RemoteException re) { // registry not found
      this.logger.log("Couldn't connect to server: registry not found");
      System.err.println("Couldn't connect to server: registry not found");
      this.logger.close();
      this.scanner.close();
      System.exit(1);
    }
  }

  /**
   * Pre-populates the key-value store.
   */
  @Override
  public void prePopulate() {
    try {
      this.logger.log("Pre-populating...");
      System.out.println("Pre-populating...");
      System.out.println(this.server.put("hello", "ciao"));
      System.out.println(this.server.put("goodbye", "addio"));
      System.out.println(this.server.put("thank you", "grazie"));
      System.out.println(this.server.put("please", "per favore"));
      System.out.println(this.server.put("yes", "sì"));
      System.out.println(this.server.put("no", "no"));
      System.out.println(this.server.put("water", "acqua"));
      System.out.println(this.server.put("food", "cibo"));
      System.out.println(this.server.put("friend", "amico"));
      System.out.println(this.server.put("love", "amore"));
      this.logger.log("Pre-population completed");
      System.out.println("Pre-population completed");
      Thread.sleep(1000); // wait a second before user interaction
    } catch (ConnectException ce) { // connection times out
      this.logger.log("TranslationService timed out (pre-populate): " + ce.getMessage());
      System.err.println("TranslationService timed out (pre-populate): " + ce.getMessage());
    } catch (RemoteException re) { // RMI failure
      this.logger.log("TranslationService error (pre-populate): " + re.getMessage());
      System.err.println("TranslationService error (pre-populate): " + re.getMessage());
    } catch (InterruptedException ie) { // thread is prematurely resumed
      this.logger.log("Pre-population error (timeout interrupted): " + ie.getMessage());
      System.err.println("Pre-population error (timeout interrupted): " + ie.getMessage());
    }
  }

  /**
   * Gets the user request.
   *
   * @return the user request
   */
  @Override
  public String getRequest() {
    System.out.print("Enter operation (PUT/GET/DELETE:key:value[only with PUT]): ");
    return this.scanner.nextLine();
  }

  private String parseRequest(String request) {
    String result;
    String[] elements = request.split(":");
    if (elements.length < 2 || elements.length > 3) { // the protocol is not followed
      this.logger.log("Received malformed request: " + request);
      return "FAIL: please follow the predefined protocol PUT/GET/DELETE:key:value[with PUT only] and try again";
    } else {
      String operation;
      try {
        operation = elements[0].toUpperCase(); // PUT/GET/DELETE
      } catch (Exception e) {
        this.logger.log("Parsing error: invalid operation");
        return "FAIL: could not parse the operation requested. Please follow the predefined protocol PUT/GET/DELETE:key:value[with PUT only] and try again";
      }
      String key;
      try {
        key = elements[1].toLowerCase(); // word to be translated
      } catch (Exception e) {
        this.logger.log("Parsing error: invalid key");
        return "FAIL: could not parse the key requested. Please follow the predefined protocol PUT/GET/DELETE:key:value[with PUT only] and try again";
      }
      String value;
      try {
        switch (operation) {
          case "PUT":
            try {
              value = elements[2].toLowerCase(); // word to translate
              this.logger.log("Received a request to save " + "\"" + key + "\"" + " mapped to " + "\"" + value + "\"");
            } catch (Exception e) {
              this.logger.log("Parsing error: invalid value");
              return "FAIL: could not parse the value requested. Please follow the predefined protocol PUT/GET/DELETE:key:value[with PUT only] and try again";
            }
            result = this.server.put(key, value);
            break;
          case "GET":
            result = this.get(key);
            if (result == null) {
              this.logger.log("Received a request to retrieve the value mapped to a nonexistent key: \"" + key + "\"");
              result = "FAIL: I don't know the translation for " + "\"" + key + "\"" + " yet";
            } else {
              this.logger.log("Received a request to retrieve the value mapped to \"" + key + "\"");
            }
            break;
          case "DELETE":
            this.logger.log("Received a request to delete the key-value pair associated with the key: \"" + key + "\"");
            result = this.server.delete(key);
            break;
          default: // invalid request
            this.logger.log("Received an invalid request: " + request);
            return "Invalid request. Please follow the predefined protocol PUT/GET/DELETE:key:value[with PUT only] and try again";
        }
      } catch (ConnectException ce) { // connection times out
        this.logger.log("TranslationService timed out: " + ce.getMessage());
        result = "TranslationService timed out: " + ce.getMessage();
      } catch (RemoteException re) { // RMI failure
        this.logger.log("TranslationService error: " + re.getMessage());
        result = "TranslationService error: " + re.getMessage();
      }
    }
    this.logger.log("Reply: " + result);
    return result;
  }

  /**
   * Starts the client.
   */
  @Override
  public void execute() {
    boolean isRunning = true;
    this.logger.log("Client is running...");
    while (isRunning) { // keep getting user input
      String request = this.getRequest(); // get the user request
      if (request.equalsIgnoreCase("shutdown") || request.equalsIgnoreCase("stop")) { // if the user wants to quit
        isRunning = false; // prepare the shutdown process
      } else {
        System.out.println(this.parseRequest(request)); // process the request and output the result
      }
    }
    this.shutdown(); // shut down the servers and the client
  }

  public String get(String key) {
    Map<String, Integer> valuesToFreq = new HashMap<>(NUM_NODES);
    for (int nodeId = 0; nodeId < NUM_NODES; nodeId++) {
      int port = BASE_PORT + nodeId; // increment port number for each node
      String portStr = Integer.toString(port);
      String url = "rmi://localhost:" + port + "/KVStore" + portStr.charAt(portStr.length() - 1); // NOTE: here is assumed host is always localhost
      try {
        IKVStore node = (IKVStore) Naming.lookup(url);
        String value = node.get(key);
        valuesToFreq.put(value, valuesToFreq.getOrDefault(value, 0) + 1);
      } catch (NotBoundException nbe) {
        this.logger.log(url + " is not currently bound in registry: " + nbe.getMessage());
        return "FAIL: " + url + " is not currently bound in registry";
      } catch (MalformedURLException mue) {
        this.logger.log(url + " is not an appropriately formatted URL: " + mue.getMessage());
        return "FAIL: " + url + " is not an appropriately formatted URL";
      } catch (RemoteException re) {
        this.logger.log("Could not contact the registry: " + re.getMessage());
        return "FAIL: could not contact the registry";
      }
    }
    for (Map.Entry<String, Integer> entry : valuesToFreq.entrySet()) {
      if (entry.getValue() > NUM_NODES / 2) {
        return entry.getKey();
      }
    }
    return "FAIL: nodes did not agree on a single value";
  }

  /**
   * Stops this client and the servers.
   */
  @Override
  public void shutdown() {
    this.logger.log("Received a request to shut down...");
    System.out.println("Client is shutting down...");
      for (int nodeId = 0; nodeId < NUM_NODES; nodeId++) {
        int port = BASE_PORT + nodeId; // increment port number for each node
        String portStr = Integer.toString(port);
        String url = "rmi://localhost:" + port + "/KVStore" + portStr.charAt(portStr.length() - 1); // NOTE: here is assumed host is always localhost
        try {
          IKVStore node = (IKVStore) Naming.lookup(url);
          node.shutdown(); // shut down all the servers
        } catch (NotBoundException nbe) { // url not bound
          this.logger.log("Couldn't connect to node with ID " + nodeId + " at port " + port + " (shutdown): " + url + " not bound");
          System.err.println("Couldn't connect to node with ID " + nodeId + " at port " + port + " (shutdown): " + url + " not bound.\n" + nbe.getMessage());
        } catch (MalformedURLException mue) { // malformed url
          this.logger.log(url + " is not an appropriately formatted URL (shutdown): " + mue.getMessage());
          System.err.println(url + " is not an appropriately formatted URL (shutdown): " + mue.getMessage());
        } catch (ConnectException ce) { // connection times out
          this.logger.log("Node with ID " + nodeId + " at port " + port + " timed out (shutdown): " + ce.getMessage());
          System.err.println("Node with ID " + nodeId + " at port " + port + " timed out (shutdown): " + ce.getMessage());
        } catch (RemoteException re) { // RMI failure
          this.logger.log("Node with ID " + nodeId + " at port " + port + " connection error (shutdown): " + re.getMessage());
          System.err.println("Node with ID " + nodeId + " at port " + port + " connection error (shutdown): " + re.getMessage());
        }
      }
    this.logger.log("Nodes closed");
    this.scanner.close();
    this.logger.log("Client closed");
    this.logger.close();
    System.out.println("Client closed");
  }
}
