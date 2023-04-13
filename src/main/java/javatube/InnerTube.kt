package javatube

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.channels.Channels
import javax.net.ssl.HttpsURLConnection

internal object InnerTube {
    @Throws(IOException::class)
    fun post(param: String?, data: String): String {
        val output = StringBuilder()
        val url = URL(param)
        val conn = url.openConnection()
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Content-Length", Integer.toString(data.length))
        try {
            DataOutputStream(conn.getOutputStream()).use { dos -> dos.writeBytes(data) }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        try {
            BufferedReader(
                InputStreamReader(
                    conn.getInputStream()
                )
            ).use { bf ->
                var keepGoing = true
                while (keepGoing) {
                    val currentLine = bf.readLine()
                    if (currentLine == null) {
                        keepGoing = false
                    } else {
                        output.append(currentLine)
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return output.toString()
    }

    @Throws(IOException::class)
    operator fun get(videoUrl: String?, outputFileName: String?, start: String, end: String) {
        val url = URL(videoUrl)
        val com = url.openConnection()
        com.setRequestProperty("Method", "GET")
        Channels.newChannel(com.getInputStream()).use { rbc ->
            FileOutputStream(outputFileName, true).use { fos ->
                fos.channel.transferFrom(
                    rbc, start.toInt()
                        .toLong(), end.toInt().toLong()
                )
            }
        }
    }

    @Throws(IOException::class)
    fun downloadWebPage(webpage: String?): String {
        val url = URL(webpage)
        val con = url.openConnection() as HttpsURLConnection
        con.setRequestProperty("accept-language", "en-US,en")
        con.setRequestProperty("User-Agent", "Mozilla/5.0")
        val ins = con.inputStream
        val isr = InputStreamReader(ins)
        val `in` = BufferedReader(isr)
        var inputLine: String?
        val html = StringBuilder()
        while (`in`.readLine().also { inputLine = it } != null) {
            html.append(inputLine)
        }
        `in`.close()
        return html.toString()
    }

    @Throws(IOException::class)
    fun postChunk(chunk: String?): ByteArrayOutputStream {
        val url = URL(chunk)
        val `in`: InputStream = BufferedInputStream(url.openStream())
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        var n: Int
        while (-1 != `in`.read(buf).also { n = it }) {
            out.write(buf, 0, n)
        }
        out.close()
        `in`.close()
        return out
    }
}