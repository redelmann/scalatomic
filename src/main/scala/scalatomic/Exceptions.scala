// Copyright Romain Edelmann 2018.

package scalatomic

import scala.util.control.ControlThrowable

/** Control exception thrown by calls to `retry`. */
private case object RetryException extends ControlThrowable

/** Control exception thrown by calls to `restart`. */
private case object RestartException extends ControlThrowable