package javatube

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

class Channel(private val url: String) : Playlist(url) {
    private val channelUrl: String
    private val videosUrl: String
    private val shortsUrl: String
    private val streamsUrl: String
    private val playlistUrl: String
    private val communityUrl: String
    private val featuredChannelUrl: String
    private val aboutUrl: String
    private var htmlPage: String? = null
    private var visitorData: String? = null

    init {
        channelUrl = "https://www.youtube.com" + extractUrl()
        videosUrl = "$channelUrl/videos"
        shortsUrl = "$channelUrl/shorts"
        streamsUrl = "$channelUrl/streams"
        playlistUrl = "$channelUrl/playlists"
        communityUrl = "$channelUrl/community"
        featuredChannelUrl = "$channelUrl/channels"
        aboutUrl = "$channelUrl/about"
    }

    private fun extractUrl(): String {
        val re = mutableListOf<String>()
        re.add("(?:\\/(c)\\/([%\\d\\w_\\-]+)(\\/.*)?)")
        re.add("(?:\\/(channel)\\/([%\\w\\d_\\-]+)(\\/.*)?)")
        re.add("(?:\\/(u)\\/([%\\d\\w_\\-]+)(\\/.*)?)")
        re.add("(?:\\/(user)\\/([%\\w\\d_\\-]+)(\\/.*)?)")
        re.add("(?:\\/(\\@)([%\\d\\w_\\-\\.]+)(\\/.*)?)")
        for (regex in re) {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                return if (matcher.group(1) == "@") {
                    "/@" + matcher.group(2)
                } else {
                    "/" + matcher.group(1) + "/" + matcher.group(2)
                }
            }
        }
        throw Exception("RegexMatcherError")
    }

    private var htmlUrl: String
        get() = htmlPage!!
        private set(url) {
            if (htmlPage != url) {
                htmlPage = url
                html = null
                json = null
            }
        }

    override fun baseData(continuation: String): String {
        return "{\"continuation\": \"$continuation\", \"context\": {\"client\": {\"clientName\": \"WEB\",  \"visitorData\": \"$visitorData\", \"clientVersion\": \"2.20221107.06.00\"}}}"
    }

    @Throws(IOException::class)
    override fun setHtml(): String {
        return InnerTube.downloadWebPage(htmlUrl)
    }

    override fun extractVideos(rawJson: JSONObject): JSONArray {
        val swap = JSONArray()
        try {
            var importantContent: JSONArray
            try {
                var activeTab = JSONObject()
                for (tab in rawJson.getJSONObject("contents")
                    .getJSONObject("twoColumnBrowseResultsRenderer").getJSONArray("tabs")) {
                    val tabUrl = JSONObject(tab.toString()).getJSONObject("tabRenderer")
                        .getJSONObject("endpoint")
                        .getJSONObject("commandMetadata")
                        .getJSONObject("webCommandMetadata")
                        .getString("url")
                    if (tabUrl.substring(tabUrl.lastIndexOf("/") + 1) == htmlUrl.substring(
                            htmlUrl.lastIndexOf("/") + 1
                        )
                    ) {
                        activeTab = JSONObject(tab.toString())
                        break
                    }
                }
                visitorData = rawJson.getJSONObject("responseContext")
                    .getJSONObject("webResponseContextExtensionData")
                    .getJSONObject("ytConfigData")
                    .getString("visitorData")
                importantContent = activeTab.getJSONObject("tabRenderer")
                    .getJSONObject("content")
                    .getJSONObject("richGridRenderer")
                    .getJSONArray("contents")
            } catch (e: JSONException) {
                importantContent = rawJson.getJSONArray("onResponseReceivedActions")
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
                val continuationEnd = JSONArray(buildContinuationUrl(continuation))
                for (i in 0 until importantContent.length()) {
                    swap.put(importantContent[i])
                }
                if (!continuationEnd.isEmpty) {
                    for (i in 0 until continuationEnd.length()) {
                        swap.put(continuationEnd[i])
                    }
                }
            } catch (e: JSONException) {
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

    override val videos: List<String>
        get() {
            htmlUrl = videosUrl
            return extractVideosId()
        }

    val shorts: List<String>
        get() {
            htmlUrl = shortsUrl
            return extractVideosId()
        }

    val lives: List<String>
        get() {
            htmlUrl = streamsUrl
            return extractVideosId()
        }

    private fun extractVideosId(): List<String> {
        val video = extractVideos(getJsonObj())
        val videosId = mutableListOf<String>()
        return try {
            for (i in 0 until video.length()) {
                try {
                    try {
                        videosId.add(
                            "https://www.youtube.com/watch?v=" + video.getJSONObject(i)
                                .getJSONObject("richItemRenderer")
                                .getJSONObject("content")
                                .getJSONObject("videoRenderer")
                                .getString("videoId")
                        )
                    } catch (s: JSONException) {
                        videosId.add(
                            "https://www.youtube.com/watch?v=" + video.getJSONObject(i)
                                .getJSONObject("richItemRenderer")
                                .getJSONObject("content")
                                .getJSONObject("reelItemRenderer")
                                .getString("videoId")
                        )
                    }
                } catch (ignored: Exception) {
                }
            }
            videosId
        } catch (e: JSONException) {
            throw Error(e)
        }
    }

    override val title: String
        get() = channelName

    override val lastUpdated: String?
        get() {
        htmlUrl = videosUrl
        return try {
            getJsonObj().getJSONObject("contents")
                .getJSONObject("twoColumnBrowseResultsRenderer")
                .getJSONArray("tabs")
                .getJSONObject(1)
                .getJSONObject("tabRenderer")
                .getJSONObject("content")
                .getJSONObject("richGridRenderer")
                .getJSONArray("contents")
                .getJSONObject(0)
                .getJSONObject("richItemRenderer")
                .getJSONObject("content")
                .getJSONObject("videoRenderer")
                .getJSONObject("publishedTimeText")
                .getString("simpleText")
        } catch (j: JSONException) {
            ""
        }
    }

    override val length: String
        get() {
            htmlUrl = channelUrl
            return getJsonObj().getJSONObject("header")
                .getJSONObject("c4TabbedHeaderRenderer")
                .getJSONObject("videosCountText")
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text")
        }

    val channelName: String
        get() {
            htmlUrl = channelUrl
            return getJsonObj().getJSONObject("metadata")
                .getJSONObject("channelMetadataRenderer")
                .getString("title")
        }

    val channelId: String
        get() {
            htmlUrl = channelUrl
            return getJsonObj().getJSONObject("metadata")
                .getJSONObject("channelMetadataRenderer")
                .getString("externalId")
        }

    val vanityUrl: String
        get() {
            htmlUrl = channelUrl
            return getJsonObj().getJSONObject("metadata")
                .getJSONObject("channelMetadataRenderer")
                .getString("vanityChannelUrl")
        }

    override val description: String
        get() {
            htmlUrl = channelUrl
            return getJsonObj().getJSONObject("metadata")
                .getJSONObject("channelMetadataRenderer")
                .getString("description")
        }

    val biography: String?
        get() {
            htmlUrl = aboutUrl
            val pos =
                getJsonObj().getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs").length() - 2
            return try {
                getJsonObj().getJSONObject("contents")
                    .getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs")
                    .getJSONObject(pos)
                    .getJSONObject("tabRenderer")
                    .getJSONObject("content")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents")
                    .getJSONObject(0)
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents")
                    .getJSONObject(0)
                    .getJSONObject("channelAboutFullMetadataRenderer")
                    .getJSONObject("artistBio")
                    .getString("simpleText")
            } catch (e: JSONException) {
                null
            }
        }

    override val views: String?
        get() {
            htmlUrl = aboutUrl
            val pos =
                getJsonObj().getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs").length() - 2
            return try {
                getJsonObj().getJSONObject("contents")
                    .getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs")
                    .getJSONObject(pos)
                    .getJSONObject("tabRenderer")
                    .getJSONObject("content")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents")
                    .getJSONObject(0)
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents")
                    .getJSONObject(0)
                    .getJSONObject("channelAboutFullMetadataRenderer")
                    .getJSONObject("viewCountText")
                    .getString("simpleText")
            } catch (e: JSONException) {
                null
            }
        }

    val keywords: String
        get() {
            htmlUrl = channelUrl
            return getJsonObj().getJSONObject("metadata")
                .getJSONObject("channelMetadataRenderer")
                .getString("keywords")
        }

    val availableCountryCodes: JSONArray
        get() {
            htmlUrl = channelUrl
            return getJsonObj().getJSONObject("metadata")
                .getJSONObject("channelMetadataRenderer")
                .getJSONArray("availableCountryCodes")
        }

    val thumbnailUrl: String
        get() {
            htmlUrl = channelUrl
            return getJsonObj().getJSONObject("metadata")
                .getJSONObject("channelMetadataRenderer")
                .getJSONObject("avatar")
                .getJSONArray("thumbnails")
                .getJSONObject(0)
                .getString("url")
        }
}