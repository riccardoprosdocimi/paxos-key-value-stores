package node;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The Acceptor interface defines the remote methods to be implemented by the acceptors in the Paxos consensus algorithm.
 * It includes methods for preparing and accepting proposals.
 */
public interface IAcceptor extends Remote {

  /**
   * Sends a 'promise' message to the proposer promising to accept the proposal with the given unique sequence number.
   *
   * @param sequenceNumber the unique sequence number of the proposal
   * @return an integer response indicating the status or decision related to the proposal (1 meaning the proposal has been accepted or 0 meaning the proposal has been rejected) and the accepted value
   * @throws RemoteException the RMI failure
   */
  Object prepare(long sequenceNumber) throws RemoteException;

  /**
   * Accepts or rejects the value promised to before.
   *
   * @param sequenceNumber the unique sequence number of the proposal
   * @param proposalValue  the value of the proposal
   * @return the sequence number associated with the accepted value
   * @throws RemoteException the RMI failure
   */
  long accept(long sequenceNumber, Object proposalValue) throws RemoteException;
}
