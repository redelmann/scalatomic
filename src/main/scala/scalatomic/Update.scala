// Copyright Romain Edelmann 2018.

package scalatomic

/** Contains both an update to the state of the atomic variable and a result.
 *
 * @group update
 */
trait UpdateAndResult[+A, +R] { self =>

  /** The value to replace the state with, if any. */
  val update: Option[A]

  /** The desired result. */
  val result: R

  /** Records a new desired result.
   *
   * @param result The desired result.
   */
  def withResult[S](value: S): UpdateAndResult[A, S] =
    new UpdateAndResult[A, S] {
      val update = self.update
      val result = value
    }
}

/** Contains the update to the state.
 *
 * @param value The intended new state.
 * @group update
 */
case class Update[A](val value: A) extends UpdateAndResult[A, Unit] {
  override val update = Some(value)
  override val result = ()
}

/** Indicates that no update should be made to the state. *
 *
 * @group update
 */
case object NoUpdate extends UpdateAndResult[Nothing, Unit] {
  override val update = None
  override val result = ()
}