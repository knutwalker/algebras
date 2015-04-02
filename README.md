# Algebras

Reusable, composable, reasonably priced algebras for typeful effects and composable applications.

## Install

From maven central

```
libraryDependencies += "de.knutwalker" %% "algebra-effect" % "0.1.0"
```

`Algebras` only depend on `scalaz-core`.

## Available modules

### Effect

`"de.knutwalker" %% "algebra-effect" % "0.1.0"`

`algebras.Effect` is a placeholder for the resulting effect type.
It is basically a wrapper around `scalaz.Free` and provides typical combinators like `map` and `flatMap`.


The `algebra-effect` module also contains syntax for arbitrary types that add an `effect[F[_]]` method to
some `A` to lift a value into an `Effect`.

### Log

`"de.knutwalker" %% "algebra-log" % "0.1.0"`

`algebras.Log` prodives an algebra for simple logging (`debug`, `info`, `warn`, `error`)

### Random

`"de.knutwalker" %% "algebra-random" % "0.1.0"`

`algebras.Random` prodives an very simple algebra for random number generation.

The basic method is `nextInt(n)` which returns an int in \[0, n) – i.e. n is the upper exclusive bound.
This is conform with `scala.util.Random.nextInt(Int)`

There are some additional methods, like `chooseInt(a, b)` which return an in in \[a, b] and `oneOf` which takes a variable number of `A`s and returns one of these `A`s.


## Using Effect

Any method, that produces an effect using an algebra must be paramterized in a type `F[_]`
with a context bound for the algebra that this method uses. The return type is `Effect[F, A]` where `A` is the methods actual return type.

At the end of the universe, you have a composed `Effect[F, A]` that you need to run.

To do so, first you have to combine all used algebras (their Ops, actually) into a super-algebra using `scalaz.Coproduct`
Then, you write interpreters for each algebra and combine those into an interpreter for the super-algebra.
An interpreter is a `scala.~>` for the algebra op and some monad.
All interpreters must use the same result monad.

Finally, run `effect.runM(interpreter)` to turn your `Effect[F, A]` into an `M[A]`.

Here's an example. Suppose we want to roll a dice...

```scala
import algebras._, Algebras._

import scalaz.{~>, Coproduct, Id}, Id.Id

object pureCore {

  def roll[F[_]: Random]: Effect[F, Int] =
    Random.chooseInt(1, 6)

  def play[F[_]: Random : Log]: Effect[F, Int] = for {
    _   ← Log.debug("rolling a dice...")
    num ← roll
    _   ← Log.debug(s"rolled a $num")
    _   ← if (num == 6) Log.info("Yay! Rolled a 6!")
          else          ().effect[F]
  } yield num
}

object edgeOfTheWorld {

  type App[A] = Coproduct[RandomOp, LogOp, A]

  val RandomInterpreter = new (RandomOp ~> Id) {
    def apply[A](fa: RandomOp[A]): A = fa match {
      case RandomOp.NextInt(n) ⇒ scala.util.Random.nextInt(n)
    }
  }

  val LogInterpreter = new (LogOp ~> Id) {
    def apply[A](fa: LogOp[A]): A = fa match {
      case LogOp.Logs(msg, level, _) ⇒ println(s"[$level] $msg")
    }
  }

  val AppInterpreter: App ~> Id = RandomInterpreter or LogInterpreter
  val program: Effect[App, Int] = pureCore.play[App]

  def main(args: Array[String]): Unit = {
    program.runM(AppInterpreter)
  }
}
```


## Interpreters

There are two basic interpreters already available.

`algebra-interpreter-rng` which uses [NICTA/rng](https://github.com/NICTA/rng) for the random number generation.
`algebra-interpreter-slf4j` which uses slf4j (duh) to log the messages. 


Using these interpreters, the example above could be rewritten as

```scala
import algebras._, Algebras._

import scalaz.{~>, Coproduct, effect}, effect.IO

object pureCore {

  def roll[F[_]: Random]: Effect[F, Int] =
    Random.chooseInt(1, 6)

  def play[F[_]: Random : Log]: Effect[F, Int] = for {
    _   ← Log.debug("rolling a dice...")
    num ← roll
    _   ← Log.debug(s"rolled a $num")
    _   ← if (num == 6) Log.info("Yay! Rolled a 6!")
          else          ().effect[F]
  } yield num
}

object edgeOfTheWorld {

  type App[A] = Coproduct[RandomOp, LogOp, A]
  val AppInterpreter: App ~> IO = random.Interpreter or log.Interpreter
  val program: Effect[App, Int] = pureCore.play[App]

  def main(args: Array[String]): Unit = {
    program.runM(AppInterpreter).unsafePerformIO()
  }
}
```

## Acknowledgement

Based on https://www.parleys.com/tutorial/composable-application-architecture-reasonably-priced-monads 

## License

Apache 2
