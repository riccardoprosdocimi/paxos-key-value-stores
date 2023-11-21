package paxosbase;

import java.rmi.Naming;
import java.rmi.RemoteException;

public class Client {
  private KVStoreInterface server;

  public Client(String host, int port) throws Exception {
    String url = "rmi://" + host + ":" + port + "/KVStore";
    server = (KVStoreInterface) Naming.lookup(url);
  }

  public void put(String key, String value) {
    try {
      server.put(key, value);
      System.out.println("PUT operation successful!");
    } catch (RemoteException e) {
      System.err.println("PUT operation failed: " + e.getMessage());
    }
  }

  public void delete(String key) {
    try {
      server.delete(key);
      System.out.println("DELETE operation successful!");
    } catch (RemoteException e) {
      System.err.println("DELETE operation failed: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    try {
      Client client = new Client("localhost", 1099); // example host and port
      client.put("key1", "value1");
      client.delete("key2");
    } catch (Exception e) {
      System.err.println("Client exception: " + e.getMessage());
    }
  }
}
