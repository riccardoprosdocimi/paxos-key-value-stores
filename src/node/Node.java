package node;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import utils.ILogger;
import utils.Logger;

/**
 * Implementation of a Node class that represents a node in a Paxos distributed consensus system.
 * This server plays the role of Proposer, Acceptor, and Learner in the Paxos algorithm, and it also handles key-value store operations.
 */
public class Node extends UnicastRemoteObject implements IProposer, IAcceptor, ILearner, IKVStore {
  private static final AtomicLong SEQ_NUM = new AtomicLong(0);
  private Registry registry;
  private final ConcurrentHashMap<String, String> kvStore;
  private final long nodeId;
  private final int numNodes;
  private long highestSeenSeqNum;
  private Object acceptedValue;
  private final ILogger logger;
  private List<IAcceptor> acceptors;
  private List<ILearner> learners;

  /**
   * Constructor to create a Node instance.
   * @param nodeId The unique ID of this server
   * @param port the port number of this node
   * @param numNodes The total number of servers in the system
   */
  public Node(long nodeId, int port, int numNodes) throws RemoteException {
    this.kvStore = new ConcurrentHashMap<>();
    this.nodeId = nodeId;
    this.numNodes = numNodes;
    this.highestSeenSeqNum = -Long.MAX_VALUE;
    String loggerName = "Node" + nodeId + "Logger";
    String logFileName = "Node" + nodeId + "Log.log";
    this.logger = new Logger(loggerName, logFileName);
    this.logger.log(this + " is ready at port " + port);
  }

  /**
   * Sets the acceptors for this node.
   *
   * @param acceptors array of acceptors
   */
  public void setAcceptors(List<IAcceptor> acceptors) {
    this.acceptors = acceptors;
  }

  /**
   * Sets the learners for this node.
   *
   * @param learners array of learners
   */
  public void setLearners(List<ILearner> learners) {
    this.learners = learners;
  }

  @Override
  public synchronized String get(String key) throws RemoteException {
    if (this.kvStore.containsKey(key)) {
      this.logger.log("SUCCESS: returned the value \"" + key + "\" associated with \"" + key + "\"");
    } else { // if the key doesn't exist, just return null
      this.logger.log("FAIL: no value is associated with \"" + key + "\"");
    }
    return this.kvStore.get(key);
  }

  @Override
  public synchronized String put(String key, String value) throws RemoteException {
    if (this.kvStore.containsKey(key)) { // if the key is already in the store
      this.logger.log("FAIL: the translation for \"" + key + "\" already exists");
      return "FAIL: the translation for \"" + key + "\" already exists";
    } else {
      if (this.propose(new Operation("PUT", key, value))) {
        return "SUCCESS";
      } else {
        return "FAIL: execution of Paxos failed.\nPlease try again.";
      }
    }
  }

  @Override
  public synchronized String delete(String key) throws RemoteException {
    if (this.kvStore.containsKey(key)) {
      if (this.propose(new Operation("DELETE", key))) {
        this.logger.log("Value proposed promised to be accepted by the majority of nodes");
        return "SUCCESS";
      } else {
        return "FAIL: execution of Paxos failed.\nPlease try again.";
      }
    } else {
      this.logger.log("FAIL: " + "\"" + key + "\" does not exist");
      return "FAIL: " + "\"" + key + "\" does not exist";
    }
  }

  /**
   * Generates a globally unique and sortable sequence number.
   *
   * @return the sequence number
   */
  private long generateSequenceNumber() {
    long currTime = System.currentTimeMillis(); // store the current time in milliseconds
    long sequenceNumber = (currTime << 16) | (this.nodeId & 0xFFFF); // create a globally unique and sortable 64-bit number that includes both the timestamp and the node ID
    return sequenceNumber + SEQ_NUM.getAndIncrement(); // ensure that each generated number is unique, even if multiple calls happen concurrently
  }

  /**
   * Apply the given operation to the key-value store.
   *
   * @param operation the operation to apply
   */
  private void applyOperation(Operation operation) {
    if (operation == null) return;
    switch (operation.type) {
      case "PUT":
        this.kvStore.put(operation.key, operation.value);
        this.logger.log("SUCCESS: added the key \"" + operation.key + "\" associated with \"" + operation.value + "\"");
        break;
      case "DELETE":
        this.kvStore.remove(operation.key);
        this.logger.log("SUCCESS: deleted the key-value pair associated with \"" + operation.key + "\"");
        break;
      default: // should never get here
        this.logger.log("FAIL: unknown operation type: " + operation.type);
        throw new IllegalArgumentException("Unknown operation type: " + operation.type);
    }
  }

  /**
   * Creates the 'prepare' message that holds inside it the operation being proposed, as well as the sequence number.
   *
   * @param operation the operation
   * @throws RemoteException the RMI failure
   */
  private boolean propose(Operation operation) throws RemoteException {
    long sequenceNumber = generateSequenceNumber();
    this.logger.log("Sequence number generated: " + sequenceNumber);
    return executePaxos(sequenceNumber, operation);
  }

  @Override
  public synchronized Object sendPromiseMsg(long sequenceNumber, Object proposalValue) throws RemoteException {
    // Implement Paxos prepare logic here
    if (sequenceNumber > this.highestSeenSeqNum) {
      this.logger.log("Sent promise message (" + sequenceNumber + " > " + this.highestSeenSeqNum + ") with value " + proposalValue.toString());
      this.highestSeenSeqNum = sequenceNumber;
      this.acceptedValue = proposalValue;
      return new Promise(1, proposalValue);
    } else {
      return new Promise(0);
    }
  }

  @Override
  public synchronized int accept(long sequenceNumber, Object proposalValue) throws RemoteException {
    // Implement Paxos accept logic here
    if (this.highestSeenSeqNum == sequenceNumber && this.acceptedValue.equals(proposalValue)) {
      this.logger.log(proposalValue + " associated with sequence number " + sequenceNumber + " accepted");
      return 1;
    } else {
      this.logger.log(proposalValue + " associated with sequence number " + sequenceNumber + " rejected");
      return 0;
    }
  }

  @Override
  public synchronized void learn(long sequenceNumber, Object acceptedValue) throws RemoteException {
    // Implement Paxos learn logic here
    this.applyOperation((Operation) acceptedValue);
    this.logger.log(acceptedValue.toString() + " learned and executed");
  }

  /**
   * Initiates the Paxos algorithm with the given sequence number and value.
   *
   * @param sequenceNumber the unique sequence number for the proposal
   * @param proposalValue the value being proposed
   * @return whether this proposer has extracted a majority or not
   * @throws RemoteException the RMI failure
   */
  @Override
  public synchronized boolean executePaxos(long sequenceNumber, Object proposalValue) throws RemoteException {
    // Send propose messages
    int majorityThreshold = this.numNodes / 2;
    int promiseMsgCount = 0;
    Set<Object> acceptedValue = new HashSet<>(this.numNodes);
    for (IAcceptor acceptor : this.acceptors) { // gather promises
      Promise promise = (Promise) acceptor.sendPromiseMsg(sequenceNumber, proposalValue);
      promiseMsgCount += promise.vote;
      acceptedValue.add(promise.value);
    }
    if (promiseMsgCount > majorityThreshold && acceptedValue.size() == 1) { // check if majority is reached and the accepted value is single
      // Send accept messages
      this.acceptedValue = acceptedValue.toArray()[0];
      this.logger.log("Proposal accepted with value " + this.acceptedValue.toString() + ": able to extract a majority of promises (" + promiseMsgCount + " / " + this.numNodes + ")");
      int acceptCount = 0;
      for (IAcceptor acceptor : this.acceptors) { // gather votes
        acceptCount += acceptor.accept(sequenceNumber, this.acceptedValue);
      }
      if (acceptCount > majorityThreshold) { // check if majority accepts the value
        for (ILearner learner : this.learners) {
          learner.learn(sequenceNumber, this.acceptedValue);
        }
        this.logger.log("Accepted value " + this.acceptedValue.toString() + " has been saved: accept message accepted by the majority (" + acceptCount + " / " + this.numNodes + ")");
        this.applyOperation((Operation) this.acceptedValue);
        return true;
      } else {
        this.logger.log("Accepted value " + this.acceptedValue.toString() + " has been rejected: accept message rejected by the majority (" + acceptCount + " / " + this.numNodes + ")");
        return false;
      }
    } else {
      this.logger.log("Proposal aborted: couldn't extract a majority of promises (" + promiseMsgCount + " / " + this.numNodes + ")");
      return false;
    }
  }

  @Override
  public String toString() {
    return "Node{" +
            "nodeId=" + nodeId +
            '}';
  }

  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  /**
   * Stops this participant.
   *
   * @throws RemoteException the RMI failure
   */
  @Override
  public void shutdown() throws RemoteException {
    this.logger.log("Received a request to shut down...");
    System.out.println(this + " is shutting down...");
    try {
      this.registry.unbind("KVStore" + this.nodeId); // unbind the remote object from the custom name
      this.logger.log(this + " unbound in registry");
    } catch (NotBoundException e) {
      this.logger.log("Unbind error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
    UnicastRemoteObject.unexportObject(this, true); // unexport the remote object
    this.logger.log(this + " unexported");
    this.logger.log(this + " closed");
    this.logger.close();
    System.out.println(this + " closed");
  }

  /**
   * Static class representing an operation on the key-value store.
   */
  private static class Operation {
    final String type;
    final String key;
    final String value;

    Operation(String type, String key, String value) {
      this.type = type;
      this.key = key;
      this.value = value;
    }

    Operation(String type, String key) {
      this(type, key, null);
    }

    @Override
    public String toString() {
      return "Operation{" +
              "type='" + type + '\'' +
              ", key='" + key + '\'' +
              ", value='" + value + '\'' +
              '}';
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      Operation operation = (Operation) obj;
      if (!type.equals(operation.type)) return false;
      if (!key.equals(operation.key)) return false;
      return Objects.equals(value, operation.value);
    }

    @Override
    public int hashCode() {
      int result = type.hashCode();
      result = 31 * result + key.hashCode();
      result = 31 * result + (value != null ? value.hashCode() : 0);
      return result;
    }
  }

  private static class Promise {
    final int vote;
    final Object value;

    Promise(int vote, Object value) {
      this.vote = vote;
      this.value = value;
    }

    Promise(int vote) {
      this(vote, null);
    }
  }
}
