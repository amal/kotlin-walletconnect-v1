package io.walletconnect.example

import android.app.Application
import com.squareup.moshi.Moshi
import io.walletconnect.example.server.BridgeServer
import okhttp3.OkHttpClient
import org.komputing.khex.extensions.toNoPrefixHexString
import org.walletconnect.Session
import org.walletconnect.impls.*
import org.walletconnect.nullOnThrow
import java.io.File
import java.util.*

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initMoshi()
        initClient()
        initBridge()
        initSessionStorage()
    }

    private fun initClient() {
        client = OkHttpClient.Builder().build()
    }

    private fun initMoshi() {
        moshi = Moshi.Builder().build()
    }


    private fun initBridge() {
        bridge = BridgeServer(moshi)
        bridge.start()
    }

    private fun initSessionStorage() {
        storage = FileWCSessionStore(File(cacheDir, "session_store.json").apply { createNewFile() }, moshi)
    }

    companion object {
        private lateinit var client: OkHttpClient
        private lateinit var moshi: Moshi
        private lateinit var bridge: BridgeServer
        private lateinit var storage: WCSessionStore
        lateinit var config: Session.FullyQualifiedConfig
        lateinit var session: Session

        fun resetSession() {
            nullOnThrow { session }?.clearCallbacks()
            val key = ByteArray(32).also { Random().nextBytes(it) }.toNoPrefixHexString()
            config = Session.FullyQualifiedConfig(
                handshakeTopic = UUID.randomUUID().toString(),
                bridge = "http://localhost:${BridgeServer.PORT}",
                key = key,
            )
            session = WCSession(
                config = config,
                payloadAdapter = MoshiPayloadAdapter(moshi),
                payloadEncryption = MoshiPayloadEncryption(moshi),
                sessionStore = storage,
                transportBuilder = OkHttpTransport.Builder(client, moshi),
                clientMeta = Session.PeerMeta(name = "Example App"),
            )
            session.offer()
        }
    }
}
