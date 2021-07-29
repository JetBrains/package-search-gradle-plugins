import java.util.*

val localProperties = file("local.properties")

allprojects {
    group = "com.github.lamba92"
    version = "0.0.1"

    localProperties.takeIf { it.exists() }
        ?.let { Properties().apply { load(it.inputStream().buffered()) } }
        ?.forEach { (key, value) ->
            extra[key.toString()] = value.toString()
        }
}
