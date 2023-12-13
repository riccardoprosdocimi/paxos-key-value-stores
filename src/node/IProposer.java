package node;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The IProposer provides a remote method to initiate a proposal in the Paxos consensus algorithm.
 * It is part of the Paxos distributed consensus protocol representing the proposing role.
 */
public interface IProposer extends Remote {
  /**
   * Executes the Paxos algorithm with the given sequence number and proposal value.
   *
   * @param sequenceNumber the unique sequence number for the proposal
   * @param proposalValue  the value being proposed
   * @return whether this proposer has successfully committed the proposal value
   * @throws RemoteException the RMI failure
   */
  boolean executePaxos(long sequenceNumber, Object proposalValue) throws RemoteException;
}
