package javatube

import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

class Captions(captionTrack: JSONObject) {
    val url: String
    val code: String

    init {
        url = captionTrack.getString("baseUrl")
        code = captionTrack.getString("vssId").replace(".", "")
    }

    @get:Throws(IOException::class)
    private val xmlCaptions: String
        private get() = InnerTube.downloadWebPage(url).replace("(&#39;)|(&amp;#39;)".toRegex(), "'")

    @Throws(IOException::class)
    private fun generateSrtCaptions(): String {
        return xmlCaptions
    }

    @Throws(UnsupportedEncodingException::class)
    private fun decodeString(s: String): String {
        return URLDecoder.decode(s, StandardCharsets.UTF_8.name())
    }

    private fun srtTimeFormat(d: Float): String {
        val ms = d % 1
        val round = ((d - ms).toString().replace(".", "") + "00").toInt()
        val seconds = round / 1000 % 60
        val minutes = round / 60000 % 60
        val hours = round / 3600000
        return String.format("%02d:%02d:%02d,", hours, minutes, seconds) + String.format("%.3f", ms)
            .replace("0,", "")
    }

    @Throws(Exception::class)
    fun xmlCaptionToSrt(): String {
        val root = generateSrtCaptions()
        var i = 0
        var segments: String? = ""
        val pattern = "start=\\\"(.*?)\\\".*?dur=\\\"(.*?)\\\">(.*?)<"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(root)
        while (matcher.find()) {
            val start = matcher.group(1).toFloat()
            val duration = matcher.group(2).toFloat()
            val caption = decodeString(matcher.group(3))
            val end = start + duration
            val sequenceNumber = i + 1
            val line = """$sequenceNumber
${srtTimeFormat(start)} --> ${srtTimeFormat(end)}
$caption

"""
            segments += line
            i++
        }
        return segments!!
    }

    fun download(filename: String, savePath: String) {
        val fullPath: String
        fullPath = if (savePath.endsWith("/")) {
            savePath + filename
        } else {
            "$savePath/$filename"
        }
        val path = Paths.get(fullPath)
        if (filename.endsWith(".srt")) {
            try {
                Files.writeString(path, xmlCaptionToSrt(), StandardCharsets.UTF_8)
            } catch (ex: IOException) {
                print("Invalid Path")
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        } else {
            try {
                Files.writeString(path, xmlCaptions, StandardCharsets.UTF_8)
            } catch (ex: IOException) {
                print("Invalid Path")
            }
        }
    }
}