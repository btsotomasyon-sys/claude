package com.btstechnic.player

import android.content.Context
import android.graphics.drawable.GradientDrawable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

const val BG = 0xFF030818.toInt()
const val CARD = 0xFF0C1736.toInt()
const val CYAN = 0xFF12D8FF.toInt()
const val BLUE = 0xFF1B6CFF.toInt()
const val MUTED = 0xFF8A94B2.toInt()

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()
fun rounded(color: Int, r: Float, stroke: Int = 0, sc: Int = 0) = GradientDrawable().apply {
    setColor(color); cornerRadius = r; if (stroke > 0) setStroke(stroke, sc)
}

object Net {
    fun open(url: String): HttpURLConnection {
        var u = url
        repeat(5) {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 10000; c.readTimeout = 20000; c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "BTS Technic Player/1.0")
            if (c.responseCode in 300..399) { u = URL(URL(u), c.getHeaderField("Location")).toString(); c.disconnect() }
            else return c
        }
        throw IOException("Çok fazla yönlendirme")
    }
    fun text(url: String): String {
        val c = open(url)
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
}

/** Ağ hatalarını anlaşılır Türkçe iletiye çevirir. */
fun friendly(e: Exception): String {
    val m = e.message ?: ""
    return when {
        e is java.net.UnknownHostException -> "Sunucu adresi bulunamadı. Adresi ve internet bağlantınızı kontrol edin."
        e is java.net.SocketTimeoutException -> "Sunucu yanıt vermedi (zaman aşımı)."
        e is javax.net.ssl.SSLException -> "Güvenli bağlantı (SSL) hatası. Adresi http:// ile deneyin."
        e is java.net.ConnectException || m.contains("Failed to connect") ->
            "Sunucuya bağlanılamadı. Adres veya port yanlış olabilir, sunucu kapalı olabilir ya da internet sağlayıcınız erişimi engelliyor olabilir."
        m.startsWith("HTTP 401") || m.startsWith("HTTP 403") -> "Sunucu erişimi reddetti ($m). Kullanıcı adı, şifre veya cihaz kısıtlaması olabilir."
        else -> m.ifEmpty { e.javaClass.simpleName }
    }
}
