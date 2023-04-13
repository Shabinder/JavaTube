package javatube

import java.util.LinkedList

class StreamQuery(val all: List<Stream>) {
    var itagIndex: MutableMap<Int?, Stream> = HashMap()

    init {
        for (fmt_stream in all) {
            itagIndex[fmt_stream.itag] = fmt_stream
        }
    }

    fun filter(filters: MutableMap<String, String>): StreamQuery {
        val streamFilter = mutableListOf<Stream>()
        if (filters.containsKey("res")) {
            if (streamFilter.isNotEmpty()) {
                streamFilter.retainAll(getResolution(filters["res"].toString()))
            } else {
                streamFilter.addAll(getResolution(filters["res"].toString()))
            }
            if (streamFilter.isEmpty()) {
                filters.clear()
            }
        }
        if (filters.containsKey("fps")) {
            if (streamFilter.isNotEmpty()) {
                streamFilter.retainAll(getFps(filters["fps"]!!))
            } else {
                streamFilter.addAll(getFps(filters["fps"]!!))
            }
            if (streamFilter.isEmpty()) {
                filters.clear()
            }
        }
        if (filters.containsKey("mineType")) {
            if (streamFilter.isNotEmpty()) {
                streamFilter.retainAll(getMineType(filters["mineType"]!!))
            } else {
                streamFilter.addAll(getMineType(filters["mineType"]!!))
            }
            if (streamFilter.isEmpty()) {
                filters.clear()
            }
        }
        if (filters.containsKey("type")) {
            if (streamFilter.isNotEmpty()) {
                streamFilter.retainAll(getType(filters["type"]!!))
            } else {
                streamFilter.addAll(getType(filters["type"]!!))
            }
            if (streamFilter.isEmpty()) {
                filters.clear()
            }
        }
        if (filters.containsKey("subType")) {
            if (streamFilter.isNotEmpty()) {
                streamFilter.retainAll(getSubtype(filters["subType"]!!))
            } else {
                streamFilter.addAll(getSubtype(filters["subType"]!!))
            }
            if (streamFilter.isEmpty()) {
                filters.clear()
            }
        }
        if (filters.containsKey("abr")) {
            if (streamFilter.isNotEmpty()) {
                streamFilter.retainAll(getAbr(filters["abr"]!!))
            } else {
                streamFilter.addAll(getAbr(filters["abr"]!!))
            }
            if (streamFilter.isEmpty()) {
                filters.clear()
            }
        }
        if (filters.containsKey("videoCodec")) {
            if (streamFilter.isNotEmpty()) {
                streamFilter.retainAll(getVideoCodec(filters["videoCodec"]!!))
            } else {
                streamFilter.addAll(getVideoCodec(filters["videoCodec"]!!))
            }
            if (streamFilter.isEmpty()) {
                filters.clear()
            }
        }
        if (filters.containsKey("audioCodec")) {
            if (streamFilter.isNotEmpty()) {
                streamFilter.retainAll(getAudioCodec(filters["audioCodec"]!!))
            } else {
                streamFilter.addAll(getAudioCodec(filters["audioCodec"]!!))
            }
            if (streamFilter.isEmpty()) {
                filters.clear()
            }
        }
        if (filters.containsKey("onlyAudio")) {
            if (filters["onlyAudio"] == "true") {
                if (streamFilter.isNotEmpty()) {
                    streamFilter.retainAll(onlyAudio())
                } else {
                    streamFilter.addAll(onlyAudio())
                }
                if (streamFilter.isEmpty()) {
                    filters.clear()
                }
            }
        }
        if (filters.containsKey("onlyVideo")) {
            if (filters["onlyVideo"] == "true") {
                if (streamFilter.isNotEmpty()) {
                    streamFilter.retainAll(onlyVideo())
                } else {
                    streamFilter.addAll(onlyVideo())
                }
                if (streamFilter.isEmpty()) {
                    filters.clear()
                }
            }
        }
        if (filters.containsKey("progressive")) {
            if (filters["progressive"] == "true") {
                if (streamFilter.isNotEmpty()) {
                    streamFilter.retainAll(progressive)
                } else {
                    streamFilter.addAll(progressive)
                }
                if (streamFilter.isEmpty()) {
                    filters.clear()
                }
            } else if (filters["progressive"] == "false") {
                if (streamFilter.isNotEmpty()) {
                    streamFilter.retainAll(adaptive)
                } else {
                    streamFilter.addAll(adaptive)
                }
                if (streamFilter.isEmpty()) {
                    filters.clear()
                }
            }
        }
        if (filters.containsKey("adaptive")) {
            if (filters["adaptive"] == "true") {
                if (streamFilter.isNotEmpty()) {
                    streamFilter.retainAll(adaptive)
                } else {
                    streamFilter.addAll(adaptive)
                }
                if (streamFilter.isEmpty()) {
                    filters.clear()
                }
            } else if (filters["adaptive"] == "false") {
                if (streamFilter.isNotEmpty()) {
                    streamFilter.retainAll(progressive)
                } else {
                    streamFilter.addAll(progressive)
                }
                if (streamFilter.isEmpty()) {
                    filters.clear()
                }
            }
        }
        return StreamQuery(streamFilter)
    }

    private fun getResolution(re: String): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.resolution == re) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun getFps(fps: String): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.fps == fps.toInt()) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun getMineType(mineType: String): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.mimeType == mineType) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun getType(type: String): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.type == type) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun getSubtype(subtype: String): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.subType == subtype) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun getAbr(abr: String): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.abr == abr) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun getVideoCodec(videoCodec: String): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.videoCodec == videoCodec) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun getAudioCodec(audioCodec: String): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.audioCodec == audioCodec) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun onlyAudio(): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.includeAudioTrack() && !st.includeVideoTrack()) {
                filter.add(st)
            }
        }
        return filter
    }

    private fun onlyVideo(): List<Stream> {
        val filter = mutableListOf<Stream>()
        for (st in all) {
            if (st.includeVideoTrack() && !st.includeAudioTrack()) {
                filter.add(st)
            }
        }
        return filter
    }

    private val progressive: List<Stream>
        private get() {
            val filter = mutableListOf<Stream>()
            for (st in all) {
                if (st.isProgressive) {
                    filter.add(st)
                }
            }
            return filter
        }
    private val adaptive: List<Stream>
        get() {
            val filter = mutableListOf<Stream>()
            for (st in all) {
                if (st.isAdaptive) {
                    filter.add(st)
                }
            }
            return filter
        }


    @Throws(Exception::class)
    fun orderBy(by: String?): StreamQuery {
        val map = HashMap<Stream, Int>()
        for (s in all) {
            if (by == "res") {
                if (s.resolution != null) {
                    map[s] = s.resolution.replace("p", "").toInt()
                }
            } else if (by == "abr") {
                if (s.abr != null) {
                    map[s] = s.abr.replace("kbps", "").toInt()
                }
            } else if (by == "fps") {
                if (s.fps != null) {
                    map[s] = s.fps!!
                }
            } else {
                throw Exception("InvalidParameter")
            }
        }
        return StreamQuery(sortByValue(map))
    }

    fun getOtf(otf: Boolean): StreamQuery {
        val filter = mutableListOf<Stream>()
        for (s in all) {
            if (otf) {
                if (s.isOtf) {
                    filter.add(s)
                }
            } else {
                if (!s.isOtf) {
                    filter.add(s)
                }
            }
        }
        return StreamQuery(filter)
    }

    val desc: StreamQuery
        get() = StreamQuery(all.reversed())
    val asc: StreamQuery
        get() = StreamQuery(all)
    val first: Stream
        get() = all[0]
    val last: Stream
        get() = all[all.size - 1]
    val onlyAudio: Stream
        get() {
            val filters = HashMap<String, String>()
            filters["onlyAudio"] = "true"
            filters["subType"] = "mp4"
            return filter(filters).last
        }
    val lowestResolution: Stream
        get() {
            val filters = HashMap<String, String>()
            filters["progressive"] = "true"
            filters["subType"] = "mp4"
            return filter(filters).first
        }
    val highestResolution: Stream
        get() {
            val filters = HashMap<String, String>()
            filters["progressive"] = "true"
            filters["subType"] = "mp4"
            return filter(filters).last
        }

    companion object {
        private fun sortByValue(hm: HashMap<Stream, Int>): List<Stream> {
            val list: List<Map.Entry<Stream, Int>> =
                LinkedList<Map.Entry<Stream, Int>>(hm.entries).sortedWith { o1, o2 ->
                    val v1 = o1.value
                    val v2 = o2.value
                    v2.compareTo(v1)
                }
            val ordered = mutableListOf<Stream>()
            for ((key) in list) {
                ordered.add(key)
            }
            return ordered
        }
    }
}