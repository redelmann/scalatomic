// Copyright Romain Edelmann 2018.

package scalatomic

import java.util.concurrent.atomic.AtomicReference

/** Atomic variable holding a reference to an object of type `A`. */
private class AtomicVariable[A](initial: A) extends Atomic[A] {

  /** The atomic reference holding the state. */
  private val reference = new AtomicReference(initial)

  override def modify[R](body: A => UpdateAndResult[A, R]): Result[R] = {

    // Wrap the loop in a named function so that we can return from it.
    def retryLoop(): (A, UpdateAndResult[A, R]) = {

      while (true) {
        // Reads the atomic reference.
        val currentValue = reference.get()

        // Apply the body to get the (optional) new value of the variable,
        // and the desired return value.
        try {
          return (currentValue, body(currentValue))
        } catch {
          // In case of a call to retry,
          // We continue with the loop.
          case RetryException =>
        }
      }

      throw new IllegalStateException("Unreachable state.")
    }

    val (currentValue, updateAndResult) = retryLoop()

    updateAndResult.update match {
      // The state is requested to be modified.
      case Some(newValue) => {

        // We try to atomically modify it, ensuring that the value read
        // to compute the new state is not stale.
        if (reference.compareAndSet(currentValue, newValue)) {
          // Modification successful.
          Success(updateAndResult.result)
        }
        else {
          // The value we read to compute the new state was stale.
          Failure(() => modify(body), updateAndResult.result)
        }
      }
      case None => {
        // No modifications to do, we always succeed.
        Success(updateAndResult.result)
      }
    }
  }

  override def read: A = reference.get()
}


/** Versioned atomic variable holding a reference to an object of type `A` and its version. */
private class VersionedAtomicVariable[A](initial: A) extends VersionedAtomic[A] {

  /** The atomic reference holding the state. */
  private val reference = new AtomicReference((initial, 0: Version))

  def modifyWithVersion[R](body: (A, Version) => UpdateAndResult[A, R]): Result[R] = {

    // Wrap the loop in a named function so that we can return from it.
    def retryLoop(): ((A, Version), UpdateAndResult[A, R]) = {

      while (true) {
        // Reads the atomic reference.
        val currentPair@(currentValue, currentVersion) = reference.get()

        // Apply the body to get the (optional) new value of the variable,
        // and the desired return value.
        try {
          return (currentPair, body(currentValue, currentVersion))
        } catch {
          // In case of a call to retry,
          // We continue with the loop.
          case RetryException =>
        }
      }

      throw new IllegalStateException("Unreachable state.")
    }

    val (currentPair, updateAndResult) = retryLoop()

    updateAndResult.update match {
      // The state is requested to be modified.
      case Some(newValue) => {

        // We try to atomically modify it, ensuring that the value read
        // to compute the new state is not stale.
        if (reference.compareAndSet(currentPair, (newValue, currentPair._2 + 1))) {
          // Modification successful.
          Success(updateAndResult.result)
        }
        else {
          // The value we read to compute the new state was stale.
          Failure(() => modifyWithVersion(body), updateAndResult.result)
        }
      }
      case None => {
        // No modifications to do, we always succeed.
        Success(updateAndResult.result)
      }
    }
  }

  override def modify[R](body: A => UpdateAndResult[A, R]): Result[R] =
    modifyWithVersion((value: A, version: Version) => body(value))

  override def readWithVersion: (A, Version) = reference.get()
}