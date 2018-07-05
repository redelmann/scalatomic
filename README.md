Scalatomic
==========

Scalatomic is a Scala library that proposes an easy to use and *hard to misuse* API to manipulate atomic variables.

Usage
=====

The main data type offered by the library is `Atomic[A]`, which represents atomic variables with values of type `A`. One creates such variables using the `newAtomic` method:

```scala
val myAtomicVar: Atomic[Int] = newAtomic(0)
```

`Atomic` objects always hold a value. Therefore, at creation time, an initial value must be provided.

The value held by an atomic variable can be read using the `read` method:

```scala
val currentValue: Int = myAtomicVar.read
```

Instead of providing a low level instruction such as `compareAndSet`, the library offers the method `modify` to issue modification requests to the atomic variable. The method `onFailure` is called on the result of `modify` to handle failures due to concurrent accesses.

As an example, here is how to specify that we wish to increment by one the value held by `myAtomicVar`, but only if it is currently smaller than 10:

```scala
myAtomicVar modify { (value: Int) =>
    if (value < 10) {
        Update(value + 1)  // Requests the value to be updated.
    }
    else {
        NoUpdate  // No update is requested.
    }
} onFailure {
    retry  // In case of concurrent modification, retry.
}
``` 

It is also possible to return values from the `modify` / `onFailure` calls:

```scala
val result: String = myAtomicVar modify { (value: Int) =>
    if (value < 10) {
        Update(value + 1) withResult "Updated"
    }
    else {
        NoUpdate withResult "Not updated"
    }
} onFailure {
    "Update failed"
}
```
