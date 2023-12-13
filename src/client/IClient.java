package client;

/**
 * The interface Client contains methods that all types of RMI clients should support.
 */
public interface IClient {
  /**
   * Gets the user request.
   *
   * @return the user request
   */
  String getRequest();

  /**
   * Pre-populates the key-value store.
   */
  void prePopulate();

  /**
   * Starts this client.
   */
  void execute();

  /**
   * Stops this client and the nodes.
   */
  void shutdown();
}
