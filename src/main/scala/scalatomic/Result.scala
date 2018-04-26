// Copyright Romain Edelmann 2018.

package scalatomic

/** Result of a call to `modify` or `modifyWithVersion`.
 *
 * @group modify
 */
sealed trait Result[R] {

  /** Indicates what to do in case of a failed modification attempt.
   *
   * @param failureHandler The handler.
   * @see retry for retrying the call to modify.
   * @see restart for restarting an entire `restartable` block.
   */
  def onFailure(failureHandler: => R): R = this match {
    case Success(value) => value
    case Failure(retryHandler, _) => try {
      failureHandler
    } catch {
      // In case of a call to `retry` within the failure handler, we
      // use the retry handler stored in the failure object.
      case RetryException => retryHandler().onFailure(failureHandler)
    }
  }

  /** Indicates what to do in case of a failed modification attempt.
   *
   * Passes the intended result as a parameter.
   *
   * @param failureHandler The handler.
   * @see retry for retrying the call to modify.
   * @see restart for restarting an entire `restartable` block.
   */
  def onFailureWithResult(failureHandler: R => R): R = this match {
    case Success(value) => value
    case Failure(retryHandler, candidateValue) => try {
      failureHandler(candidateValue)
    } catch {
      // In case of a call to `retry` within the failure handler, we
      // use the retry handler stored in the failure object.
      case RetryException => retryHandler().onFailureWithResult(failureHandler)
    }
  }

  /** Ignores failed modification attempts and recovers the intended result. */
  def ignoreFailure: R = this match {
    case Success(value) => value
    case Failure(_, candidateValue) => candidateValue
  }
}

/** Indicates a successful call to `modify`. */
private case class Success[R](value: R) extends Result[R]

/** Indicates an unsuccessful call to modify due to a stale read. */
private case class Failure[R](retryHandler: () => Result[R], candidateValue: R) extends Result[R]