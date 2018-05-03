// Copyright Romain Edelmann 2018.


/** User friendly interface for manipulating atomic state.
 *
 * ==Basic usage==
 *
 * This example showcases how to create and modify an atomic variable in the most basic case.
 *
 * {{{
 *   val myState = newAtomic("Hello")  // Create a new atomic variable.
 *
 *   myState modify { (currentValue) =>
 *     if (currentValue contains "World") {
 *       NoUpdate  // No need to update the state.
 *     }
 *     else {
 *       Update(currentValue + " World!")  // Indicate that we want to update the state.
 *     }
 *   } onFailure {
 *     // The operation might fail due to a concurrent modification.
 *     retry  // In this case, retries the call to modify.
 *   }
 * }}}
 *
 * @groupname atomic Atomic variables
 * @groupprio atomic 1
 * @groupdesc atomic Functions and types related to atomic variables.
 *
 * @groupname modify Atomic modifications
 * @groupprio modify 2
 * @groupdesc modify Functions and related types for modifying atomic variables.
 *
 * @groupname update Intended updates to atomic variables
 * @groupprio update 3
 * @groupdesc update Functions and types for indicating intented changes to the state
 *                   and optional associated results.
 *
 * @groupname restart Restartable computations
 * @groupprio restart 4
 * @groupdesc restart Control flow functions for restarting computations.
 *
 * @groupname read_ops Atomic read operations
 * @groupprio read_ops 1
 *
 * @groupname mod_ops Atomic modify operations
 * @groupprio mod_ops 2
 *
 * @groupname view Views
 * @groupprio view 3
 *
 */
package object scalatomic {

  /** Version number of atomic states.
   *
   * Only tracked when an atomic variable is versioned.
   *
   * @group atomic
   */
  type Version = Long

  /** Retries the associated `modify` or `modifyWithVersion` operation.
   *
   * Should only be called within `modify`, `modifyWithVersion`,
   * or within the associated `onFailure` or `onFailureWithResult` call.
   *
   * @group restart
   */
  def retry: Nothing = throw RetryException

  /** Restarts the execution of the closest restartable block.
   *
   * @see restartable for how to declare a restartable block.
   * @group restart
   */
  def restart: Nothing = throw RestartException

  /** Indicates that the block can be restarted.
   *
   * @param body The restartable block.
   * @see the function `restart`, which is use to restart the execution of the block.
   * @group restart
   */
  def restartable[A](body: => A): A = {
    while (true) {
      try {
        return body
      }
      catch {
        case RestartException => ()
      }
    }

    // We should never reach here.
    throw new IllegalStateException("Unreachable.")
  }

  /** Creates a new atomic variable.
   *
   * @param initial   The initial value stored.
   * @group atomic
   */
  def newAtomic[A](initial: A): Atomic[A] = new AtomicVariable(initial)

  /** Creates a new atomic variable, with versioning.
   *
   * @param initial   The initial value stored.
   * @group atomic
   */
  def newVersionedAtomic[A](initial: A): VersionedAtomic[A] = new VersionedAtomicVariable(initial)

  /** Atomically modifies the value stored in the atomic state based its current value.
   *
   * Due to concurrently executing modifications, the operation might fail.
   * Please use the methods of the `Result` class to indicate what to do in this case.
   *
   * @param body The function computing the updated value.
   * @return Either a value of type `B`, or a failure.
   *
   * @group modify
   */
  def modify[A, R](variable: Atomic[A])
      (body: A => UpdateAndResult[A, R]): Result[R] =
    variable.modify(body)

  /** Atomically modifies the value stored in the atomic state based its current value
   *  and its version number.
   *
   * Due to concurrently executing modifications, the operation might fail.
   * Please use the methods of the `Result` class to indicate what to do in this case.
   *
   * Unsupported by unversioned atomic variables.
   *
   * @param body The function computing the updated value.
   * @return Either a value of type `B`, or a failure.
   *
   * @group modify
   */
  def modifyWithVersion[A, R](variable: VersionedAtomic[A])
      (body: (A, Version) => UpdateAndResult[A, R]): Result[R] =
    variable.modifyWithVersion(body)
}