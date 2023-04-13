package javatube

import java.util.regex.Pattern
import javax.script.ScriptEngineManager
import javax.script.ScriptException

class Cipher(js: String) {
    init {
        transformPlan = getTransformPlan(js)
        val varMatcher = listOf(
            *transformPlan[0].split("\\.".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        )[0]
        transformMap = getTransformMap(js, varMatcher)
        jsFuncPatterns =
            listOf("\\w+\\.(\\w+)\\(\\w,(\\d+)\\)", "\\w+\\[(\\\"\\w+\\\")\\]\\(\\w,(\\d+)\\)")

        // jsFuncPatterns =  \w+\.(\w+)\(\w,(\d+)\)   |    \w+\[(\"\w+\")\]\(\w,(\d+)\)
        throttlingFunctionName = getThrottlingFunctionName(js) // "nma"
        throttlingRawCode = getThrottlingFunctionCode(js)
    }

    fun getSignature(cipherSignature: List<String>): String {
        var cipherSignature = cipherSignature
        for (jsFunc in transformPlan) {
            var name: String
            var argument: Int
            val returnParse = parseFunction(jsFunc)
            name = returnParse[0]
            argument = returnParse[1].toInt()
            cipherSignature = transform(name, cipherSignature, argument)
        }

        return cipherSignature.joinToString(", ").replace(", ", "")
    }

    @Throws(Exception::class)
    private fun getThrottlingFunctionCode(js: String): String {
        val regex =
            Pattern.compile("$throttlingFunctionName=function\\(\\w\\)(\\{.*?return b.join\\(\\\"\\\"\\)\\}\\;)")
        val matcher = regex.matcher(js)
        if (matcher.find()) {
            return matcher.group(1)
        }
        throw Exception("RegexMatcherError")
    }

    @Throws(Exception::class)
    private fun getThrottlingFunctionName(js: String): String {
        val functionPatterns = arrayOf(
            "a\\.[a-zA-Z]\\s*&&\\s*\\([a-z]\\s*=\\s*a\\.get\\(\\\"n\\\"\\)\\)\\s*&&\\s*\\([a-z]=([a-zA-Z]*\\[\\d\\]).*?\\)"
        )
        for (pattern in functionPatterns) {
            val regex = Pattern.compile(pattern)
            val matcher = regex.matcher(js)
            if (matcher.find()) {
                val idx = matcher.group(1)
                val funName = matcher.group(1).replace("(\\[\\d\\])".toRegex(), "")
                if (!idx.isEmpty()) {
                    val regex2 = Pattern.compile("var $funName\\s*=\\s*(\\[.+?\\]);")
                    val matcher2 = regex2.matcher(js)
                    if (matcher2.find()) {
                        return matcher2.group(1).replace("[", "").replace("]", "")
                    }
                }
            }
        }
        throw Exception("RegexMatcherError")
    }

    // _W6Lyun6KYjsE7k1p80
    // N_e5OtmzoBALWbqr2Wz
    @Throws(ScriptException::class)
    fun calculateN(n: String): String {
        val engine = ScriptEngineManager().getEngineByName("js")
        engine.eval(throttlingFunctionName + "=function(a)" + throttlingRawCode + "var b = " + throttlingFunctionName + "('" + n + "')")
        return engine["b"] as String
    }

    companion object {
        private lateinit var transformPlan: List<String>
        private lateinit var transformMap: Map<String, String>
        private lateinit var jsFuncPatterns: List<String>
        private lateinit var throttlingFunctionName: String
        private lateinit var throttlingRawCode: String

        private fun getTransformPlan(js: String): List<String> {
            val name = getInitialFunctionName(js)
            val pattern =
                "$name=function\\(\\w\\)\\{[a-z]=[a-z]\\.[a-z]*\\(\\\"\\\"\\);([\\w*\\.\\w*\\(\\w,\\d\\);]*)(?:return)"
            //{"kD.EC(a,1)", "kD.UT(a,60)", "kD.gp(a,55)", "kD.UT(a,45)", "kD.EC(a,3)", "kD.UT(a,28)", "kD.gp(a,63)"};
            val regex = Pattern.compile(pattern)
            val matcher = regex.matcher(js)
            if (matcher.find()) {
                return matcher.group(1).split(";".toRegex()).dropLastWhile { it.isEmpty() }
            }
            throw Exception("RegexMatcherError: $pattern")
        }

        private fun mapFunction(jsFunc: String): String {
            val mapper = arrayOf(
                arrayOf("\\{\\w\\.reverse\\(\\)\\}", "reverse"),
                arrayOf("\\{\\w\\.splice\\(0,\\w\\)\\}", "splice"),
                arrayOf(
                    "\\{var\\s\\w=\\w\\[0\\];\\w\\[0\\]=\\w\\[\\w\\%\\w.length\\];\\w\\[\\w\\]=\\w\\}",
                    "swap"
                ),
                arrayOf(
                    "\\{var\\s\\w=\\w\\[0\\];\\w\\[0\\]=\\w\\[\\w\\%\\w.length\\];\\w\\[\\w\\%\\w.length\\]=\\w\\}",
                    "swap"
                )
            )
            for (i in 0..3) {
                val regex = Pattern.compile(mapper[i][0])
                val matcher = regex.matcher(jsFunc)
                if (matcher.find()) {
                    return mapper[i][1]
                }
            }
            throw Exception("RegexMatcherError")
        }

        @Throws(Exception::class)
        private fun getTransformMap(js: String, `var`: String): Map<String, String> {
            val transformObject = getTransformObject(js, `var`)
            //{"UT:function(a,b){var c=a[0];a[0]=a[b%a.length];a[b%a.length]=c}", "gp:function(a){a.reverse()}", "EC:function(a,b){a.splice(0,b)}"}
            val mapper = mutableMapOf<String, String>()
            for (obj in transformObject) {
                val name = obj.split(":".toRegex()).dropLastWhile { it.isEmpty() }[0]
                val function =
                    obj.split(":".toRegex()).dropLastWhile { it.isEmpty() }[1]
                mapper[name] = mapFunction(function)
            }
            return mapper
        }

        @Throws(Exception::class)
        private fun getTransformObject(js: String, `var`: String): List<String> {
            val pattern = "var $`var`=\\{(.*?)\\};"
            val regex = Pattern.compile(pattern)
            val matcher = regex.matcher(js)
            return if (matcher.find()) {
                matcher.group(1).replace("(\\}\\,)".toRegex(), "}, ").split(", ".toRegex())
                    .dropLastWhile { it.isEmpty() }
            } else {
                throw Exception("RegexMatcherError: $pattern")
            }
        }

        private fun getInitialFunctionName(js: String): String {
            val functionPattern = arrayOf(
                "\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
                "\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
                "(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)",
                "([a-zA-Z0-9$]+)\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)",
                "([\"\\'])signature\\1\\s*,\\s*([a-zA-Z0-9$]+)\\(",
                "\\.sig\\|\\|([a-zA-Z0-9$]+)\\(",
                "yt\\.akamaized\\.net/\\)\\s*\\|\\|\\s*.*?\\s*[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?\\s*([a-zA-Z0-9$]+)\\(",
                "\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\(",
                "\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\(",
                "\\bc\\s*&&\\s*a\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
                "\\bc\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
                "\\bc\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\("
            )
            for (pattern in functionPattern) {
                val regex = Pattern.compile(pattern)
                val matcher = regex.matcher(js)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }
            throw Exception("RegexMatcherError")
        }

        private fun parseFunction(jsFunc: String): List<String> {
            var fnName: String? = null
            var fnArg: String? = null
            for (pattern in jsFuncPatterns) {
                val regex = Pattern.compile(pattern)
                val matcher = regex.matcher(jsFunc)
                if (matcher.find()) {
                    fnName = matcher.group(1)
                    fnArg = matcher.group(2)
                }
            }
            return listOfNotNull(fnName, fnArg)
        }

        private fun transform(
            name: String?,
            signature: List<String>,
            argument: Int
        ): List<String> {
            return when (transformMap[name]) {
                "reverse" -> {
                    signature.reversed()
                }

                "splice" -> {
                    splice(signature, argument)
                }

                "swap" -> {
                    swap(signature, argument)
                }

                else -> {
                    signature
                }
            }
        }

        private fun splice(arr: List<String>, b: Int): List<String> {
            var arr = arr.toMutableList()
            var b = b
            while (b > 0) {
                arr.removeAt(0)
                b--
            }
            return arr
        }

        private fun swap(arr: List<String>, b: Int): List<String> {
            val arr = arr.toMutableList()
            val temp = arr[b]
            arr[b] = arr[0]
            arr[0] = temp
            return arr
        }
    }
}