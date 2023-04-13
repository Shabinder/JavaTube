package javatube

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern

open class Playlist(private val url: String) {
    protected var html: String? = null
    protected var json: JSONObject? = null

    private val playlistId: String
        get() {
            val pattern = Pattern.compile("list=[a-zA-Z0-9_\\-]*")
            val matcher = pattern.matcher(url)
            return if (matcher.find()) {
                matcher.group(0)
            } else {
                throw Exception("RegexMatcherError: $pattern")
            }
        }

    protected open fun baseData(continuation: String): String {
        return "{\"continuation\": \"$continuation\", \"context\": {\"client\": {\"clientName\": \"WEB\", \"clientVersion\": \"2.20200720.00.02\"}}}"
    }

    private fun baseParam(): String {
        return "https://www.youtube.com/youtubei/v1/browse?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    }

    private val playlistUrl: String
        private get() = "https://www.youtube.com/playlist?" + playlistId

    @Throws(Exception::class)
    protected open fun setHtml(): String? {
        return InnerTube.downloadWebPage(playlistUrl)
    }

    @Throws(Exception::class)
    protected fun getHtmlAsString(): String {
        if (html == null) {
            html = setHtml()
        }
        return html!!
    }

    @Throws(Exception::class)
    protected fun setJson(): JSONObject {
        val pattern =
            Pattern.compile("ytInitialData\\s=\\s(\\{\\\"responseContext\\\":.*\\});</script>")
        val matcher = pattern.matcher(getHtmlAsString())
        return if (matcher.find()) {
            JSONObject(matcher.group(1))
        } else {
            throw Exception("RegexMatcherError: $pattern")
        }
    }

    @Throws(Exception::class)
    protected fun getJsonObj(): JSONObject {
        if (json == null) {
            json = setJson()
        }
        return json!!
    }

    @Throws(Exception::class)
    protected fun buildContinuationUrl(continuation: String): JSONArray {
        return extractVideos(JSONObject(InnerTube.post(baseParam(), baseData(continuation))))
    }

    protected open fun extractVideos(rawJson: JSONObject): JSONArray {
        val swap = JSONArray()
        try {
            val importantContent: JSONArray = try {
                val tabs = rawJson.getJSONObject("contents")
                    .getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs")
                    .getJSONObject(0)
                    .getJSONObject("tabRenderer")
                    .getJSONObject("content")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents")
                    .getJSONObject(0)
                tabs.getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents")
                    .getJSONObject(0)
                    .getJSONObject("playlistVideoListRenderer")
                    .getJSONArray("contents")
            } catch (e: JSONException) {
                rawJson.getJSONArray("onResponseReceivedActions")
                    .getJSONObject(0)
                    .getJSONObject("appendContinuationItemsAction")
                    .getJSONArray("continuationItems")
            }
            try {
                val continuation = importantContent.getJSONObject(importantContent.length() - 1)
                    .getJSONObject("continuationItemRenderer")
                    .getJSONObject("continuationEndpoint")
                    .getJSONObject("continuationCommand")
                    .getString("token")
                val continuationEnd = buildContinuationUrl(continuation)
                for (i in 0 until importantContent.length()) {
                    swap.put(importantContent[i])
                }
                if (continuationEnd.length() > 0) {
                    for (i in 0 until continuationEnd.length()) {
                        swap.put(continuationEnd[i])
                    }
                }
            } catch (e: Exception) {
                var i = 0
                while (i < importantContent.length()) {
                    swap.put(importantContent[i])
                    i++
                }
            }
        } catch (ignored: Exception) {
        }
        return swap
    }

    open val videos: List<String>?
        get() {
            val video = extractVideos(getJsonObj())
            val videosId = mutableListOf<String>()
            return try {
                for (i in 0 until video.length()) {
                    try {
                        videosId.add(
                            "https://www.youtube.com/watch?v=" + video.getJSONObject(i)
                                .getJSONObject("playlistVideoRenderer")["videoId"].toString()
                        )
                    } catch (ignored: Exception) {
                    }
                }
                videosId
            } catch (e: JSONException) {
                throw Error(e)
            }
        }

    private fun getSidebarInfo(i: Int): JSONObject {
        return getJsonObj().getJSONObject("sidebar")
            .getJSONObject("playlistSidebarRenderer")
            .getJSONArray("items")
            .getJSONObject(i)
    }

    open val title: String?
        get() = getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
            .getJSONObject("title")
            .getJSONArray("runs")
            .getJSONObject(0)
            .getString("text")

    open val description: String?
        get() = try {
            try {
                getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                    .getJSONObject("description")
                    .getString("simpleText")
            } catch (e: JSONException) {
                getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                    .getJSONObject("description").getJSONArray("runs")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            null
        }

    open val views: String?
        get() = getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
            .getJSONArray("stats")
            .getJSONObject(1)
            .getString("simpleText")

    open val lastUpdated: String?
        get() = try {
            getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                .getJSONArray("stats").getJSONObject(2)
                .getJSONArray("runs").getJSONObject(1)
                .getString("text")
        } catch (e: JSONException) {
            getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                .getJSONArray("stats")
                .getJSONObject(2)
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text")
        }

    open val owner: String?
        get() = getSidebarInfo(1).getJSONObject("playlistSidebarSecondaryInfoRenderer")
            .getJSONObject("videoOwner")
            .getJSONObject("videoOwnerRenderer")
            .getJSONObject("title").getJSONArray("runs")
            .getJSONObject(0)
            .getString("text")

    open val ownerId: String?
        get() = getSidebarInfo(1).getJSONObject("playlistSidebarSecondaryInfoRenderer")
            .getJSONObject("videoOwner")
            .getJSONObject("videoOwnerRenderer")
            .getJSONObject("title")
            .getJSONArray("runs")
            .getJSONObject(0)
            .getJSONObject("navigationEndpoint")
            .getJSONObject("browseEndpoint")
            .getString("browseId")

    open val ownerUrl: String?
        get() = "https://www.youtube.com/channel/" + ownerId

    open val length: String?
        get() {
            return getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                .getJSONArray("stats")
                .getJSONObject(0)
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text")
        }
}