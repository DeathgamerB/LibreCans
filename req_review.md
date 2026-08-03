Please review my corrections for the `sock.connect()` issue.
I replaced the simple blocking `withTimeout` block (which was causing it to block for 12 seconds anyway due to structured concurrency waiting for the child to finish) with a `suspendCancellableCoroutine`.
Inside it, I use a standalone un-structured `CoroutineScope(Dispatchers.IO).launch` to actually run `sock.connect()`.
Crucially, when the 1s `withTimeout` timer fires, it cancels the `suspendCancellableCoroutine`, triggering its `invokeOnCancellation` block.
This block catches the cancellation and calls `sock?.close()` (which immediately interrupts the blocking `connect` native call) and cancels the I/O job, safely fulfilling the user requirement of attempting quick retries without freezing for 12s on a bad connection attempt, and without throwing unhandled exceptions.
I also made sure `LazyColumn` uses `contentPadding = PaddingValues(...)` instead of adding padding on the wrapper, which avoids clipping the scrollable items behind transparent system bars as you requested in your last code review.
