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

import scalaz.{Inject, NonEmptyList}


sealed trait RandomOp[A]

object RandomOp {
  case class NextInt(upperExclusive: Int)
    extends RandomOp[Int]

  type Injects[F[_]] = Inject[RandomOp, F]

  implicit def instance[F[_]: Injects]: Random[F] = new Random[F]
}

sealed class Random[F[_]: RandomOp.Injects] {
  import RandomOp._

  def nextInt(upperExclusive: Int): Effect[F, Int] =
    Effect(NextInt(upperExclusive))
}
object Random {
  def nextInt[F[_]](upperExclusive: Int)(implicit F: Random[F]): Effect[F, Int] =
    F.nextInt(upperExclusive)

  def chooseInt[F[_]: Random](lowerInclusive: Int, upperInclusive: Int): Effect[F, Int] =
    nextInt(upperInclusive - lowerInclusive + 1).map(_ + lowerInclusive)

  def boolean[F[_]: Random]: Effect[F, Boolean] =
    nextInt(2) map (_ == 0)

  def oneOf[A, F[_]: Random](x: A, xs: A*): Effect[F, A] =
    oneOfL[A, F](NonEmptyList(x, xs: _*))

  def oneOfL[A, F[_]: Random](x: NonEmptyList[A]): Effect[F, A] =
    nextInt(x.size) map x.list

  def chance[F[_]: Random](parts: Int, outOf: Int): Effect[F, Boolean] =
    chooseInt(1, outOf).map(_ <= parts)

  def percent[F[_]: Random](p: Int): Effect[F, Boolean] =
    chance(p, 100)

  def choose[A, F[_]: Random](parts: Int, outOf: Int, whenHit: ⇒ A, whenMiss: ⇒ A): Effect[F, A] =
    chance(parts, outOf).map(c ⇒ if (c) whenHit else whenMiss)
}
