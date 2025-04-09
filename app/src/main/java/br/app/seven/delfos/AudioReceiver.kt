package br.app.seven.delfos

import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.DatagramPacket
import java.net.DatagramSocket

class AudioReceiver {
    private val _isReceiving = MutableStateFlow(false)
    val isReceiving: StateFlow<Boolean> = _isReceiving.asStateFlow()

    private var receiveJob: Job? = null

    fun start(scope: CoroutineScope) {
        receiveJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket(9999)
                val buffer = ByteArray(2048)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(44100)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()

                var resetJob: Job? = null

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    _isReceiving.value = true
                    audioTrack.write(packet.data, 0, packet.length)

                    resetJob?.cancel()
                    resetJob = launch {
                        delay(1000)
                        _isReceiving.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioReceiver", "Erro: ${e.message}", e)
                _isReceiving.value = false
            }
        }
    }
}
