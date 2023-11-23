package node;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The IAcceptor defines the remote methods to be implemented by the acceptors in the Paxos
 * consensus algorithm. It includes methods for preparing and accepting proposals.
 */
public interface IAcceptor extends Remote {

  /**
   * Sends a 'promise' message to the proposer promising to accept the proposal with the given unique sequence number.
   *
   * @param sequenceNumber the unique sequence number of the proposal
   * @param proposalValue the value that has been accepted
   * @return an integer response indicating the status or decision related to the proposal (1 meaning the proposal has been accepted or 0 meaning the proposal has been rejected) and the accepted value
   * @throws RemoteException if a remote communication error occurs
   */
  Object sendPromiseMsg(long sequenceNumber, Object proposalValue) throws RemoteException;

  /**
   * Accepts or rejects the value promised to before.
   *
   * @param sequenceNumber the unique sequence number of the proposal
   * @param proposalValue the value of the proposal
   * @return A boolean indicating whether the proposal was accepted (1) or rejected (0)
   * @throws RemoteException if a remote communication error occurs
   */
  int accept(long sequenceNumber, Object proposalValue) throws RemoteException;
}
