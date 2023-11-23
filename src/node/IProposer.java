package node;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The IProposer provides a remote method to initiate a proposal in the Paxos consensus algorithm.
 * It is part of the Paxos distributed consensus protocol, representing the proposing role.
 */
public interface IProposer extends Remote {

  /**
   * Initiates the Paxos algorithm with the given sequence number and value.
   *
   * @param sequenceNumber the unique sequence number for the proposal
   * @param proposalValue the value being proposed
   * @return whether this proposer has extracted a majority or not
   * @throws RemoteException if a remote communication error occurs
   */
  boolean executePaxos(long sequenceNumber, Object proposalValue) throws RemoteException;
}
