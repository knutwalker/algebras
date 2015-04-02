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

package algebras.syntax

import algebras.Effect

final class EffectOps[A](val self: A) extends AnyVal {
  def effect[F[_]]: Effect[F, A] =
    Effect.point(self)
}

trait ToEffectOps {
  implicit def ToEffectOps[A](a: A): EffectOps[A] = new EffectOps(a)
}
