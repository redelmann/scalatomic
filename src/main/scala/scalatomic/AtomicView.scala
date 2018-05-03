// Copyright Romain Edelmann 2018.

package scalatomic

/** View of a subpart of an atomic variable.
 *
 * @param inner   The underlying atomic variable.
 * @param zoomIn  Indicates how to retrieve the value from the context.
 * @param zoomOut Indicates how to rebuild the context with an updated value.
 */
private class AtomicView[A, B](inner: Atomic[A], zoomIn: A => B, zoomOut: (A, B) => A) extends Atomic[B] {

  override def modify[R](body: B => UpdateAndResult[B, R]): Result[R] =
    inner.modify { (value: A) =>
      val updateAndResult = body(zoomIn(value))

      new UpdateAndResult[A, R] {
        val update = updateAndResult.update.map(zoomOut(value, _))
        val result = updateAndResult.result
      }
    }

  override def read: B = zoomIn(inner.read)
}

/** View of a subpart of an atomic variable, with versioning.
 *
 * @param inner   The underlying atomic variable.
 * @param zoomIn  Indicates how to retrieve the value from the context.
 * @param zoomOut Indicates how to rebuild the context with an updated value.
 */
private class VersionedAtomicView[A, B](inner: VersionedAtomic[A], zoomIn: A => B, zoomOut: (A, B) => A)
  extends AtomicView[A, B](inner, zoomIn, zoomOut) with VersionedAtomic[B] {

  override def modifyWithVersion[R](body: (B, Version) => UpdateAndResult[B, R]): Result[R] =
    inner.modifyWithVersion { (value: A, version: Version) =>
      val updateAndResult = body(zoomIn(value), version)

      new UpdateAndResult[A, R] {
        val update = updateAndResult.update.map(zoomOut(value, _))
        val result = updateAndResult.result
      }
    }

  override def readVersion: Version = inner.readVersion

  override def readWithVersion: (B, Version) = {
    val (value, version) = inner.readWithVersion
    (zoomIn(value), version)
  }
}
