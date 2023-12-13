package node;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The Learner interface represents a remote interface that defines the learning process in the Paxos consensus algorithm.
 * It contains the learning method to acknowledge an accepted proposal.
 */
public interface ILearner extends Remote {
  /**
   * The learn method is used to inform the Learner of an accepted proposal.
   *
   * @param sequenceNumber the unique identifier for the proposal.
   * @param acceptedValue  the value that has been accepted.
   * @throws RemoteException the RMI failure
   */
  void learn(long sequenceNumber, Object acceptedValue) throws RemoteException;
}
