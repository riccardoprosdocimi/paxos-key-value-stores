package node;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import utils.ILogger;
import utils.Logger;

/**
 * Implementation of a Node class that represents a node in a Paxos distributed consensus system.
 * This server plays the role of Proposer, Acceptor, and Learner in the Paxos algorithm.
 * It also handles key-value store operations.
 */
public class Node extends UnicastRemoteObject implements IProposer, IAcceptor, ILearner, IKVStore {
  private static final double FAILURE_RATE = 0.10; // 10% failure rate
  private final Random random = new Random();
  private static final AtomicLong SEQ_NUM = new AtomicLong(0);
  private Registry registry;
  private final ConcurrentHashMap<String, String> kvStore;
  private final long nodeId;
  private final int numNodes;
  private long highestSeenSeqNum;
  private long acceptedSeqNum;
  private Object acceptedValue;
  private final ILogger logger;
  private List<IAcceptor> acceptors;
  private List<ILearner> learners;

  /**
   * Constructor to create a Node instance.
   *
   * @param nodeId   the unique ID of this server
   * @param port     the port number of this node
   * @param numNodes the total number of servers in the system
   * @throws RemoteException the RMI failure
   */
  public Node(long nodeId, int port, int numNodes) throws RemoteException {
    this.kvStore = new ConcurrentHashMap<>();
    this.nodeId = nodeId;
    this.numNodes = numNodes;
    this.highestSeenSeqNum = -Long.MAX_VALUE;
    this.acceptedSeqNum = -Long.MAX_VALUE;
    String loggerName = "Node" + nodeId + "Logger";
    String logFileName = "Node" + nodeId + "Log.log";
    this.logger = new Logger(loggerName, logFileName);
    this.logger.log(this + " is ready at port " + port);
  }

  /**
   * Sets the acceptors for this node.
   *
   * @param acceptors the array of acceptors
   */
  public void setAcceptors(List<IAcceptor> acceptors) {
    this.acceptors = acceptors;
  }

  /**
   * Sets the learners for this node.
   *
   * @param learners the array of learners
   */
  public void setLearners(List<ILearner> learners) {
    this.learners = learners;
  }

  /**
   * Retrieves the value of a key.
   *
   * @param key the word to be translated
   * @return the translation
   * @throws RemoteException the RMI failure
   */
  @Override
  public synchronized String get(String key) throws RemoteException {
    if (this.kvStore.containsKey(key)) {
      this.logger.log("SUCCESS: returned the value \"" + key + "\" associated with \"" + key + "\"");
    } else { // if the key doesn't exist, just return null
      this.logger.log("FAIL: no value is associated with \"" + key + "\"");
    }
    return this.kvStore.get(key);
  }

  /**
   * Saves a key-value pair in a hashmap.
   *
   * @param key   the word to be translated
   * @param value the translation
   * @return the outcome of the operation
   * @throws RemoteException the RMI failure
   */
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

  /**
   * Removes a key-value pair.
   *
   * @param key the word to be deleted
   * @return the outcome of the operation
   * @throws RemoteException the RMI failure
   */
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
   * Applies the given operation to the key-value store.
   *
   * @param operation the operation to be applied
   */
  private void applyOperation(Operation operation) {
    if (operation == null) return;
    switch (operation.type) {
      case "PUT":
        if (this.kvStore.containsKey(operation.key)) {
          this.logger.log("FAIL: the translation for \"" + operation.key + "\" already exists");
        } else {
          this.kvStore.put(operation.key, operation.value);
          this.logger.log("SUCCESS: added the key \"" + operation.key + "\" associated with \"" + operation.value + "\"");
          break;
        }
      case "DELETE":
        if (this.kvStore.containsKey(operation.key)) {
          this.kvStore.remove(operation.key);
          this.logger.log("SUCCESS: deleted the key-value pair associated with \"" + operation.key + "\"");
          break;
        } else {
          this.logger.log("FAIL: " + "\"" + operation.key + "\" does not exist");
        }
      default: // should never get here
        this.logger.log("FAIL: unknown operation type: " + operation.type);
        throw new IllegalArgumentException("Unknown operation type: " + operation.type);
    }
  }

  /**
   * Initiates the Paxos algorithm with the given proposed operation and generated sequence number.
   *
   * @param operation the operation
   * @throws RemoteException the RMI failure
   */
  private boolean propose(Operation operation) throws RemoteException {
    this.highestSeenSeqNum = generateSequenceNumber();
    this.logger.log("Sequence number generated: " + this.highestSeenSeqNum);
    return executePaxos(this.highestSeenSeqNum, operation);
  }

  /**
   * Sends a 'promise' message to the proposer promising to accept the proposal with the given unique sequence number.
   *
   * @param sequenceNumber the unique sequence number of the proposal
   * @return an integer response indicating the status or decision related to the proposal (1 meaning the proposal has been accepted or 0 meaning the proposal has been rejected) or the previously accepted value and sequence number
   * @throws RemoteException the RMI failure
   */
  @Override
  public synchronized Object prepare(long sequenceNumber) throws RemoteException {
    // Implement Paxos prepare logic here
    double random = this.random.nextDouble();
    if (random < FAILURE_RATE) { // simulate node's failure
      this.logger.log(this + " failed (" + random + " < " + FAILURE_RATE + ")");
      throw new RemoteException(this + " failed");
    } else {
      if (sequenceNumber > this.highestSeenSeqNum) { // proposal sequence number is the highest seen so far
        if (this.acceptedValue != null) { // already accepted a value
          this.logger.log("Already accepted " + this.acceptedValue + " associated with the sequence number " + this.acceptedSeqNum);
          return new Promise(1, this.acceptedSeqNum, this.acceptedValue);
        } else {
          this.logger.log("Sent promise message (" + sequenceNumber + " > " + this.highestSeenSeqNum + ")");
          this.highestSeenSeqNum = sequenceNumber;
          return new Promise(1);
        }
      } else {
        this.logger.log("Did not send promise message (" + sequenceNumber + " <= " + this.highestSeenSeqNum + ")");
        return new Promise(0);
      }
    }
  }

  /**
   * Accepts or rejects the value promised to before.
   *
   * @param sequenceNumber the unique sequence number of the proposal
   * @param proposalValue  the value of the proposal
   * @return the sequence number associated with the accepted value
   * @throws RemoteException the RMI failure
   */
  @Override
  public synchronized long accept(long sequenceNumber, Object proposalValue) throws RemoteException {
    // Implement Paxos accept logic here
    double random = this.random.nextDouble();
    if (random < FAILURE_RATE) { // simulate node's failure
      this.logger.log(this + " failed (" + random + " < " + FAILURE_RATE + ")");
      throw new RemoteException(this + " failed");
    } else {
      if (sequenceNumber >= this.highestSeenSeqNum) {
        this.logger.log(proposalValue + " associated with sequence number " + sequenceNumber + " accepted");
        this.acceptedValue = proposalValue;
        this.highestSeenSeqNum = sequenceNumber;
        this.acceptedSeqNum = sequenceNumber;
      } else {
        this.logger.log(proposalValue + " associated with sequence number " + sequenceNumber + " rejected");
      }
    }
    return this.highestSeenSeqNum;
  }

  /**
   * The learn method is used to inform the Learner of an accepted proposal.
   *
   * @param sequenceNumber the unique identifier for the proposal
   * @param acceptedValue  the value that has been accepted
   * @throws RemoteException the RMI failure
   */
  @Override
  public synchronized void learn(long sequenceNumber, Object acceptedValue) throws RemoteException {
    // Implement Paxos learn logic here
    this.applyOperation((Operation) acceptedValue);
    this.logger.log(acceptedValue.toString() + " learned and executed");
    this.acceptedValue = null;
    this.highestSeenSeqNum = sequenceNumber;
    this.acceptedSeqNum = -Long.MAX_VALUE;
  }

  /**
   * Executes the Paxos algorithm with the given sequence number and proposal value.
   *
   * @param sequenceNumber the unique sequence number for the proposal
   * @param proposalValue  the value being proposed
   * @return whether this proposer has successfully committed the proposal value
   * @throws RemoteException the RMI failure
   */
  @Override
  public synchronized boolean executePaxos(long sequenceNumber, Object proposalValue) throws RemoteException {
    // Send propose/prepare messages
    int majorityThreshold = this.numNodes / 2;
    int promisesCount = 0;
    Map<Long, Object> alreadyAcceptedValues = new HashMap<>(this.numNodes);
    for (IAcceptor acceptor : this.acceptors) { // gather promises
      try {
        Promise promise = (Promise) acceptor.prepare(sequenceNumber);
        if (promise.value != null) { // acceptor already accepted anther proposal value
          alreadyAcceptedValues.put(promise.sequenceNumber, promise.value);
        }
        promisesCount += promise.vote;
      } catch (RemoteException e) {
        this.logger.log(acceptor + " failed during the propose phase");
      }
    }
    if (!alreadyAcceptedValues.isEmpty()) { // at least one acceptor has already accepted a value
      this.acceptedValue = alreadyAcceptedValues.get(Collections.max(alreadyAcceptedValues.keySet())); // set the proposal value to the one associated with the highest sequence number
    } else { // acceptors promised to accept the proposal value
      this.acceptedValue = proposalValue;
    }
    if (promisesCount > majorityThreshold) { // check if majority is reached
      // Send accept messages
      this.logger.log("Proposal accepted with value " + this.acceptedValue.toString() + ": able to extract a majority of promises (" + promisesCount + " / " + this.numNodes + ")");
      int acceptsCount = 0;
      for (IAcceptor acceptor : this.acceptors) { // gather votes
        try {
          if (acceptor.accept(sequenceNumber, this.acceptedValue) <= sequenceNumber) { // if the response contains a sequence number less than or equal to the proposal sequence number
            acceptsCount += 1; // the request has been accepted
          } // if the response contains a sequence number greater than the proposal sequence number, then the request has been rejected
        } catch (RemoteException e) {
          this.logger.log(acceptor + " failed during the accept phase");
        }
      }
      if (acceptsCount > majorityThreshold) { // check if majority accepted the value
        for (ILearner learner : this.learners) { // learner role integrated into the proposer process
          try {
            learner.learn(sequenceNumber, this.acceptedValue); // broadcast the sequence number and its associated value
          } catch (RemoteException e) {
            this.logger.log(learner + " failed during the learn phase");
          }
        }
        this.logger.log("Accepted value " + this.acceptedValue.toString() + " has been saved: accept message accepted by the majority (" + acceptsCount + " / " + this.numNodes + ")");
        this.applyOperation((Operation) this.acceptedValue);
        return true;
      } else {
        this.logger.log("Accepted value " + this.acceptedValue.toString() + " has been rejected: accept message rejected by the majority (" + acceptsCount + " / " + this.numNodes + ")");
        return false;
      }
    } else {
      this.logger.log("Proposal aborted: couldn't extract a majority of promises (" + promisesCount + " / " + this.numNodes + ")");
      return false;
    }
  }

  @Override
  public String toString() {
    return "Node{" +
            "nodeId=" + nodeId +
            '}';
  }

  /**
   * Sets this node's registry.
   *
   * @param registry the registry
   */
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  /**
   * Stops this node.
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
   * The Operation is a static class representing an operation on the key-value store.
   */
  private static class Operation {
    final String type;
    final String key;
    final String value;

    /**
     * Instantiates a new Operation.
     *
     * @param type  the type
     * @param key   the key
     * @param value the value
     */
    Operation(String type, String key, String value) {
      this.type = type;
      this.key = key;
      this.value = value;
    }

    /**
     * Instantiates a new Operation.
     *
     * @param type the type
     * @param key  the key
     */
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
  }

  /**
   * The Promise is a static class representing a promise message.
   */
  private static class Promise {
    final int vote;
    final Long sequenceNumber;
    final Object value;

    /**
     * Instantiates a new Promise.
     *
     * @param vote           the vote
     * @param sequenceNumber the sequence number
     * @param value          the value
     */
    Promise(int vote, Long sequenceNumber, Object value) {
      this.vote = vote;
      this.sequenceNumber = sequenceNumber;
      this.value = value;
    }

    /**
     * Instantiates a new Promise.
     *
     * @param vote the vote
     */
    Promise(int vote) {
      this(vote, null, null);
    }
  }
}
