package node;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The interface IKVStore contains methods that all types of key-value stores communicating via Remote Method Invocation (RMI) should support.
 */
public interface IKVStore extends Remote {
  /**
   * Saves a key-value pair in a hashmap.
   *
   * @param key   the word to be translated
   * @param value the translation
   * @return the outcome of the operation
   * @throws RemoteException the RMI failure
   */
  String put(String key, String value) throws RemoteException;

  /**
   * Removes a key-value pair.
   *
   * @param key the word to be deleted
   * @return the outcome of the operation
   * @throws RemoteException the RMI failure
   */
  String delete(String key) throws RemoteException;

  /**
   * Retrieves the value of a key.
   *
   * @param key the word to be translated
   * @return the translation
   * @throws RemoteException the RMI failure
   */
  String get(String key) throws RemoteException;

  /**
   * Stops this node.
   *
   * @throws RemoteException the RMI failure
   */
  void shutdown() throws RemoteException;
}
