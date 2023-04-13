package javatube

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Search(private val query: String) {
    private fun safeQuery(): String {
        return URLEncoder.encode(query, StandardCharsets.UTF_8)
    }

    private fun baseData(): String {
        return "{\"context\": {\"client\": {\"clientName\": \"WEB\", \"clientVersion\": \"2.20200720.00.02\"}}}"
    }

    private fun baseParam(): String {
        return "https://www.youtube.com/youtubei/v1/search?query=" + safeQuery() + "&key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8&contentCheckOk=True&racyCheckOk=True"
    }

    @Throws(IOException::class)
    private fun fetchQuery(): String {
        return InnerTube.post(baseParam(), baseData())
    }

    @Throws(Exception::class)
    private fun fetchAndParse(): List<Youtube> {
        val rawResults = JSONObject(fetchQuery())
        val sections = rawResults.getJSONObject("contents")
            .getJSONObject("twoColumnSearchResultsRenderer")
            .getJSONObject("primaryContents")
            .getJSONObject("sectionListRenderer")
            .getJSONArray("contents")
            .getJSONObject(0)
        val rawVideoList = JSONArray(
            sections.getJSONObject("itemSectionRenderer")
                .getJSONArray("contents")
        )
        val videos = mutableListOf<Youtube>()
        for (i in 0 until rawVideoList.length() - 1) {
            if (!rawVideoList.getJSONObject(i).has("videoRenderer")) {
                continue
            }
            val vidRenderer = rawVideoList.getJSONObject(i).getJSONObject("videoRenderer")
            val vidId = vidRenderer.getString("videoId")
            val vidUrl = "https://www.youtube.com/watch?v=$vidId"
            videos.add(Youtube(vidUrl))
        }
        return videos
    }

    @Throws(Exception::class)
    fun results(): List<Youtube> {
        return fetchAndParse()
    }
}