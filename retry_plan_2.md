Ah! The review is absolutely right. I used `launch` inside `withTimeout`, which waits for the `launch` to finish because of structured concurrency, and if it fails, it throws an unhandled exception causing a crash.
To correctly interrupt a blocking `sock.connect()` in Kotlin, I should use `suspendCancellableCoroutine` or just `runInterruptible`, or simply wrap it in `withContext(Dispatchers.IO)` and if it times out, the caller cancels, but `withContext` is still blocking.
Wait, the standard way to interrupt a blocking Bluetooth socket `connect()` is to call `socket.close()` from another thread when the timeout occurs.
If I use `withTimeoutOrNull(1000)` and inside it call `sock.connect()`, it will cancel the coroutine but the blocking `connect()` won't stop unless we close the socket.
The safest way is:
```kotlin
suspendCancellableCoroutine<Unit> { cont ->
    val thread = thread {
        try {
            sock.connect()
            if (cont.isActive) cont.resume(Unit)
        } catch (e: Exception) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    }
    cont.invokeOnCancellation {
        try { sock.close() } catch(e: Exception) {}
        thread.interrupt()
    }
}
```
Or with Coroutines:
```kotlin
suspend fun tryConnectSocket(device: BluetoothDevice, uuidStr: String): BluetoothSocket? {
    val uuid = UUID.fromString(uuidStr)
    var sock: BluetoothSocket? = null
    DebugLogger.log(context, "BluetoothRepository", "tryConnectSocket to UUID $uuidStr")
    return try {
        sock = device.createRfcommSocketToServiceRecord(uuid)
        withTimeout(1000L) {
            suspendCancellableCoroutine<Unit> { cont ->
                val job = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        sock?.connect()
                        if (cont.isActive) cont.resume(Unit)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }
                cont.invokeOnCancellation {
                    try { sock?.close() } catch (e: Exception) {}
                    job.cancel()
                }
            }
        }
        DebugLogger.log(context, "BluetoothRepository", "RFCOMM socket connect successful to UUID $uuidStr")
        sock
    } catch (e: Exception) {
        ...
```
Wait, if I use `CoroutineScope(Dispatchers.IO).launch`, it breaks structured concurrency from the parent (which is exactly what we want so that `withTimeout` can complete immediately when it times out!). The parent `withTimeout` cancels its children. If we use `suspendCancellableCoroutine`, its parent is `withTimeout`. `withTimeout` cancels `suspendCancellableCoroutine`, which invokes `invokeOnCancellation`, which closes the socket. Closing the socket instantly interrupts the blocking `sock.connect()` throwing an `IOException` in the un-structured `job`, which we catch and ignore!

Let's modify `BluetoothRepository.kt`.
