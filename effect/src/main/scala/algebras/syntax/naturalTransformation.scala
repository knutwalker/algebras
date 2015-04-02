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

import scalaz.{Coproduct, ~>}

final class NaturalTransformationOps[F[_], G[_]](val self: F ~> G) extends AnyVal {
  def or[H[_]](g: H ~> G): ({type λ[α] = Coproduct[F, H, α]})#λ ~> G =
    new (({type λ[α] = Coproduct[F, H, α]})#λ ~> G) {
      def apply[A](fa: Coproduct[F, H, A]): G[A] =
        fa.run.fold(self.apply, g.apply)
    }
}

trait ToNaturalTransformationOps {
  implicit def ToNaturalTransformationOps[F[_], G[_]](a: F ~> G): NaturalTransformationOps[F, G] = new NaturalTransformationOps(a)
}
