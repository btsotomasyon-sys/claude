package com.btstechnic.player

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.Executors

private const val WHITE = 0xFFEEF1FA.toInt()

class LoginActivity : Activity() {
    private lateinit var server: EditText
    private lateinit var user: EditText
    private lateinit var pass: EditText
    private lateinit var m3u: EditText
    private lateinit var xtreamBox: LinearLayout
    private lateinit var steps: LinearLayout
    private lateinit var test: Button
    private lateinit var login: Button
    private lateinit var tabX: TextView
    private lateinit var tabM: TextView
    private var xtream = true
    private var verified = ""

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(48), dp(22), dp(24)) }
        col.addView(ImageView(this).apply { setImageResource(R.drawable.app_logo) }, LinearLayout.LayoutParams(dp(84), dp(84)))
        col.addView(tv("Hoş geldiniz", 24f, WHITE, true).apply { setPadding(0, dp(14), 0, 0) })
        col.addView(tv("Hesap bilgilerinizi bir kez girin; sonraki açılışlarda doğrudan ana sayfaya girersiniz.", 13f, MUTED).apply { setPadding(0, dp(4), 0, dp(14)) })

        val tabs = LinearLayout(this)
        tabX = tab("Kullanıcı girişi") { setMode(true) }
        tabM = tab("M3U adresi") { setMode(false) }
        tabs.addView(tabX, LinearLayout.LayoutParams(0, -2, 1f))
        tabs.addView(tabM, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
        col.addView(tabs)

        xtreamBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        server = field("Sunucu adresi  (örn. http://ornek.com:8080)", InputType.TYPE_TEXT_VARIATION_URI)
        user = field("Kullanıcı adı", 0)
        pass = field("Şifre", InputType.TYPE_TEXT_VARIATION_PASSWORD)
        xtreamBox.addView(server, lp(10)); xtreamBox.addView(user, lp(10)); xtreamBox.addView(pass, lp(10))
        col.addView(xtreamBox)
        m3u = field("M3U liste adresi (http / https)", InputType.TYPE_TEXT_VARIATION_URI).apply { visibility = View.GONE }
        col.addView(m3u, lp(10))

        test = button("Bağlantıyı test et") { runTest() }
        col.addView(test, lp(12))
        steps = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(steps, lp(10))
        login = button("Giriş yap ve kaydet") {
            SessionStore.save(this, verified); startActivity(Intent(this, HomeActivity::class.java)); finish()
        }.apply { visibility = View.GONE }
        col.addView(login, lp(6))
        setContentView(ScrollView(this).apply { setBackgroundColor(BG); isFillViewport = true; addView(col) })
        setMode(true)
    }

    // ───────── arayüz yardımcıları ─────────
    private fun lp(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }
    private fun tv(s: String, sp: Float, c: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = sp; setTextColor(c); if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun field(hintText: String, type: Int) = EditText(this).apply {
        hint = hintText; setHintTextColor(MUTED); setTextColor(WHITE); textSize = 14f
        inputType = InputType.TYPE_CLASS_TEXT or type; setSingleLine()
        background = rounded(CARD, dp(12).toFloat(), 1, 0xFF262E4A.toInt()); setPadding(dp(14), dp(13), dp(14), dp(13))
    }
    private fun button(s: String, onClick: () -> Unit) = Button(this).apply {
        text = s; isAllCaps = false; textSize = 15f; setTextColor(0xFF04122E.toInt()); setTypeface(typeface, Typeface.BOLD)
        background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(CYAN, BLUE))
            .apply { cornerRadius = dp(12).toFloat() }
        setOnClickListener { onClick() }
    }
    private fun tab(s: String, onClick: () -> Unit) = tv(s, 13f, WHITE, true).apply {
        gravity = Gravity.CENTER; setPadding(0, dp(11), 0, dp(11)); setOnClickListener { onClick() }
    }
    private fun setMode(x: Boolean) {
        xtream = x
        xtreamBox.visibility = if (x) View.VISIBLE else View.GONE
        m3u.visibility = if (x) View.GONE else View.VISIBLE
        for ((t, on) in listOf(tabX to x, tabM to !x)) {
            t.setTextColor(if (on) 0xFF04122E.toInt() else WHITE)
            t.background = if (on) rounded(CYAN, dp(12).toFloat()) else rounded(CARD, dp(12).toFloat(), 1, 0xFF262E4A.toInt())
        }
        steps.removeAllViews(); login.visibility = View.GONE
    }

    // ───────── test akışı ─────────
    private fun runTest() {
        steps.removeAllViews(); login.visibility = View.GONE
        var s = ""; var u = ""; var p = ""; var direct = ""
        if (xtream) {
            s = server.text.toString().trim(); u = user.text.toString().trim(); p = pass.text.toString().trim()
            // Uzun get.php bağlantısı yapıştırıldıysa parçalarına ayır
            if (s.contains("username=") || s.contains("get.php")) {
                val uri = Uri.parse(if (s.contains("://")) s else "http://$s")
                uri.getQueryParameter("username")?.let { u = it }
                uri.getQueryParameter("password")?.let { p = it }
                s = s.substringBefore("/get.php").substringBefore("/player_api.php").substringBefore("?").trimEnd('/')
                server.setText(s); user.setText(u); pass.setText(p)
            }
        } else direct = m3u.text.toString().trim()
        test.isEnabled = false
        Executors.newSingleThreadExecutor().execute {
            try { if (xtream) xtreamFlow(s, u, p) else m3uFlow(direct) } catch (e: Exception) { /* hata satırı gösterildi */ }
            runOnUiThread { test.isEnabled = true }
        }
    }

    private fun step(text: String, warnOnly: Boolean = false, block: () -> Unit) {
        val row = tv("…  $text", 13f, WHITE).apply { setPadding(dp(12), dp(9), dp(12), dp(9)); background = rounded(CARD, dp(10).toFloat()) }
        runOnUiThread { steps.addView(row, lp(6)) }
        try { block(); runOnUiThread { row.text = "✓  $text"; row.setTextColor(0xFF2ECC8F.toInt()) } }
        catch (e: Exception) {
            val msg = friendly(e)
            runOnUiThread { row.text = (if (warnOnly) "⚠  $text: " else "✗  $text: ") + msg; row.setTextColor(if (warnOnly) 0xFFFFB86B.toInt() else 0xFFFF5D6C.toInt()) }
            if (!warnOnly) throw e
        }
    }
    private fun note(text: String) = runOnUiThread { steps.addView(tv(text, 12f, MUTED).apply { setPadding(dp(12), dp(4), dp(12), dp(2)) }) }

    private fun xtreamFlow(s0: String, u: String, p: String) {
        step("Bilgiler eksiksiz") {
            require(s0.isNotEmpty() && u.isNotEmpty() && p.isNotEmpty()) { "Sunucu adresi, kullanıcı adı ve şifre gerekli." }
        }
        val cands = if (s0.contains("://")) {
            val alt = if (s0.startsWith("https://")) "http://" + s0.removePrefix("https://") else "https://" + s0.removePrefix("http://")
            listOf(s0, alt)
        } else listOf("http://$s0", "https://$s0")
        val eu = URLEncoder.encode(u, "UTF-8"); val ep = URLEncoder.encode(p, "UTF-8")
        var base = ""; var info: JSONObject? = null

        step("Sunucuya erişildi") {
            var first: Exception? = null
            for (c in cands) {
                try {
                    val body = Net.text("$c/player_api.php?username=$eu&password=$ep")
                    base = c; info = try { JSONObject(body) } catch (x: Exception) { null }
                    return@step
                } catch (e: Exception) {
                    if (first == null) first = e
                    if (e.message?.startsWith("HTTP") == true) { base = c; return@step }   // sunucuya ulaşıldı
                }
            }
            throw Exception(friendly(first ?: Exception("Bağlantı kurulamadı")) + "  (http ve https denendi)")
        }

        val i0 = info
        step("Hesap doğrulandı", warnOnly = i0 == null) {
            val ui = i0?.optJSONObject("user_info") ?: throw Exception("Sunucu hesap bilgisi vermedi; listeyi doğrudan deneyeceğiz.")
            if (ui.optInt("auth", 0) != 1) throw Exception("Kullanıcı adı veya şifre hatalı.")
            val st = ui.optString("status", "")
            if (st.isNotEmpty() && !st.equals("Active", true)) throw Exception("Hesap durumu: $st")
            val ex = ui.optString("exp_date", "").toLongOrNull()
                ?.let { java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale("tr")).format(java.util.Date(it * 1000)) } ?: "süresiz"
            note("Hesap bitiş tarihi: $ex  ·  Eşzamanlı bağlantı: ${ui.optString("active_cons", "?")}/${ui.optString("max_connections", "?")}")
        }

        var body = ""; var plUrl = ""
        step("Kanal listesi indirildi") {
            var err: Exception? = null
            for (out in listOf("ts", "m3u8")) {
                val link = "$base/get.php?username=$eu&password=$ep&type=m3u_plus&output=$out"
                try {
                    val t = Net.text(link)
                    if (t.removePrefix("\uFEFF").trimStart().startsWith("#EXTM3U")) { body = t; plUrl = link; return@step }
                    if (err == null) err = Exception("Sunucu liste yerine başka bir yanıt verdi.")
                } catch (e: Exception) { if (err == null) err = e }
            }
            throw err ?: Exception("Liste alınamadı.")
        }
        finishFlow(body, plUrl)
    }

    private fun m3uFlow(u: String) {
        step("Adres biçimi geçerli") {
            val uri = Uri.parse(u)
            require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrEmpty()) { "Geçerli bir HTTP veya HTTPS liste adresi girin." }
        }
        var body = ""
        step("Sunucuya erişildi ve liste indirildi") { body = Net.text(u) }
        step("M3U başlığı bulundu") { require(body.removePrefix("\uFEFF").trimStart().startsWith("#EXTM3U")) { "Bu adres bir M3U kanal listesi değil." } }
        finishFlow(body, u)
    }

    private fun finishFlow(body: String, plUrl: String) {
        var list = emptyList<Entry>()
        step("Kanallar okundu") { list = M3u.parse(body); require(list.isNotEmpty()) { "Listede oynatılabilir içerik bulunamadı." } }
        note("${list.count { it.kind == Kind.LIVE }} kanal · ${list.count { it.kind == Kind.MOVIE }} film · ${list.count { it.kind == Kind.SERIES }} dizi bölümü")
        step("Örnek yayın açıldı", warnOnly = true) {
            val c = Net.open((list.firstOrNull { it.kind == Kind.LIVE } ?: list.first()).url)
            try { require(c.responseCode in 200..299) { "HTTP ${c.responseCode}" } } finally { c.disconnect() }
        }
        Library.all = list; verified = plUrl
        runOnUiThread { login.visibility = View.VISIBLE }
    }
}
