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

import algebras.RandomOp.NextInt

import scalaz._
import effect.IO

import com.nicta.rng.Rng

package object random {

  val Interpreter: RandomOp ~> IO = new (RandomOp ~> IO) {
    def apply[A](fa: RandomOp[A]): IO[A] = fa match {
      case NextInt(u) ⇒ Rng.chooseint(0, u - 1).run
    }
  }
}
