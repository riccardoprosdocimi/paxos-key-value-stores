package node;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The ILearner represents a remote interface that defines
 * the learning process in the Paxos consensus algorithm. It contains
 * the learning method to acknowledge an accepted proposal.
 */
public interface ILearner extends Remote {
  /**
   * The learn method is used to inform the Learner of an accepted proposal.
   *
   * @param sequenceNumber The unique identifier for the proposal.
   * @param acceptedValue The value that has been accepted.
   * @throws RemoteException if a remote communication error occurs
   */
  void learn(long sequenceNumber, Object acceptedValue) throws RemoteException;
}
