import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun main() {
    // Warmup
    for (i in 0 until 10_000) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
    }
    val cachedFormatWarmup = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    for (i in 0 until 10_000) {
        cachedFormatWarmup.format(Date())
    }

    val iterations = 100_000

    val start1 = System.currentTimeMillis()
    for (i in 0 until iterations) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
    }
    val end1 = System.currentTimeMillis()
    val unCachedTime = end1 - start1

    val cachedFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    val start2 = System.currentTimeMillis()
    for (i in 0 until iterations) {
        val timestamp = cachedFormat.format(Date())
    }
    val end2 = System.currentTimeMillis()
    val cachedTime = end2 - start2

    println("Uncached Time: $unCachedTime ms")
    println("Cached Time: $cachedTime ms")
}
