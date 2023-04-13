package javatube

class CaptionQuery(captions: List<Captions>) {
    var langCodeIndex: MutableMap<String, Captions> = HashMap()

    init {
        for (code in captions) {
            langCodeIndex[code.code] = code
        }
    }

    fun getByCode(code: String): Captions {
        return langCodeIndex[code]!!
    }
}