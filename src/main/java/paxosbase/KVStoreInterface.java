package paxosbase;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface KVStoreInterface extends Remote {
  void put(String key, String value) throws RemoteException;
  void delete(String key) throws RemoteException;
}

