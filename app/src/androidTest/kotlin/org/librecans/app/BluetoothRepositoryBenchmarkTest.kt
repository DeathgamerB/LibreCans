package org.librecans.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import kotlin.system.measureTimeMillis

class BluetoothRepositoryBenchmarkTest {
    @Test
    fun benchmarkSetAmbientMode() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val repo = BluetoothRepository(application)

        repo.toggleMockMode(true)

        // Warm up
        for (i in 1..10) {
            repo.setAmbientMode(controlOn = true, level = 10, focusVoice = false, sendWrite = false)
        }

        Thread.sleep(1000) // Wait for debounce and coroutines

        val time = measureTimeMillis {
            for (i in 1..100) {
                repo.setAmbientMode(controlOn = true, level = 10, focusVoice = false, sendWrite = false)
            }
        }

        println("BENCHMARK_RESULT: SetAmbientMode took $time ms for 100 iterations")

        // Give time for remaining jobs
        Thread.sleep(1000)
    }
}
