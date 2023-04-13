package javatube

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

import kotlin.Throws

class Youtube(private val urlVideo: String) {
    private val watchUrl: String

    private var vidInfo: JSONObject? = null
        private get() {
            if (field == null) {
                field = setVidInfo()
            }
            return field
        }

    private var html: String? = null
        private get() {
            if (field == null) {
                field = setHtml()
            }
            return field
        }

    private var js: String? = null
        private get() {
            if (field == null) {
                field = setJs()
            }
            return field
        }

    init {
        watchUrl = "https://www.youtube.com/watch?v=" + videoId()
    }

    @Throws(Exception::class)
    private fun videoId(): String {
        val pattern = Pattern.compile("(?:v=|/)([0-9A-Za-z_-]{11}).*")
        val matcher = pattern.matcher(urlVideo)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            throw Exception("RegexMatcherError: $pattern")
        }
    }

    @Throws(IOException::class)
    private fun setHtml(): String {
        return InnerTube.downloadWebPage(watchUrl)
    }

    @Throws(Exception::class)
    private fun setVidInfo(): JSONObject {
        val pattern = "ytInitialPlayerResponse\\s=\\s(\\{\\\"responseContext\\\":.*?\\});</script>"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(html)
        return if (matcher.find()) {
            JSONObject(matcher.group(1))
        } else {
            throw Exception("RegexMatcherError: $pattern")
        }
    }

    @Throws(Exception::class)
    private fun streamData(): JSONObject {
        return vidInfo!!.getJSONObject("streamingData")
    }

    @Throws(UnsupportedEncodingException::class)
    private fun decodeURL(s: String): String {
        return URLDecoder.decode(s, StandardCharsets.UTF_8.name())
    }

    @Throws(Exception::class)
    private fun fmtStreams(): List<Stream> {
        val streamManifest: JSONArray = Youtube.Companion.applyDescrambler(streamData())
        val fmtStream = mutableListOf<Stream>()
        val title = title
        var video: Stream
        val cipher = Cipher(js!!)
        var i = 0
        while (streamManifest.length() > i) {
            if (streamManifest.getJSONObject(i).has("signatureCipher")) {
                val oldUrl = decodeURL(streamManifest.getJSONObject(i).getString("url"))
                streamManifest.getJSONObject(i).remove("url")
                streamManifest.getJSONObject(i).put(
                    "url",
                    oldUrl + "&sig=" + cipher.getSignature(
                        decodeURL(
                            streamManifest.getJSONObject(i).getString("s")
                        ).split("(?!^)".toRegex()).dropLastWhile { it.isEmpty() })
                )
            }
            val oldUrl = streamManifest.getJSONObject(i).getString("url")
            val matcher = Pattern.compile("&n=(.*?)&").matcher(oldUrl)
            if (matcher.find()) {
                val newUrl = oldUrl.replaceFirst(
                    "&n=(.*?)&".toRegex(),
                    "&n=" + cipher.calculateN(matcher.group(1)) + "&"
                )
                streamManifest.getJSONObject(i).put("url", newUrl)
            }
            video = Stream(streamManifest.getJSONObject(i), title)
            fmtStream.add(video)
            i++
        }
        return fmtStream
    }

    private val ytPlayerJs: String
        get() {
            val pattern = Pattern.compile("(/s/player/[\\w\\d]+/[\\w\\d_/.]+/base\\.js)")
            val matcher = pattern.matcher(html)
            return if (matcher.find()) {
                "https://youtube.com" + matcher.group(1)
            } else {
                throw Exception("RegexMatcherError: $pattern")
            }
        }

    private fun setJs(): String {
        return InnerTube.downloadWebPage(ytPlayerJs)
    }

    val title: String
        get() = vidInfo!!.getJSONObject("videoDetails")
            .getString("title")

    val description: String
        get() = vidInfo!!.getJSONObject("videoDetails")
            .getString("shortDescription")

    val publishDate: String
        get() {
            val pattern =
                Pattern.compile("(?<=itemprop=\"datePublished\" content=\")\\d{4}-\\d{2}-\\d{2}")
            val matcher = pattern.matcher(html)
            return if (matcher.find()) {
                matcher.group(0)
            } else {
                throw Exception("RegexMatcherError: $pattern")
            }
        }

    @Throws(Exception::class)
    fun length(): Int {
        return vidInfo!!.getJSONObject("videoDetails")
            .getInt("lengthSeconds")
    }

    val thumbnailUrl: String
        get() {
            val thumbnails = vidInfo!!.getJSONObject("videoDetails")
                .getJSONObject("thumbnail")
                .getJSONArray("thumbnails")
            return thumbnails.getJSONObject(thumbnails.length() - 1).getString("url")
        }

    val views: Int
        get() = vidInfo!!.getJSONObject("videoDetails")
            .getString("viewCount").toInt()

    val author: String
        get() = vidInfo!!.getJSONObject("videoDetails")
            .getString("author")

    val captionTracks: List<Captions>
        get() = try {
            val rawTracks = vidInfo!!.getJSONObject("captions")
                .getJSONObject("playerCaptionsTracklistRenderer")
                .getJSONArray("captionTracks")
            val captions = mutableListOf<Captions>()
            for (i in 0 until rawTracks.length() - 1) {
                captions.add(Captions(rawTracks.getJSONObject(i)))
            }
            captions
        } catch (e: JSONException) {
            emptyList()
        }

    val captions: CaptionQuery
        get() = CaptionQuery(captionTracks)

    val keywords: JSONArray
        get() = try {
            vidInfo!!.getJSONObject("videoDetails")
                .getJSONArray("keywords")
        } catch (e: JSONException) {
            JSONArray()
        }

    fun streams(): StreamQuery {
        return StreamQuery(fmtStreams())
    }

    companion object {
        private fun applyDescrambler(streamData: JSONObject): JSONArray {
            val formats = JSONArray()
            run {
                var i = 0
                while (streamData.getJSONArray("formats").length() > i) {
                    formats.put(streamData.getJSONArray("formats")[i])
                    i++
                }
            }
            run {
                var i = 0
                while (streamData.getJSONArray("adaptiveFormats").length() > i) {
                    formats.put(streamData.getJSONArray("adaptiveFormats")[i])
                    i++
                }
            }
            for (i in 0 until formats.length()) {
                if (formats.getJSONObject(i).has("signatureCipher")) {
                    val rawSig =
                        formats.getJSONObject(i).getString("signatureCipher").replace("sp=sig", "")

                    for (j in rawSig.split("&".toRegex()).dropLastWhile { it.isEmpty() }) {
                        when {
                            j.startsWith("url") -> {
                                formats.getJSONObject(i).put(
                                    "url",
                                    j.replace("url=", ""))
                            }

                            j.startsWith("s") -> {
                                formats.getJSONObject(i).put(
                                    "s",
                                    j.replace("s=", ""))
                            }
                        }
                    }
                }
            }
            return formats
        }
    }
}