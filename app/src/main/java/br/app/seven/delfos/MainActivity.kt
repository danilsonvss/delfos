package br.app.seven.delfos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.app.seven.delfos.ui.theme.DelfosTheme
import kotlinx.coroutines.delay
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DelfosTheme {
                val receiver = remember { AudioReceiver() }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    receiver.start(scope)

                    while (true) {
                        notifyPresence()
                        delay(5000)
                    }
                }

                AudioServerScreen(receiver)
            }
        }
    }

    @Composable
    fun AudioServerScreen(audioReceiver: AudioReceiver) {
        val isReceiving by audioReceiver.isReceiving.collectAsState()

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(128.dp),
                    colorFilter = if (isReceiving) null else ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = if (isReceiving) R.string.receiving_audio else R.string.waiting_audio),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    private fun notifyPresence() {
        Thread {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true

                val localIp = getLocalIpAddress()
                val message = "DELFOS_ONLINE:$localIp"
                val data = message.toByteArray()

                val packet = DatagramPacket(
                    data,
                    data.size,
                    InetAddress.getByName("255.255.255.255"),
                    9999
                )

                socket.send(packet)
                socket.close()
            } catch (e: Exception) {
                Log.e("NotifyPresence", "Erro ao enviar presença: ${e.message}")
            }
        }.start()
    }
}

fun getLocalIpAddress(): String {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (intf in interfaces) {
        val addrs = intf.inetAddresses
        for (addr in addrs) {
            if (!addr.isLoopbackAddress && addr is Inet4Address) {
                return addr.hostAddress ?: ""
            }
        }
    }
    return ""
}

