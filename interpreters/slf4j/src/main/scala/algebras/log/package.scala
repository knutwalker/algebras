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

import algebras.LogLevel.{Debug, Error, Info, Warn}
import algebras.LogOp.Logs

import scalaz._
import effect.IO

import org.slf4j.{Logger, LoggerFactory}

package object log {
  private val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

  val Interpreter: LogOp ~> IO = new (LogOp ~> IO) {
    def apply[A](fa: LogOp[A]): IO[A] = fa match {
      case Logs(x, Debug, Some(ex))     ⇒ IO(logger.debug(x, ex))
      case Logs(x, Debug, None)         ⇒ IO(logger.debug(x))
      case Logs(x, Info, Some(ex))      ⇒ IO(logger.info(x, ex))
      case Logs(x, Info, None)          ⇒ IO(logger.info(x))
      case Logs(x, Warn, Some(ex))      ⇒ IO(logger.warn(x, ex))
      case Logs(x, Warn, None)          ⇒ IO(logger.warn(x))
      case Logs(x, Error, Some(ex))     ⇒ IO(logger.error(x, ex))
      case Logs(x, Error, None)         ⇒ IO(logger.error(x))
    }
  }
}
