// Copyright Romain Edelmann 2018.

package scalatomic

/** Atomically modifiable state.
 *
 * @group atomic
 */
trait Atomic[A] {

  /** Atomically modifies the value stored based the current value.
   *
   * Due to concurrently executing modifications, the operation might fail.
   * Please use the methods of the `Result` class to indicate what to do in this case.
   *
   * @param body The function computing the updated value.
   * @return Either a value of type `B`, or a failure.
   *
   * @group mod_ops
   */
  def modify[R](body: A => UpdateAndResult[A, R]): Result[R]

  /** Reads the value stored.
   *
   * @group read_ops
   */
  def read: A

  /** Views the variable through a lens.
   *
   * @param zoomIn  Indicates how to retrieve the value from the context.
   * @param zoomOut Indicates how to rebuild the context with an updated value.
   *
   * @group view
   */
  def view[B](zoomIn: A => B)(zoomOut: (A, B) => A): Atomic[B] =
    new AtomicView(this, zoomIn, zoomOut)
}


/** Atomically modifiable state, with versioning.
 *
 * @group atomic
 */
trait VersionedAtomic[A] extends Atomic[A] {

  /** Atomically modifies the value stored based on the current value and version.
   *
   * Due to concurrently executing modifications, the operation might fail.
   * Please use the methods of the `Result` class to indicate what to do in this case.
   *
   * @param body The function computing the updated value.
   * @return Either a value of type `B`, or a failure.
   *
   * @group mod_ops
   */
  def modifyWithVersion[R](body: (A, Version) => UpdateAndResult[A, R]): Result[R]

  /** Atomically reads the value and its version.
   *
   * @group read_ops
   */
  def readWithVersion: (A, Version)

  /** Reads the version of the value stored.
   *
   * @group read_ops
   */
  def readVersion: Version = readWithVersion._2

  override def read: A = readWithVersion._1

  override def view[B](zoomIn: A => B)(zoomOut: (A, B) => A): VersionedAtomic[B] =
    new VersionedAtomicView(this, zoomIn, zoomOut)
}