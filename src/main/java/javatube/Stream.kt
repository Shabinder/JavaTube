package javatube

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Arrays
import java.util.function.Consumer
import java.util.regex.Matcher
import java.util.regex.Pattern

class Stream(stream: JSONObject, val title: String) {
    val url: String
    val itag: Int
    val mimeType: String
    val codecs: String
    val type: String
    val subType: String
    val videoCodec: String?
    val audioCodec: String?
    val bitrate: Int
    val isOtf: Boolean
    val fileSize: Long
    val itagProfile: Map<String, String?>
    val abr: String?
    var fps: Int? = null
    val resolution: String?

    init {
        url = stream.getString("url")
        itag = stream.getInt("itag")
        mimeType = mimeTypeCodec(stream.getString("mimeType")).group(1)
        codecs = mimeTypeCodec(stream.getString("mimeType")).group(2)
        type = Arrays.asList(*mimeType.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray())[0]
        subType = Arrays.asList(*mimeType.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray())[1]
        videoCodec = parseCodecs()[0]
        audioCodec = parseCodecs()[1]
        bitrate = stream.getInt("bitrate")
        isOtf = setIsOtf(stream)
        fileSize =
            setFileSize(if (stream.has("contentLength")) stream.getString("contentLength") else null)
        itagProfile = formatProfile
        abr = itagProfile["abr"]
        if (stream.has("fps")) {
            fps = stream.getInt("fps")
        }
        resolution = itagProfile["resolution"]
    }

    @Throws(IOException::class)
    private fun setFileSize(size: String?): Long {
        var size = size
        if (size == null) {
            if (!isOtf) {
                val url = URL(url)
                val http = url.openConnection() as HttpURLConnection
                http.requestMethod = "HEAD"
                size = try {
                    http.headerFields["Content-Length"]!![0]
                } catch (e: NullPointerException) {
                    "0"
                }
                http.disconnect()
            } else {
                size = "0"
            }
            return size!!.toLong()
        }
        return size.toLong()
    }

    private fun setIsOtf(stream: JSONObject): Boolean {
        return if (stream.has("type")) {
            stream.getString("type") == "FORMAT_STREAM_TYPE_OTF"
        } else {
            false
        }
    }

    val isAdaptive: Boolean
        get() = codecs.split(",".toRegex()).dropLastWhile { it.isEmpty() }.size % 2 == 1
    val isProgressive: Boolean
        get() = !isAdaptive

    fun includeAudioTrack(): Boolean {
        return isProgressive || type == "audio"
    }

    fun includeVideoTrack(): Boolean {
        return isProgressive || type == "video"
    }

    private fun parseCodecs(): List<String?> {
        val array = mutableListOf<String?>()
        var video: String? = null
        var audio: String? = null
        val split = codecs.split(",".toRegex()).dropLastWhile { it.isEmpty() }
        if (!isAdaptive) {
            video = split[0]
            audio = split[1]
        } else if (includeVideoTrack()) {
            video = split[0]
        } else if (includeAudioTrack()) {
            audio = split[0]
        }
        array.add(video)
        array.add(audio)
        return array
    }

    @Throws(Exception::class)
    private fun mimeTypeCodec(mimeTypeCodec: String): Matcher {
        val pattern = Pattern.compile("(\\w+/\\w+);\\scodecs=\"([a-zA-Z-0-9.,\\s]*)\"")
        val matcher = pattern.matcher(mimeTypeCodec)
        return if (matcher.find()) {
            matcher
        } else {
            throw Exception("RegexMatcherError: $pattern")
        }
    }

    private fun safeFileName(s: String): String {
        return s.replace("[\"'#$%*,.:;<>?\\\\^|~/]".toRegex(), " ")
    }

    @Throws(Exception::class)
    fun download(path: String) {
        startDownload(path, title) { value: Long -> onProgress(value) }
    }

    @Throws(Exception::class)
    fun download(path: String, progress: Consumer<Long>) {
        startDownload(path, title, progress)
    }

    @Throws(Exception::class)
    fun download(path: String, fileName: String) {
        startDownload(path, fileName) { value: Long -> onProgress(value) }
    }

    @Throws(Exception::class)
    fun download(path: String, fileName: String, progress: Consumer<Long>) {
        startDownload(path, fileName, progress)
    }

    @Throws(Exception::class)
    private fun startDownload(path: String, fileName: String, progress: Consumer<Long>) {
        if (!isOtf) {
            val savePath = path + safeFileName(fileName) + "." + subType
            var startSize = 0
            var stopPos: Int
            val defaultRange = 1048576
            val f = File(savePath)
            if (f.exists()) {
                if (!f.delete()) {
                    throw IOException("Failed to delete existing output file: " + f.name)
                }
                do {
                    stopPos = Math.min((startSize + defaultRange).toLong(), fileSize).toInt()
                    if (stopPos >= fileSize) {
                        stopPos = fileSize.toInt()
                    }
                    val chunk = "$url&range=$startSize-$stopPos"
                    InnerTube.get(
                        chunk,
                        savePath,
                        Integer.toString(startSize),
                        Integer.toString(stopPos)
                    )
                    progress.accept(stopPos * 100L / fileSize)
                    startSize = if (startSize < defaultRange) {
                        stopPos
                    } else {
                        stopPos + 1
                    }
                } while (stopPos.toLong() != fileSize)
            } else {
                downloadOtf(path, fileName, progress)
            }
        }
    }

    @Throws(Exception::class)
    private fun downloadOtf(path: String, fileName: String, progress: Consumer<Long>) {
        var countChunk = 0
        var chunkReceived: ByteArray?
        var lastChunk = 0
        val savePath = path + safeFileName(fileName) + "." + subType
        val outputFile = File(savePath)
        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                throw IOException("Failed to delete existing output file: " + outputFile.name)
            }
        }
        do {
            val chunk = "$url&sq=$countChunk"
            chunkReceived = InnerTube.postChunk(chunk).toByteArray()
            if (countChunk == 0) {
                val pattern = Pattern.compile("Segment-Count: (\\d*)")
                val matcher = pattern.matcher(String(chunkReceived))
                lastChunk = if (matcher.find()) {
                    matcher.group(1).toInt()
                } else {
                    throw Exception("RegexMatcherError: $pattern")
                }
                progress.accept(countChunk * 100L / lastChunk)
                countChunk = countChunk + 1
                FileOutputStream(savePath, true).use { fos -> fos.write(chunkReceived) }
            }
        } while (countChunk <= lastChunk)
    }

    private val formatProfile: Map<String, String?>
        get() {
            val itags: MutableMap<Int, List<String>> = mutableMapOf()

            // progressive video
            itags[5] = mutableListOf<String>().apply {
                add("240p")
                add("64kbps")
            }
            itags[6] = mutableListOf<String>().apply {
                add("270p")
                add("64kbps")
            }

            itags[13] = mutableListOf<String>().apply {
                add("144p")
            }
            itags[17] = mutableListOf<String>().apply {
                add("144p")
                add("24kbps")
            }
            itags[18] = mutableListOf<String>().apply {
                add("360p")
                add("96kbps")
            }
            itags[22] = mutableListOf<String>().apply {
                add("720p")
                add("192kbps")
            }
            itags[34] = mutableListOf<String>().apply {
                add("360p")
                add("128kbps")
            }
            itags[35] = mutableListOf<String>().apply {
                add("480p")
                add("128kbps")
            }
            itags[36] = mutableListOf<String>().apply {
                add("240p")
            }
            itags[37] = mutableListOf<String>().apply {
                add("1080p")
                add("192kbps")
            }
            itags[38] = mutableListOf<String>().apply {
                add("3072p")
                add("192kbps")
            }
            itags[43] = mutableListOf<String>().apply {
                add("360p")
                add("128kbps")
            }
            itags[44] = mutableListOf<String>().apply {
                add("480p")
                add("128kbps")
            }
            itags[45] = mutableListOf<String>().apply {
                add("720p")
                add("192kbps")
            }
            itags[46] = mutableListOf<String>().apply {
                add("1080p")
                add("192kbps")
            }
            itags[59] = mutableListOf<String>().apply {
                add("480p")
                add("128kbps")
            }
            itags[78] = mutableListOf<String>().apply {
                add("480p")
                add("128kbps")
            }
            itags[82] = mutableListOf<String>().apply {
                add("360p")
                add("128kbps")
            }
            itags[83] = mutableListOf<String>().apply {
                add("480p")
                add("128kbps")
            }
            itags[84] = mutableListOf<String>().apply {
                add("720p")
                add("192kbps")
            }
            itags[85] = mutableListOf<String>().apply {
                add("1080p")
                add("192kbps")
            }
            itags[91] = mutableListOf<String>().apply {
                add("144p")
                add("48kbps")
            }
            itags[92] = mutableListOf<String>().apply {
                add("240p")
                add("48kbps")
            }
            itags[93] = mutableListOf<String>().apply {
                add("360p")
                add("128kbps")
            }
            itags[94] = mutableListOf<String>().apply {
                add("480p")
                add("128kbps")
            }
            itags[95] = mutableListOf<String>().apply {
                add("720p")
                add("256kbps")
            }
            itags[96] = mutableListOf<String>().apply {
                add("1080p")
                add("256kbps")
            }
            itags[100] = mutableListOf<String>().apply {
                add("360p")
                add("128kbps")
            }
            itags[101] = mutableListOf<String>().apply {
                add("480p")
                add("192kbps")
            }
            itags[102] = mutableListOf<String>().apply {
                add("720p")
                add("192kbps")
            }
            itags[132] = mutableListOf<String>().apply {
                add("240p")
                add("48kbps")
            }
            itags[151] = mutableListOf<String>().apply {
                add("720p")
                add("24kbps")
            }
            itags[300] = mutableListOf<String>().apply {
                add("720p")
                add("128kbps")
            }
            itags[301] = mutableListOf<String>().apply {
                add("1080p")
                add("128kbps")
            }

            // dash video
            itags[133] = mutableListOf<String>().apply {
                add("240p")
            } // MP4
            itags[134] = mutableListOf<String>().apply {
                add("360p")
            } // MP4
            itags[135] = mutableListOf<String>().apply {
                add("480p")
            } // MP4
            itags[136] = mutableListOf<String>().apply {
                add("720p")
            } // MP4
            itags[137] = mutableListOf<String>().apply {
                add("1080p")
            } // MP4
            itags[138] = mutableListOf<String>().apply {
                add("2160p")
            } // MP4
            itags[160] = mutableListOf<String>().apply {
                add("144p")
            } // WEBM
            itags[167] = mutableListOf<String>().apply {
                add("360p")
            } // WEBM
            itags[168] = mutableListOf<String>().apply {
                add("480p")
            } // WEBM
            itags[169] = mutableListOf<String>().apply {
                add("720p")
            } // WEBM
            itags[170] = mutableListOf<String>().apply {
                add("1080p")
            } // WEBM
            itags[212] = mutableListOf<String>().apply {
                add("480p")
            } // MP4
            itags[218] = mutableListOf<String>().apply {
                add("480p")
            } // WEBM
            itags[219] = mutableListOf<String>().apply {
                add("480p")
            } // WEBM
            itags[242] = mutableListOf<String>().apply {
                add("240p")
            } // WEBM
            itags[243] = mutableListOf<String>().apply {
                add("360p")
            } // WEBM
            itags[244] = mutableListOf<String>().apply {
                add("480p")
            } // WEBM
            itags[245] = mutableListOf<String>().apply {
                add("480p")
            } // WEBM
            itags[246] = mutableListOf<String>().apply {
                add("480p")
            } // WEBM
            itags[247] = mutableListOf<String>().apply {
                add("720p")
            } // WEBM
            itags[248] = mutableListOf<String>().apply {
                add("1080p")
            } // WEBM
            itags[264] = mutableListOf<String>().apply {
                add("1440p")
            } // MP4
            itags[266] = mutableListOf<String>().apply {
                add("2160p")
            } // MP4
            itags[271] = mutableListOf<String>().apply {
                add("1440p")
            } // WEBM
            itags[272] = mutableListOf<String>().apply {
                add("4320p")
            } // WEBM
            itags[278] = mutableListOf<String>().apply {
                add("144p")
            } // WEBM
            itags[298] = mutableListOf<String>().apply {
                add("720p")
            } // MP4
            itags[299] = mutableListOf<String>().apply {
                add("1080p")
            } // MP4
            itags[302] = mutableListOf<String>().apply {
                add("720p")
            } // WEBM
            itags[303] = mutableListOf<String>().apply {
                add("1080p")
            } // WEBM
            itags[308] = mutableListOf<String>().apply {
                add("1440p")
            } // WEBM
            itags[313] = mutableListOf<String>().apply {
                add("2160p")
            } // WEBM
            itags[315] = mutableListOf<String>().apply {
                add("2160p")
            } // WEBM
            itags[330] = mutableListOf<String>().apply {
                add("144p")
            } // WEBM
            itags[331] = mutableListOf<String>().apply {
                add("240p")
            } // WEBM
            itags[332] = mutableListOf<String>().apply {
                add("360p")
            } // WEBM
            itags[333] = mutableListOf<String>().apply {
                add("480p")
            } // WEBM
            itags[334] = mutableListOf<String>().apply {
                add("720p")
            } // WEBM
            itags[335] = mutableListOf<String>().apply {
                add("1080p")
            } // WEBM
            itags[336] = mutableListOf<String>().apply {
                add("1440p")
            } // WEBM
            itags[337] = mutableListOf<String>().apply {
                add("2160p")
            } // WEBM
            itags[394] = mutableListOf<String>().apply {
                add("144p")
            } // MP4
            itags[395] = mutableListOf<String>().apply {
                add("240p")
            } // MP4
            itags[396] = mutableListOf<String>().apply {
                add("360p")
            } // MP4
            itags[397] = mutableListOf<String>().apply {
                add("480p")
            } // MP4
            itags[398] = mutableListOf<String>().apply {
                add("720p")
            } // MP4
            itags[399] = mutableListOf<String>().apply {
                add("1080p")
            } // MP4
            itags[400] = mutableListOf<String>().apply {
                add("1440p")
            } // MP4
            itags[401] = mutableListOf<String>().apply {
                add("2160p")
            } // MP4
            itags[402] = mutableListOf<String>().apply {
                add("4320p")
            } // MP4
            itags[571] = mutableListOf<String>().apply {
                add("4320p")
            } // MP4
            itags[694] = mutableListOf<String>().apply {
                add("144p")
            } // MP4
            itags[695] = mutableListOf<String>().apply {
                add("240p")
            } // MP4
            itags[696] = mutableListOf<String>().apply {
                add("360p")
            } // MP4
            itags[697] = mutableListOf<String>().apply {
                add("480p")
            } // MP4
            itags[698] = mutableListOf<String>().apply {
                add("720p")
            } // MP4
            itags[699] = mutableListOf<String>().apply {
                add("1080p")
            } // MP4
            itags[700] = mutableListOf<String>().apply {
                add("1440p")
            } // MP4
            itags[701] = mutableListOf<String>().apply {
                add("2160p")
            } // MP4
            itags[702] = mutableListOf<String>().apply {
                add("4320p")
            } // MP4

            // dash audio
            itags[139] = mutableListOf<String>().apply {
                add("48kbps")
            } // MP4
            itags[140] = mutableListOf<String>().apply {
                add("128kbps")
            } // MP4
            itags[141] = mutableListOf<String>().apply {
                add("256kbps")
            } // MP4
            itags[171] = mutableListOf<String>().apply {
                add("128kbps")
            } // WEBM
            itags[172] = mutableListOf<String>().apply {
                add("256kbps")
            } // WEBM
            itags[249] = mutableListOf<String>().apply {
                add("50kbps")
            } // WEBM
            itags[250] = mutableListOf<String>().apply {
                add("70kbps")
            } // WEBM
            itags[251] = mutableListOf<String>().apply {
                add("160kbps")
            } // WEBM
            itags[256] = mutableListOf<String>().apply {
                add("192kbps")
            } // MP4
            itags[258] = mutableListOf<String>().apply {
                add("384kbps")
            } // MP4
            itags[325] = mutableListOf<String>().apply {
            } // MP4
            itags[328] = mutableListOf<String>().apply {
            } // MP4
            val res: String?
            val bitrate: String?
            if (itags.containsKey(itag)) {
                res = itags[itag]?.getOrNull(0)
                bitrate = itags[itag]?.getOrNull(1)
            } else {
                res = null
                bitrate = null
            }
            val returnItags: MutableMap<String, String?> = HashMap()
            returnItags["resolution"] = res
            returnItags["abr"] = bitrate
            return returnItags
        }


    companion object {
        fun onProgress(value: Long) {
            println("$value%")
        }
    }
}