package org.librecans.protocol

import org.junit.Test
import kotlin.system.measureNanoTime

class BenchmarkTest {
    @Test
    fun benchmarkListCreation() {
        fun benchmarkMap() {
            var sum = 0
            for (i in 0..100000) {
                val list = (0 until 5).map { it }
                sum += list.size
            }
        }

        fun benchmarkList() {
            var sum = 0
            for (i in 0..100000) {
                val list = List(5) { it }
                sum += list.size
            }
        }

        // Warmup
        for (i in 0..100) {
            benchmarkMap()
            benchmarkList()
        }

        var timeMap = 0L
        var timeList = 0L

        for (i in 0..10) {
            timeMap += measureNanoTime { benchmarkMap() }
            timeList += measureNanoTime { benchmarkList() }
        }

        println("BENCHMARK RESULT Map time: ${timeMap / 11} ns")
        println("BENCHMARK RESULT List time: ${timeList / 11} ns")
    }
}
