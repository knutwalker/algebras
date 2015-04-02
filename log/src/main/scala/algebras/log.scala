/*
 * Copyright 2015 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package algebras

import scalaz.Inject

sealed trait LogOp[A]

object LogOp {
  case class Logs(x: String, level: LogLevel, ex: Option[Throwable])
    extends LogOp[Unit]

  type Injects[F[_]] = Inject[LogOp, F]

  implicit def instance[F[_]: Injects]: algebras.Log[F] = new algebras.Log[F]
}

sealed class Log[F[_]: LogOp.Injects] {
  import LogOp._

  def log(s: String, l: LogLevel, ex: Option[Throwable]): Effect[F, Unit] =
    Effect(Logs(s, l, ex))
}
object Log {
  def log[F[_]](s: String, l: LogLevel, ex: Option[Throwable])(implicit F: Log[F]): Effect[F, Unit] =
    F.log(s, l, ex)

  def debug[F[_]: Log](s: String): Effect[F, Unit] =
    log(s, LogLevel.Debug, None)

  def info[F[_]: Log](s: String): Effect[F, Unit] =
    log(s, LogLevel.Info, None)

  def warn[F[_]: Log](s: String): Effect[F, Unit] =
    log(s, LogLevel.Warn, None)

  def error[F[_]: Log](s: String, ex: Throwable): Effect[F, Unit] =
    log(s, LogLevel.Error, Some(ex))
}
