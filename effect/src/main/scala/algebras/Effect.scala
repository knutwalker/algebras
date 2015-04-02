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

import scalaz._
import Free._
import Leibniz._


sealed trait Effect[F[_], A] {

  def free: FreeC[F, A]

  def map[B](f: A ⇒ B): Effect[F, B] =
    Effect(free map f)

  def as[B](f: ⇒ B): Effect[F, B] =
    >|(f)

  def >|[B](f: ⇒ B): Effect[F, B] =
    map(_ ⇒ f)

  def flatMap[B](f: A ⇒ Effect[F, B]): Effect[F, B] =
    Effect(free flatMap (f(_).free))

  def >>=[B](f: A ⇒ Effect[F, B]): Effect[F, B] =
    flatMap(f)

  def andThen[B](f: ⇒ Effect[F, B]): Effect[F, B] =
    >>(f)

  def >>[B](f: ⇒ Effect[F, B]): Effect[F, B] =
    flatMap(_ ⇒ f)

  def ap[X](f: Effect[F, A ⇒ X]): Effect[F, X] =
    f.flatMap(ff ⇒ map(aa ⇒ ff(aa)))

  def zip[X](q: Effect[F, X]): Effect[F, (A, X)] =
    zipWith(q)(a ⇒ (a, _))

  def zipWith[B, C](r: Effect[F, B])(f: A ⇒ B ⇒ C): Effect[F, C] =
    r.ap(map(f))

  def |+|[AA >: A](x: Effect[F, AA])(implicit S: Semigroup[AA]): Effect[F, AA] =
    flatMap(a ⇒ x.map(b ⇒ S.append(a, b)))

  def ***[X](x: Effect[F, X]): Effect[F, (A, X)] =
    zip(x)

  def flatten[AA >: A, B](implicit f: AA === Effect[F, B]): Effect[F, B] =
    flatMap(f)

  def runM[M[_]](f: F ~> M)(implicit M: Monad[M]): M[A] =
    run[M](f, M)

  def run[M[_]](implicit f: F ~> M, M: Monad[M]): M[A] =
    Free.runFC(free)(f)(M)
}
object Effect {
  private[algebras] def apply[F[_], A](f: FreeC[F, A]): Effect[F, A] =
    new Effect[F, A] { val free = f }

  def apply[F[_], G[_], A](fa: F[A])(implicit I: Inject[F, G]): Effect[G, A] =
    apply(Free.liftFC(I.inj(fa)))

  def point[F[_], A](a: ⇒ A): Effect[F, A] =
    apply(Free.point[({type λ[α] = Coyoneda[F, α]})#λ, A](a))

  def unit[F[_]]: Effect[F, Unit] =
    point(())

  def sequence[F[_], T[_], A](x: T[Effect[F, A]])(implicit T: Traverse[T]): Effect[F, T[A]] =
    T.sequence[({type λ[α] = Effect[F, α]})#λ, A](x)

  def pair[F[_], A, B](a: Effect[F, A], b: Effect[F, B]): Effect[F, (A, B)] =
    a zip b

  def triple[F[_], A, B, C](a: Effect[F, A], b: Effect[F, B], c: Effect[F, C]): Effect[F, (A, B, C)] =
    for {
      aa <- a
      bb <- b
      cc <- c
    } yield (aa, bb, cc)

  // =====================

  implicit def AlgebraMonad[F[_]]: Monad[({type λ[α] = Effect[F, α]})#λ] =
    new Monad[({type λ[α] = Effect[F, α]})#λ] {

      def point[A](a: ⇒ A): Effect[F, A] =
        Effect.point(a)

      def bind[A, B](fa: Effect[F, A])(f: A ⇒ Effect[F, B]): Effect[F, B] =
        fa flatMap f

      override def map[A, B](fa: Effect[F, A])(f: A ⇒ B): Effect[F, B] =
        fa map f

      override def ap[A, B](fa: ⇒ Effect[F, A])(f: ⇒ Effect[F, A ⇒ B]): Effect[F, B] =
        fa ap f
    }

  implicit def AlgebraSemigroup[F[_], A: Semigroup]: Semigroup[Effect[F, A]] =
    new Semigroup[Effect[F, A]] {

      def append(f1: Effect[F, A], f2: ⇒ Effect[F, A]): Effect[F, A] =
        f1 |+| f2
    }

  implicit def AlgebraMonoid[F[_], A: Monoid]: Monoid[Effect[F, A]] =
    new Monoid[Effect[F, A]] {

      def zero: Effect[F, A] =
        point(Monoid[A].zero)

      def append(f1: Effect[F, A], f2: ⇒ Effect[F, A]): Effect[F, A] =
        f1 |+| f2
    }
}
