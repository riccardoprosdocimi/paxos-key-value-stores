package node;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IKVStore extends Remote {
  String put(String key, String value) throws RemoteException;
  String delete(String key) throws RemoteException;
  String get(String key) throws RemoteException;

  /**
   * Stops this participant.
   *
   * @throws RemoteException the RMI failure
   */
  void shutdown() throws RemoteException;
}
