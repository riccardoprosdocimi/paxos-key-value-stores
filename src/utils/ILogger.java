package utils;

/**
 * The interface Logger contains methods that all types of loggers should support.
 */
public interface ILogger {
  /**
   * Logs an event.
   *
   * @param msg the message to be logged
   */
  void log(String msg);

  /**
   * Stops this logger.
   */
  void close();
}
