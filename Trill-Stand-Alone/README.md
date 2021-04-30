# BISON
(tntokum/tyto8880's networking branch)

## Documentation
This is mainly to keep track of how to handle live data in Trill/Rx. There's a lot of annoying little intricacies, such as the `Observable`/`Observer` interaction, how threading interacts with `async`, etc.

### Observables
Some background covering `Main()` in `TrillBI.cs`:

Trill requires an `Observable` to provide data to `Streamable`.

Enumerable sequences (i.e. `Array`, `List`) can be easily converted to `IObservable` with `ToObservable()`, but we have live data incoming from a socket. Even if we modify the output buffer per connection, we'd need to call `ToObservable()` on that buffer, which gets very messy. Therefore, we construct a custom `Observable` using `Observable.Create()`.

Here's a quick rundown:
- `Observable.Create()` returns `Observable`.
- `Observable` implements `IObservable`, and therefore requires a `Subscribe()` method.
- `Subscribe()` is called when the `Observable` is subscribed to.
  + Calling `observable.Subscribe((x) -> y)` (note lowercase observable denotes an instance)
    tells the `Observable` to run the given function on each new value of the sequence
- We pass a definition for the `Subscribe()` method to `Create()`
- Return a `Disposable`
  + `Disposable` implements `IDisposable`, which requires a `Dispose()` method

In the case where we have an infinite loop (like a socket server), we don't need to `Dispose()` anything else, so we just print if we `Dispose()`.

### Observers
Observers inform Observables of incoming data via the `OnNext()` when giving input, `OnError()` if an error has occurred, and `OnComplete()` when the input sequence is terminated. With this model, we can call `OnNext()` for every value of our incoming data.

### Threading
We never want to lock the main thread when ingressing live data, so we should either be looking towards a multi-threaded program or an asynchronous program. `async` is probably not the right move since it seems the entire thread will be locked if a certain `await` is reached (i.e. when egressing from the `Streamable`), so a multi-threaded model would suit us better. It also turns out that this allows us to "complete" the main thread while keeping the program running with the worker thread (more research will be needed here into what's actually happening).

We load the worker with, at the very least, a method that calls `OnNext(x)` for some new data `x`. Since we're looking into streaming over network, we may also load the worker with a socket server, which is what we've done in this case. The server runs in a normal `while` loop, and upon receiving data on the specified IP and port, we call `observable.OnNext(data)` to inform our stream that we have new data (Note: we initialize and ingress from this stream in `Main` using `Select`).