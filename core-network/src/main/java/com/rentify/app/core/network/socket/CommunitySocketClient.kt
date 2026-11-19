package com.rentify.app.core.network.socket

import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.client.NetworkConfig
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object CommunitySocketClient {

    private const val SOCKET_IO_PATH = "/api/v1/community/socket.io/"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val moshi = NetworkClient.createMoshi()

    private val messageCreatedAdapter = moshi.adapter(MessageListResponseDto::class.java)
    private val messageReceivedAdapter = moshi.adapter(SocketMessageReceivedDto::class.java)
    private val readUpdatedAdapter = moshi.adapter(SocketReadUpdatedDto::class.java)
    private val memberReadAdapter = moshi.adapter(SocketMemberReadDto::class.java)
    private val typingAdapter = moshi.adapter(SocketTypingDto::class.java)
    private val conversationIdAdapter = moshi.adapter(SocketConversationIdDto::class.java)
    private val ackAdapter = moshi.adapter(SocketJoinLeaveAckDto::class.java)

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    private val _events = MutableSharedFlow<CommunitySocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<CommunitySocketEvent> = _events.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val joinedConversations = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val pendingAckCallbacks = ConcurrentHashMap<Int, (SocketJoinLeaveAckDto?) -> Unit>()
    private val ackIdGenerator = AtomicInteger(1)
    private val lastTypingSentAt = ConcurrentHashMap<String, Long>()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var currentToken: String? = null

    @Volatile
    private var baseUrl: String = NetworkConfig.DEFAULT_BASE_URL

    @Volatile
    private var isSessionReady: Boolean = false

    @Volatile
    private var hasConnectedOnce: Boolean = false

    @Volatile
    private var shouldReconnect: Boolean = false

    private var reconnectJob: Job? = null
    private var reconnectAttempts: Int = 0

    @Synchronized
    fun ensureConnected(token: String?, customBaseUrl: String = NetworkConfig.DEFAULT_BASE_URL) {
        if (token.isNullOrBlank()) {
            disconnect()
            return
        }

        val tokenChanged = currentToken != null && currentToken != token
        currentToken = token
        baseUrl = customBaseUrl
        shouldReconnect = true

        if (tokenChanged || webSocket == null) {
            reconnectJob?.cancel()
            openSocketInternal()
        }
    }

    @Synchronized
    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        isSessionReady = false
        _isConnected.value = false
        pendingAckCallbacks.clear()
        lastTypingSentAt.clear()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        currentToken = null
    }

    fun joinConversation(
        conversationId: String,
        onAck: ((SocketJoinLeaveAckDto?) -> Unit)? = null
    ) {
        if (conversationId.isBlank()) return
        joinedConversations.add(conversationId)
        if (isSessionReady) {
            val payload = JSONObject().put("conversationId", conversationId)
            emitSocketEvent("conversation:join", payload, onAck)
        }
    }

    fun leaveConversation(
        conversationId: String,
        onAck: ((SocketJoinLeaveAckDto?) -> Unit)? = null
    ) {
        if (conversationId.isBlank()) return
        joinedConversations.remove(conversationId)
        lastTypingSentAt.remove(conversationId)
        if (isSessionReady) {
            val payload = JSONObject().put("conversationId", conversationId)
            emitSocketEvent("conversation:leave", payload, onAck)
        }
    }

    fun sendTyping(conversationId: String, isTyping: Boolean) {
        if (conversationId.isBlank() || !isSessionReady) return
        val now = System.currentTimeMillis()
        if (isTyping) {
            val lastSent = lastTypingSentAt[conversationId] ?: 0L
            if (now - lastSent < 2000L) return
            lastTypingSentAt[conversationId] = now
        } else {
            lastTypingSentAt.remove(conversationId)
        }

        val payload = JSONObject()
            .put("conversationId", conversationId)
            .put("isTyping", isTyping)
        emitSocketEvent("conversation:typing", payload, null)
    }

    @Synchronized
    private fun openSocketInternal() {
        val token = currentToken ?: return
        webSocket?.cancel()
        isSessionReady = false
        _isConnected.value = false

        val wsUrl = buildSocketIoWebSocketUrl(baseUrl) ?: return
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleEngineIoFrame(webSocket, text, token)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleSocketClosed(webSocket)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleSocketClosed(webSocket)
            }
        })
    }

    private fun handleEngineIoFrame(ws: WebSocket, frame: String, token: String) {
        if (frame.isEmpty()) return

        when (frame[0]) {
            '0' -> {
                // Engine.IO OPEN -> Gửi gói Socket.IO CONNECT (40) kèm auth token
                val authPayload = JSONObject().put("token", token)
                ws.send("40$authPayload")
            }
            '2' -> {
                // Engine.IO PING -> Phản hồi Engine.IO PONG (3)
                ws.send("3")
            }
            '4' -> {
                handleSocketIoPacket(frame.substring(1))
            }
        }
    }

    private fun handleSocketIoPacket(packet: String) {
        if (packet.isEmpty()) return

        when (packet[0]) {
            '0' -> {
                // Socket.IO CONNECT ACK
                _isConnected.value = true
            }
            '1', '4' -> {
                // Socket.IO DISCONNECT / CONNECT_ERROR
                _isConnected.value = false
                isSessionReady = false
            }
            '2' -> {
                // Socket.IO EVENT: 2["eventName", {...}]
                val jsonStart = packet.indexOf('[')
                if (jsonStart == -1) return
                val arrayStr = packet.substring(jsonStart)
                val jsonArray = runCatching { JSONArray(arrayStr) }.getOrNull() ?: return
                val eventName = jsonArray.optString(0, "")
                val dataObj = jsonArray.optJSONObject(1)
                val dataStr = dataObj?.toString() ?: return
                dispatchServerEvent(eventName, dataObj, dataStr)
            }
            '3' -> {
                // Socket.IO ACK: 3<ackId>[{...}]
                val jsonStart = packet.indexOf('[')
                if (jsonStart <= 1) return
                val ackId = packet.substring(1, jsonStart).toIntOrNull() ?: return
                val callback = pendingAckCallbacks.remove(ackId) ?: return
                val arrayStr = packet.substring(jsonStart)
                val jsonArray = runCatching { JSONArray(arrayStr) }.getOrNull()
                val firstObjStr = jsonArray?.optJSONObject(0)?.toString()
                val ackDto = firstObjStr?.let { runCatching { ackAdapter.fromJson(it) }.getOrNull() }
                callback.invoke(ackDto)
            }
        }
    }

    private fun dispatchServerEvent(eventName: String, dataObj: JSONObject, dataStr: String) {
        when (eventName) {
            "session:ready" -> {
                val userId = dataObj.optString("userId", "")
                val isReconnect = hasConnectedOnce
                hasConnectedOnce = true
                isSessionReady = true
                _isConnected.value = true

                // Tự động join lại tất cả các conversation đang mở
                for (convId in joinedConversations) {
                    val joinPayload = JSONObject().put("conversationId", convId)
                    emitSocketEvent("conversation:join", joinPayload, null)
                }

                scope.launch {
                    _events.emit(CommunitySocketEvent.SessionReady(userId, isReconnect))
                }
            }
            "message:created" -> {
                val msg = runCatching { messageCreatedAdapter.fromJson(dataStr) }.getOrNull() ?: return
                scope.launch {
                    _events.emit(CommunitySocketEvent.MessageCreated(msg))
                }
            }
            "chat:message-received" -> {
                val dto = runCatching { messageReceivedAdapter.fromJson(dataStr) }.getOrNull() ?: return
                scope.launch {
                    _events.emit(CommunitySocketEvent.ChatMessageReceived(dto))
                }
            }
            "conversation:read-updated" -> {
                val dto = runCatching { readUpdatedAdapter.fromJson(dataStr) }.getOrNull() ?: return
                scope.launch {
                    _events.emit(CommunitySocketEvent.ConversationReadUpdated(dto))
                }
            }
            "conversation:member-read" -> {
                val dto = runCatching { memberReadAdapter.fromJson(dataStr) }.getOrNull() ?: return
                scope.launch {
                    _events.emit(CommunitySocketEvent.ConversationMemberRead(dto))
                }
            }
            "conversation:typing" -> {
                val dto = runCatching { typingAdapter.fromJson(dataStr) }.getOrNull() ?: return
                scope.launch {
                    _events.emit(CommunitySocketEvent.ConversationTyping(dto))
                }
            }
            "conversation:access-revoked" -> {
                val dto = runCatching { conversationIdAdapter.fromJson(dataStr) }.getOrNull() ?: return
                joinedConversations.remove(dto.conversationId)
                scope.launch {
                    _events.emit(CommunitySocketEvent.ConversationAccessRevoked(dto.conversationId))
                }
            }
            "conversation:available" -> {
                val dto = runCatching { conversationIdAdapter.fromJson(dataStr) }.getOrNull() ?: return
                scope.launch {
                    _events.emit(CommunitySocketEvent.ConversationAvailable(dto.conversationId))
                }
            }
        }
    }

    private fun emitSocketEvent(
        eventName: String,
        payload: JSONObject,
        onAck: ((SocketJoinLeaveAckDto?) -> Unit)?
    ) {
        val ws = webSocket ?: return
        val array = JSONArray().put(eventName).put(payload)
        if (onAck != null) {
            val ackId = ackIdGenerator.getAndIncrement()
            pendingAckCallbacks[ackId] = onAck
            ws.send("42$ackId$array")
        } else {
            ws.send("42$array")
        }
    }

    private fun handleSocketClosed(closedWs: WebSocket) {
        if (webSocket !== closedWs) return
        isSessionReady = false
        _isConnected.value = false
        webSocket = null

        if (!shouldReconnect || currentToken.isNullOrBlank()) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val attempt = reconnectAttempts.coerceAtMost(5)
            val delayMs = (1500L * (1 shl attempt)).coerceAtMost(15000L)
            reconnectAttempts++
            delay(delayMs)
            if (shouldReconnect && !currentToken.isNullOrBlank()) {
                openSocketInternal()
            }
        }
    }

    private fun buildSocketIoWebSocketUrl(rawBaseUrl: String): String? {
        val parsed = rawBaseUrl.toHttpUrlOrNull() ?: return null
        val scheme = if (parsed.isHttps) "https" else "http"
        val httpUrl = parsed.newBuilder()
            .scheme(scheme)
            .encodedPath(SOCKET_IO_PATH)
            .query(null)
            .addQueryParameter("EIO", "4")
            .addQueryParameter("transport", "websocket")
            .build()
            .toString()

        return when {
            httpUrl.startsWith("https://") -> "wss://" + httpUrl.removePrefix("https://")
            httpUrl.startsWith("http://") -> "ws://" + httpUrl.removePrefix("http://")
            else -> httpUrl
        }
    }
}
