package org.librecans.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.librecans.protocol.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothRepository(private val context: Context) {
    private val TAG = "BluetoothRepository"

    private val AMBIENT_PREFS_NAME = "librecans_ambient_prefs"
    private val KEY_ASM_LEVEL = "ambient_sound_level"
    private val KEY_FOCUS_ON_VOICE = "focus_on_voice"

    private val _deviceState = MutableStateFlow(BluetoothDeviceState())
    val deviceState: StateFlow<BluetoothDeviceState> = _deviceState.asStateFlow()

    init {
        val prefs = context.getSharedPreferences(AMBIENT_PREFS_NAME, Context.MODE_PRIVATE)
        val storedLevel = prefs.getInt(KEY_ASM_LEVEL, 20)
        val storedFocus = prefs.getBoolean(KEY_FOCUS_ON_VOICE, false)
        _deviceState.value = BluetoothDeviceState(
            asmLevel = storedLevel,
            focusOnVoice = storedFocus
        )
    }

    private var socket: BluetoothSocket? = null
    private var isConnected = false
    private var seqNumber: Byte = 0
    private var ioJob: Job? = null
    private var pollJob: Job? = null
    private var ambientDebounceJob: Job? = null
    private var eqDebounceJob: Job? = null
    @Volatile private var lastAmbientUpdate: Long = 0
    private val scope = CoroutineScope(DispatchScope + SupervisorJob())

    // Cache of successful protocol versions per MAC address
    private val protocolCache = ConcurrentHashMap<String, SonyProtocolVersion>()

    // For sending commands safely (thread-safe synchronization)
    private val ioMutex = Any()

    // Temporary storage for responses we are waiting for (command serialization)
    private val pendingResponses = ConcurrentHashMap<Byte, CompletableDeferred<ByteArray>>()
    private val pendingSubtypeResponses = ConcurrentHashMap<String, CompletableDeferred<ByteArray>>()

    // Mock mode toggle for previewing in emulators without real hardware
    var isMockMode = false
        private set

    companion object {
        private val DispatchScope = Dispatchers.IO
    }

    fun toggleMockMode(enabled: Boolean) {
        isMockMode = enabled
        if (enabled) {
            disconnect()
            _deviceState.value = BluetoothDeviceState(
                connected = true,
                deviceName = "WH-CH720N (Mock)",
                deviceMac = "00:11:22:33:44:55",
                protocolVersion = SonyProtocolVersion.V2,
                ambientSoundControl = true,
                asmLevel = 20,
                focusOnVoice = false,
                focusOnVoiceAvailable = true,
                batteryLevel = 85,
                batteryCharging = false,
                hasDualBattery = false,
                eqPreset = EqPreset.OFF,
                eqClearBass = 2,
                eqBands = listOf(0, 1, 2, 1, -1),
                dsee = true,
                hasAutoPowerOff = true,
                autoPowerOff = 2,
                hasFirmware = true,
                firmware = "1.0.8",
                hasCodec = true,
                codec = "AAC",
                hasSpeakToChat = true,
                speakToChat = false,
                hasAdaptiveVolume = true,
                adaptiveVolume = false
            )
        } else {
            disconnect()
        }
    }

    fun getPairedDevices(): List<android.bluetooth.BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun connect(macAddress: String) {
        if (isMockMode) return
        disconnect()

        val prefs = context.getSharedPreferences(AMBIENT_PREFS_NAME, Context.MODE_PRIVATE)
        val storedLevel = prefs.getInt(KEY_ASM_LEVEL, 20)
        val storedFocus = prefs.getBoolean(KEY_FOCUS_ON_VOICE, false)

        _deviceState.value = BluetoothDeviceState(
            connecting = true,
            deviceMac = macAddress,
            asmLevel = storedLevel,
            focusOnVoice = storedFocus
        )

        scope.launch {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                updateError("Bluetooth is not supported on this device.")
                return@launch
            }
            if (!adapter.isEnabled) {
                updateError("Bluetooth is turned off.")
                return@launch
            }

            val device = try {
                adapter.getRemoteDevice(macAddress)
            } catch (e: Exception) {
                updateError("Invalid Bluetooth MAC address: $macAddress")
                return@launch
            }

            val name = device.name ?: "Sony Headset"
            Log.d(TAG, "Attempting to connect to device: $name ($macAddress)")
            DebugLogger.log(context, "BluetoothRepository", "Attempting to connect to device: $name ($macAddress)")

            // Check protocol cache
            val cachedVersion = protocolCache[macAddress]
            var connectedSocket: BluetoothSocket? = null
            var usedVersion: SonyProtocolVersion? = null

            val startTime = System.currentTimeMillis()
            var attempt = 1
            while (connectedSocket == null && (System.currentTimeMillis() - startTime) < 15000) {
                Log.d(TAG, "Connection attempt $attempt...")
                DebugLogger.log(context, "BluetoothRepository", "Connection attempt $attempt for $macAddress")

                if (cachedVersion != null) {
                    Log.d(TAG, "Trying cached protocol version: $cachedVersion")
                    val uuidStr = if (cachedVersion == SonyProtocolVersion.V2) Constants.SERVICE_UUID_V2 else Constants.SERVICE_UUID
                    connectedSocket = tryConnectSocket(device, uuidStr)
                    if (connectedSocket != null) {
                        usedVersion = cachedVersion
                    }
                }

                if (connectedSocket == null) {
                    Log.d(TAG, "No cache or cached connection failed. Trying V1 UUID...")
                    connectedSocket = tryConnectSocket(device, Constants.SERVICE_UUID)
                    if (connectedSocket != null) {
                        usedVersion = SonyProtocolVersion.V1
                    } else {
                        Log.d(TAG, "V1 UUID connection failed. Trying V2 UUID...")
                        connectedSocket = tryConnectSocket(device, Constants.SERVICE_UUID_V2)
                        if (connectedSocket != null) {
                            usedVersion = SonyProtocolVersion.V2
                        }
                    }
                }

                if (connectedSocket == null) {
                    attempt++
                    delay(1500) // Wait 1.5s before retrying
                }
            }

            if (connectedSocket != null && usedVersion != null) {
                Log.d(TAG, "Successfully connected via protocol: $usedVersion")
                DebugLogger.log(context, "BluetoothRepository", "Successfully connected via protocol: $usedVersion")
                protocolCache[macAddress] = usedVersion
                socket = connectedSocket
                isConnected = true
                seqNumber = 0

                _deviceState.value = BluetoothDeviceState(
                    connected = true,
                    deviceName = name,
                    deviceMac = macAddress,
                    protocolVersion = usedVersion
                )

                // Start I/O loop to read incoming frames
                startIoLoop(connectedSocket.inputStream, connectedSocket.outputStream)

                // Perform handshakes and probe capabilities
                try {
                    initializeConnectedDevice(usedVersion)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize device parameters: ${e.message}")
                }

                // Start active polling loop to sync controls back (e.g. NC level, EQ)
                startPollingLoop(usedVersion)

            } else {
                DebugLogger.log(context, "BluetoothRepository", "Failed to connect to headphones.")
                _deviceState.value = BluetoothDeviceState(connecting = false, errorMessage = "Failed to connect. Tap to try again.")
            }
        }
    }

    private suspend fun tryConnectSocket(device: BluetoothDevice, uuidStr: String): BluetoothSocket? {
        val uuid = UUID.fromString(uuidStr)
        var sock: BluetoothSocket? = null
        DebugLogger.log(context, "BluetoothRepository", "tryConnectSocket to UUID $uuidStr")
        return try {
            sock = device.createRfcommSocketToServiceRecord(uuid)
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(1000) {
                    suspendCancellableCoroutine<Unit> { cont ->
                        cont.invokeOnCancellation {
                            try {
                                sock.close()
                            } catch (e: IOException) {
                                // Ignore
                            }
                        }

                        try {
                            sock.connect()
                            if (cont.isActive) {
                                cont.resumeWith(Result.success(Unit))
                            }
                        } catch (e: IOException) {
                            if (cont.isActive) {
                                cont.resumeWith(Result.failure(e))
                            }
                        }
                    }
                }
            } ?: throw IOException("Socket connection timed out")

            DebugLogger.log(context, "BluetoothRepository", "RFCOMM socket connect successful to UUID $uuidStr")
            sock
        } catch (e: Exception) {
            Log.w(TAG, "Connection failed to UUID $uuidStr: ${e.message}")
            DebugLogger.log(context, "BluetoothRepository", "Connection failed to UUID $uuidStr: ${e.message}")
            try {
                sock?.close()
            } catch (closeEx: IOException) {
                // Ignore
            }
            null
        }
    }

    private fun startIoLoop(inputStream: InputStream, outputStream: OutputStream) {
        ioJob = scope.launch(Dispatchers.IO) {
            val buffer = java.io.ByteArrayOutputStream()
            var ongoing = false
            try {
                while (isActive) {
                    val b = inputStream.read()
                    if (b == -1) break
                    val byteVal = b.toByte()

                    if (byteVal == Constants.START_MARKER) {
                        ongoing = true
                        buffer.reset()
                    } else if (byteVal == Constants.END_MARKER) {
                        if (ongoing) {
                            ongoing = false
                            val framedBytes = buffer.toByteArray()
                            try {
                                val msg = CommandSerializer.unpackBtMessage(framedBytes)
                                handleIncomingMessage(msg, outputStream)
                            } catch (e: Exception) {
                                Log.e(TAG, "Unpacking error: ${e.message}")
                            }
                        }
                    } else if (ongoing) {
                        buffer.write(b)
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "Socket read error / connection dropped: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    onConnectionDropped()
                }
            }
        }
    }

    private fun handleIncomingMessage(msg: CommandSerializer.Message, outputStream: OutputStream) {
        // If it is a DATA_MDR message, send an ACK back
        if (msg.dataType == DataType.DATA_MDR) {
            sendRawWithStream(outputStream, CommandSerializer.packageDataForBt(
                byteArrayOf(),
                DataType.ACK,
                (1 - msg.seqNumber).toByte()
            ))
        }

        if (msg.dataType == DataType.ACK) {
            seqNumber = msg.seqNumber
            return
        }

        // Process actual payloads
        if (msg.dataType == DataType.DATA_MDR && msg.payload.isNotEmpty()) {
            val op = msg.payload[0]
            val sub = if (msg.payload.size > 1) msg.payload[1] else -1.toByte()

            Log.d(TAG, "Recv DATA_MDR: op=0x${String.format("%02X", op)}, sub=0x${String.format("%02X", sub)}")
            val payloadHex = msg.payload.joinToString("") { String.format("%02X ", it) }
            DebugLogger.log(context, "BluetoothRepository", "Received DATA_MDR: op=0x${String.format("%02X", op)} sub=0x${String.format("%02X", sub)} | payload hex: $payloadHex")

            // Complete any pending deferables
            val keySub = "0x${String.format("%02X", op)}_0x${String.format("%02X", sub)}"
            val deferredSub = pendingSubtypeResponses.remove(keySub)
            if (deferredSub != null) {
                deferredSub.complete(msg.payload)
            } else {
                val deferredOp = pendingResponses.remove(op)
                deferredOp?.complete(msg.payload)
            }

            // Sync values back (live sync from physical button or direct polling)
            parseStateUpdate(msg.payload)
        }
    }

    private fun parseStateUpdate(payload: ByteArray) {
        if (payload.isEmpty()) return
        val op = payload[0]

        when (op) {
            0x67.toByte() -> { // NCASM STATE RET
                // Ignore incoming status updates that arrive shortly after an optimistic change
                if (System.currentTimeMillis() - lastAmbientUpdate < 1500L) {
                    return
                }
                // layout: 67 17 01 <effect> <settingType 0=NC/1=Ambient> <voice> <level>
                val state = _deviceState.value
                val isV2 = state.protocolVersion == SonyProtocolVersion.V2
                if (isV2 && payload.size >= 7) {
                    val on = payload[3] != 0.toByte()
                    val ambient = payload[4] != 0.toByte()
                    val voice = payload[5] != 0.toByte()
                    val level = payload[6].toInt() and 0xFF
                    _deviceState.value = state.copy(
                        ambientSoundControl = on,
                        asmLevel = if (ambient) level else 0,
                        focusOnVoice = voice
                    )
                }
            }
            V2Command.BATTERY_RET -> { // BATTERY RET
                val sub = if (payload.size > 1) payload[1] else -1.toByte()
                val state = _deviceState.value
                when (sub) {
                    0x00.toByte() -> { // Single Battery
                        if (payload.size >= 4) {
                            val level = payload[2].toInt() and 0xFF
                            val charging = payload[3] == 1.toByte()
                            _deviceState.value = state.copy(
                                batteryLevel = level,
                                batteryCharging = charging,
                                hasDualBattery = false
                            )
                        }
                    }
                    0x09.toByte() -> { // TWS Left/Right
                        if (payload.size >= 6) {
                            val left = payload[2].toInt() and 0xFF
                            val leftChg = payload[3] == 1.toByte()
                            val right = payload[4].toInt() and 0xFF
                            val rightChg = payload[5] == 1.toByte()
                            _deviceState.value = state.copy(
                                hasDualBattery = true,
                                batteryLeft = left,
                                batteryRight = right,
                                batteryLevel = left.coerceAtMost(right),
                                batteryCharging = leftChg || rightChg
                            )
                        }
                    }
                    0x0a.toByte() -> { // TWS Case
                        if (payload.size >= 4) {
                            val caseLvl = payload[2].toInt() and 0xFF
                            _deviceState.value = state.copy(batteryCase = caseLvl)
                        }
                    }
                }
            }
            V2Command.EQ_RET -> { // EQ RET
                if (payload.size >= 3) {
                    val presetByte = payload[2]
                    val state = _deviceState.value
                    var clearBass = state.eqClearBass
                    var bands = state.eqBands

                    if (payload.size >= 10) {
                        clearBass = (payload[4].toInt() and 0xFF) - 10
                        bands = (0 until 5).map { i ->
                            (payload[5 + i].toInt() and 0xFF) - 10
                        }
                    }

                    _deviceState.value = state.copy(
                        eqPreset = EqPreset.fromByte(presetByte),
                        eqClearBass = clearBass,
                        eqBands = bands
                    )
                }
            }
            V2Command.DSEE_RET -> { // DSEE RET
                if (payload.size >= 3) {
                    val enabled = payload[2] != 0.toByte()
                    _deviceState.value = _deviceState.value.copy(dsee = enabled)
                }
            }
            V2Command.FW_RET -> { // FW RET
                if (payload.size > 3) {
                    val fwStr = String(payload.copyOfRange(3, payload.size), Charsets.US_ASCII).trim()
                    _deviceState.value = _deviceState.value.copy(
                        firmware = fwStr,
                        hasFirmware = true
                    )
                }
            }
            V2Command.CODEC_RET -> { // CODEC RET
                if (payload.size >= 3) {
                    val name = when (payload[2].toInt() and 0xFF) {
                        0x01 -> "SBC"
                        0x02 -> "AAC"
                        0x10 -> "LDAC"
                        0x20 -> "aptX"
                        0x21 -> "aptX HD"
                        else -> "Unknown"
                    }
                    _deviceState.value = _deviceState.value.copy(
                        codec = name,
                        hasCodec = true
                    )
                }
            }
            V2Command.APO_RET -> { // APO RET
                if (payload.size >= 4) {
                    val c0 = payload[2].toInt() and 0xFF
                    val c1 = payload[3].toInt() and 0xFF
                    val index = when {
                        c0 == 0x11 && c1 == 0x00 -> 0
                        c0 == 0x00 && c1 == 0x00 -> 1
                        c0 == 0x01 && c1 == 0x01 -> 2
                        c0 == 0x02 && c1 == 0x02 -> 3
                        c0 == 0x03 && c1 == 0x03 -> 4
                        c0 == 0x10 && c1 == 0x00 -> 5
                        else -> 0
                    }
                    _deviceState.value = _deviceState.value.copy(
                        autoPowerOff = index,
                        hasAutoPowerOff = true
                    )
                }
            }
            V2Command.BTNMODE_RET -> { // BTNMODE RET
                if (payload.size >= 3) {
                    val sub = payload[1]
                    val enabled = payload[2] == 0.toByte() // Inverted enable bit on v2
                    if (sub == V2Command.SUB_ADAPTIVE_VOLUME) {
                        _deviceState.value = _deviceState.value.copy(
                            adaptiveVolume = enabled,
                            hasAdaptiveVolume = true
                        )
                    } else if (sub == V2Command.SUB_SPEAK_TO_CHAT) {
                        _deviceState.value = _deviceState.value.copy(
                            speakToChat = enabled,
                            hasSpeakToChat = true
                        )
                    }
                }
            }
        }
    }

    private suspend fun initializeConnectedDevice(version: SonyProtocolVersion) {
        if (version == SonyProtocolVersion.V2) {
            // Send V2 INIT handshake
            sendCommandAndReadResponse(byteArrayOf(V2Command.INIT_REQUEST, 0x00), V2Command.INIT_REPLY)

            // Read core properties (Battery, EQ, DSEE)
            try {
                // Try TWS dual battery first
                sendCommandAndReadResponse(byteArrayOf(V2Command.BATTERY_GET, 0x09), V2Command.BATTERY_RET, 0x09.toByte())
                sendCommandAndReadResponse(byteArrayOf(V2Command.BATTERY_GET, 0x0a), V2Command.BATTERY_RET, 0x0a.toByte())
            } catch (e: Exception) {
                // Fallback to single battery
                try {
                    sendCommandAndReadResponse(byteArrayOf(V2Command.BATTERY_GET, 0x00), V2Command.BATTERY_RET, 0x00.toByte())
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed battery query: ${e2.message}")
                }
            }

            try {
                sendCommandAndReadResponse(byteArrayOf(V2Command.EQ_GET, 0x00), V2Command.EQ_RET)
            } catch (e: Exception) {
                Log.w(TAG, "Failed EQ query: ${e.message}")
            }

            try {
                sendCommandAndReadResponse(byteArrayOf(V2Command.DSEE_GET, 0x01), V2Command.DSEE_RET)
            } catch (e: Exception) {
                Log.w(TAG, "Failed DSEE query: ${e.message}")
            }

            // Actively query current V2 ambient/NC state immediately upon successful connection
            try {
                sendCommandAndReadResponse(byteArrayOf(0x66, 0x17), 0x67)
            } catch (e: Exception) {
                Log.w(TAG, "Failed initial V2 ambient check: ${e.message}")
            }

            // Probe capability-gated extras with timeouts
            probeCapabilities()
        } else {
            // V1 device handshake: Read ambient state to keep connection alive early
            try {
                sendCommandAndReadResponse(byteArrayOf(0x66, 0x02), 0x67)
            } catch (e: Exception) {
                Log.w(TAG, "Failed V1 initial ambient check: ${e.message}")
            }
        }
    }

    private suspend fun probeCapabilities() {
        withContext(Dispatchers.IO) {
            // Query FW
            try {
                withTimeout(1500) {
                    sendCommandAndReadResponse(byteArrayOf(V2Command.FW_GET, 0x02), V2Command.FW_RET)
                }
            } catch (e: Exception) { Log.d(TAG, "APO/FW probe timeout/fail") }

            // Query Codec
            try {
                withTimeout(1500) {
                    sendCommandAndReadResponse(byteArrayOf(V2Command.CODEC_GET, 0x02), V2Command.CODEC_RET)
                }
            } catch (e: Exception) { Log.d(TAG, "Codec probe timeout/fail") }

            // Query APO
            try {
                withTimeout(1500) {
                    sendCommandAndReadResponse(byteArrayOf(V2Command.APO_GET, 0x05), V2Command.APO_RET)
                }
            } catch (e: Exception) { Log.d(TAG, "APO probe timeout/fail") }

            // Query Adaptive Volume
            try {
                withTimeout(1500) {
                    sendCommandAndReadResponse(
                        byteArrayOf(V2Command.BTNMODE_GET, V2Command.SUB_ADAPTIVE_VOLUME),
                        V2Command.BTNMODE_RET,
                        V2Command.SUB_ADAPTIVE_VOLUME
                    )
                }
            } catch (e: Exception) { Log.d(TAG, "Adaptive Volume probe timeout/fail") }

            // Query Speak to Chat
            try {
                withTimeout(1500) {
                    sendCommandAndReadResponse(
                        byteArrayOf(V2Command.BTNMODE_GET, V2Command.SUB_SPEAK_TO_CHAT),
                        V2Command.BTNMODE_RET,
                        V2Command.SUB_SPEAK_TO_CHAT
                    )
                }
            } catch (e: Exception) { Log.d(TAG, "Speak-to-Chat probe timeout/fail") }
        }
    }

    private fun startPollingLoop(version: SonyProtocolVersion) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                delay(2000)
                try {
                    if (version == SonyProtocolVersion.V2) {
                        // GET ambient/NC: 66 17
                        sendCommandAndReadResponse(byteArrayOf(0x66, 0x17), 0x67)
                    } else {
                        // GET v1: 66 02 (to keep socket open, do not overwrite state from reply)
                        sendCommandAndReadResponse(byteArrayOf(0x66, 0x02), 0x67)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Periodic poll failed: ${e.message}")
                }
            }
        }
    }

    private fun sendRaw(bytes: ByteArray) {
        if (isMockMode) return
        val outStream = socket?.outputStream ?: return
        sendRawWithStream(outStream, bytes)
    }

    private fun sendRawWithStream(stream: OutputStream, bytes: ByteArray) {
        try {
            synchronized(ioMutex) {
                stream.write(bytes)
                stream.flush()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write raw bytes: ${e.message}")
        }
    }

    fun sendCommand(bytes: ByteArray) {
        if (isMockMode) {
            // Mock state changes
            simulateMockChange(bytes)
            return
        }
        val outStream = socket?.outputStream ?: return
        scope.launch(Dispatchers.IO) {
            synchronized(ioMutex) {
                val packed = CommandSerializer.packageDataForBt(bytes, DataType.DATA_MDR, seqNumber++)
                try {
                    outStream.write(packed)
                    outStream.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "Send error: ${e.message}")
                }
            }
        }
    }

    private suspend fun sendCommandSuspend(bytes: ByteArray) {
        val outStream = socket?.outputStream ?: throw IOException("Socket is closed")
        synchronized(ioMutex) {
            val packed = CommandSerializer.packageDataForBt(bytes, DataType.DATA_MDR, seqNumber++)
            val hexStr = packed.joinToString("") { String.format("%02X ", it) }
            Log.d(TAG, "Writing packed frame to BluetoothSocket: size=${packed.size} bytes, frame hex=$hexStr")

            val payloadHex = bytes.joinToString("") { String.format("%02X ", it) }
            DebugLogger.log(context, "BluetoothRepository", "Writing command payload (size=${bytes.size} bytes) hex: $payloadHex | packed frame hex: $hexStr")

            outStream.write(packed)
            outStream.flush()
        }
    }

    private fun updateStateOptimistically(
        update: (BluetoothDeviceState) -> BluetoothDeviceState,
        bytes: ByteArray,
        sendWrite: Boolean = true
    ) {
        val prevState = _deviceState.value
        _deviceState.value = update(prevState)

        if (isMockMode) {
            if (sendWrite) simulateMockChange(bytes)
            return
        }

        if (socket == null || !isConnected) {
            _deviceState.value = prevState
            return
        }

        if (!sendWrite) return

        scope.launch(Dispatchers.IO) {
            try {
                sendCommandSuspend(bytes)
            } catch (e: Exception) {
                Log.e(TAG, "Optimistic send failed, reverting state: ${e.message}")
                withContext(Dispatchers.Main) {
                    _deviceState.value = prevState
                }
            }
        }
    }

    private suspend fun sendCommandAndReadResponse(
        bytes: ByteArray,
        retCommandId: Byte,
        retSubType: Byte? = null
    ): ByteArray {
        val deferred = CompletableDeferred<ByteArray>()
        if (retSubType != null) {
            val keySub = "0x${String.format("%02X", retCommandId)}_0x${String.format("%02X", retSubType)}"
            pendingSubtypeResponses[keySub] = deferred
        } else {
            pendingResponses[retCommandId] = deferred
        }

        // Send command
        sendCommand(bytes)

        // Wait for deferred response with 3.5s timeout
        return withTimeout(3500) {
            deferred.await()
        }
    }

    private fun simulateMockChange(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val op = bytes[0]
        val state = _deviceState.value
        when (op) {
            CommandType.NCASM_SET_PARAM.value -> {
                // v2 layout: 0x68 | 0x17 | 0x01 | [effect] | [settingType] | [voice] | [level]
                if (bytes.size >= 7 && bytes[1] == 0x17.toByte()) {
                    val effectOn = bytes[3] != 0.toByte()
                    val isAmbient = bytes[4] == 1.toByte()
                    val voiceOn = bytes[5] == 1.toByte()
                    val level = bytes[6].toInt() and 0xFF
                    _deviceState.value = state.copy(
                        ambientSoundControl = effectOn,
                        asmLevel = if (isAmbient) level else 0,
                        focusOnVoice = voiceOn
                    )
                }
            }
            V2Command.EQ_SET -> {
                // custom: 58 00 A0 06 <bass+10> <b1..b5 +10>
                // preset: 58 00 <preset> 00
                if (bytes.size >= 4) {
                    val preset = EqPreset.fromByte(bytes[2])
                    if (preset == EqPreset.MANUAL && bytes.size >= 10) {
                        val clearBass = (bytes[4].toInt() and 0xFF) - 10
                        val bands = (0 until 5).map { i ->
                            (bytes[5 + i].toInt() and 0xFF) - 10
                        }
                        _deviceState.value = state.copy(
                            eqPreset = EqPreset.MANUAL,
                            eqClearBass = clearBass,
                            eqBands = bands
                        )
                    } else {
                        _deviceState.value = state.copy(eqPreset = preset)
                    }
                }
            }
            V2Command.DSEE_SET -> {
                if (bytes.size >= 3) {
                    _deviceState.value = state.copy(dsee = bytes[2] == 1.toByte())
                }
            }
            V2Command.APO_SET -> {
                if (bytes.size >= 4) {
                    val c0 = bytes[2].toInt() and 0xFF
                    val c1 = bytes[3].toInt() and 0xFF
                    val index = when {
                        c0 == 0x11 && c1 == 0x00 -> 0
                        c0 == 0x00 && c1 == 0x00 -> 1
                        c0 == 0x01 && c1 == 0x01 -> 2
                        c0 == 0x02 && c1 == 0x02 -> 3
                        c0 == 0x03 && c1 == 0x03 -> 4
                        c0 == 0x10 && c1 == 0x00 -> 5
                        else -> 0
                    }
                    _deviceState.value = state.copy(autoPowerOff = index)
                }
            }
            V2Command.BTNMODE_SET -> {
                if (bytes.size >= 4) {
                    val sub = bytes[1]
                    val enabled = bytes[2] == 0.toByte()
                    if (sub == V2Command.SUB_ADAPTIVE_VOLUME) {
                        _deviceState.value = state.copy(adaptiveVolume = enabled)
                    } else if (sub == V2Command.SUB_SPEAK_TO_CHAT) {
                        _deviceState.value = state.copy(speakToChat = enabled)
                    }
                }
            }
        }
    }

    // --- Control API methods ---

    fun setAmbientMode(controlOn: Boolean, level: Int, focusVoice: Boolean, sendWrite: Boolean = true) {
        ambientDebounceJob?.cancel()
        ambientDebounceJob = scope.launch(Dispatchers.IO) {
            lastAmbientUpdate = System.currentTimeMillis()
            delay(150)
            val state = _deviceState.value
            val usedLevel = if (level > 0) level else 1

            if (controlOn && level > 0) {
                val prefs = context.getSharedPreferences(AMBIENT_PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt(KEY_ASM_LEVEL, level)
                    .putBoolean(KEY_FOCUS_ON_VOICE, focusVoice)
                    .apply()
            }

            val cmd = if (state.protocolVersion == SonyProtocolVersion.V2 || isMockMode) {
                CommandSerializer.serializeNcAndAsmSettingV2(
                    if (controlOn) NcAsmEffect.ON else NcAsmEffect.OFF,
                    if (level > 0) NcAsmSettingTypeV2.AMBIENT_SOUND else NcAsmSettingTypeV2.NOISE_CANCELLING,
                    if (focusVoice) AsmId.VOICE else AsmId.NORMAL,
                    usedLevel.toByte()
                )
            } else {
                // V1 Protocol
                CommandSerializer.serializeNcAndAsmSetting(
                    if (controlOn) NcAsmEffect.ADJUSTMENT_COMPLETION else NcAsmEffect.OFF,
                    NcAsmSettingType.LEVEL_ADJUSTMENT,
                    AsmSettingType.LEVEL_ADJUSTMENT,
                    if (focusVoice) AsmId.VOICE else AsmId.NORMAL,
                    (if (controlOn) level else -1).toByte()
                )
            }

            updateStateOptimistically(
                update = { current ->
                    current.copy(
                        ambientSoundControl = controlOn,
                        asmLevel = if (controlOn) level else 0,
                        focusOnVoice = focusVoice
                    )
                },
                bytes = cmd
            )
        }
    }

    fun setEqualizerPreset(preset: EqPreset) {
        scope.launch(Dispatchers.IO) {
        val isManual = preset == EqPreset.MANUAL ||
                       preset == EqPreset.CUSTOM_1 ||
                       preset == EqPreset.CUSTOM_2

        if (isManual) {
            val out = java.io.ByteArrayOutputStream()
            out.write(V2Command.EQ_SET.toInt())
            out.write(0x00)

            val targetPreset = preset
            out.write(targetPreset.value.toInt())
            out.write(0x06)

            // Clear Bass: range -10..10, offset 10 (bytes 0..20)
            val clearBassByte = (_deviceState.value.eqClearBass.coerceIn(-10, 10) + 10).toByte()
            out.write(clearBassByte.toInt())

            // 5 bands: range -10..10, offset 10 (bytes 0..20)
            for (i in 0 until 5) {
                val bVal = if (i < _deviceState.value.eqBands.size) _deviceState.value.eqBands[i] else 0
                val bandByte = (bVal.coerceIn(-10, 10) + 10).toByte()
                out.write(bandByte.toInt())
            }

            val packedBytes = out.toByteArray()
            updateStateOptimistically(
                update = { current ->
                    current.copy(eqPreset = preset)
                },
                bytes = packedBytes
            )
        } else {
            val bytes = byteArrayOf(
                V2Command.EQ_SET,
                0x00.toByte(),
                preset.value,
                0x00.toByte()
            )
            updateStateOptimistically(
                update = { current ->
                    current.copy(eqPreset = preset)
                },
                bytes = bytes
            )
        }
            }
    }

    fun setEqualizerCustom(clearBass: Int, bands: List<Int>, sendWrite: Boolean = true) {
        eqDebounceJob?.cancel()
        eqDebounceJob = scope.launch(Dispatchers.IO) {
            delay(150)
            val out = java.io.ByteArrayOutputStream()
            out.write(V2Command.EQ_SET.toInt())
            out.write(0x00)

            val currentPreset = _deviceState.value.eqPreset
            val presetToSend = if (currentPreset == EqPreset.CUSTOM_1 || currentPreset == EqPreset.CUSTOM_2) {
                currentPreset
            } else {
                EqPreset.MANUAL
            }

            out.write(presetToSend.value.toInt())
            out.write(0x06)

            // Clear Bass: range -10..10, offset 10 (bytes 0..20)
            val clearBassByte = (clearBass.coerceIn(-10, 10) + 10).toByte()
            out.write(clearBassByte.toInt())

            // 5 bands: range -10..10, offset 10 (bytes 0..20)
            for (i in 0 until 5) {
                val bVal = if (i < bands.size) bands[i] else 0
                val bandByte = (bVal.coerceIn(-10, 10) + 10).toByte()
                out.write(bandByte.toInt())
            }

            val packedBytes = out.toByteArray()
            val payloadHex = buildString(packedBytes.size * 3) {
                val hexChars = "0123456789ABCDEF"
                for (b in packedBytes) {
                    val v = b.toInt() and 0xFF
                    append(hexChars[v ushr 4])
                    append(hexChars[v and 0x0F])
                    append(' ')
                }
            }
            Log.d(TAG, "setEqualizerCustom: clearBass=$clearBass, bands=$bands -> packing command payload hex=$payloadHex")

            val targetBands = (0 until 5).map { i -> if (i < bands.size) bands[i].coerceIn(-10, 10) else 0 }

            updateStateOptimistically(
                update = { current ->
                    current.copy(
                        eqPreset = if (currentPreset == EqPreset.CUSTOM_1 || currentPreset == EqPreset.CUSTOM_2) currentPreset else EqPreset.MANUAL,
                        eqClearBass = clearBass.coerceIn(-10, 10),
                        eqBands = targetBands
                    )
                },
                bytes = packedBytes
            )
        }
    }

    fun setDsee(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
        val bytes = byteArrayOf(
            V2Command.DSEE_SET,
            0x01.toByte(),
            if (enabled) 0x01.toByte() else 0x00.toByte()
        )
        updateStateOptimistically(
            update = { current ->
                current.copy(dsee = enabled)
            },
            bytes = bytes
        )
            }
    }

    fun setAutoPowerOff(index: Int) {
        scope.launch(Dispatchers.IO) {
        if (index !in 0..5) return@launch
        val codes = arrayOf(
            Pair(0x11, 0x00), // Off
            Pair(0x00, 0x00), // 5 min
            Pair(0x01, 0x01), // 30 min
            Pair(0x02, 0x02), // 1 hr
            Pair(0x03, 0x03), // 3 hr
            Pair(0x10, 0x00)  // When taken off
        )
        val code = codes[index]
        val bytes = byteArrayOf(
            V2Command.APO_SET,
            0x05.toByte(),
            code.first.toByte(),
            code.second.toByte()
        )
        updateStateOptimistically(
            update = { current ->
                current.copy(autoPowerOff = index)
            },
            bytes = bytes
        )
            }
    }

    fun setSpeakToChat(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
        // SET f8 0c <enabled? 0:1> 01 (inverted bit on v2)
        val bytes = byteArrayOf(
            V2Command.BTNMODE_SET,
            V2Command.SUB_SPEAK_TO_CHAT,
            (if (enabled) 0x00 else 0x01).toByte(),
            0x01.toByte()
        )
        updateStateOptimistically(
            update = { current ->
                current.copy(speakToChat = enabled)
            },
            bytes = bytes
        )
            }
    }

    fun setAdaptiveVolume(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
        // SET f8 0a <enabled? 0:1> (inverted)
        val bytes = byteArrayOf(
            V2Command.BTNMODE_SET,
            V2Command.SUB_ADAPTIVE_VOLUME,
            (if (enabled) 0x00 else 0x01).toByte()
        )
        updateStateOptimistically(
            update = { current ->
                current.copy(adaptiveVolume = enabled)
            },
            bytes = bytes
        )
            }
    }

    private fun updateError(msg: String) {
        _deviceState.value = BluetoothDeviceState(errorMessage = msg)
    }

    private fun onConnectionDropped() {
        isConnected = false
        socket = null
        pollJob?.cancel()
        DebugLogger.log(context, "BluetoothRepository", "Connection dropped / socket closed.")
        _deviceState.value = BluetoothDeviceState(errorMessage = "Headphones disconnected.")
    }

    fun disconnect() {
        if (isMockMode) {
            _deviceState.value = BluetoothDeviceState()
            return
        }
        isConnected = false
        pollJob?.cancel()
        ioJob?.cancel()
        ambientDebounceJob?.cancel()
        eqDebounceJob?.cancel()
        DebugLogger.log(context, "BluetoothRepository", "Explicit disconnect() requested.")
        try {
            socket?.close()
        } catch (e: Exception) {
            // ignore
        }
        socket = null
        _deviceState.value = BluetoothDeviceState()
    }
}
