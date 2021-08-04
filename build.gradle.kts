import java.util.*

val localProperties = file("local.properties")

allprojects {
    group = "org.jetbrains.gradle"
    version = "0.0.1"

    localProperties.takeIf { it.exists() }?.let { extra.setAll(Properties(it)) }
}

fun Properties(from: File) = Properties().apply { load(from.inputStream().buffered()) }

fun <T : Any, R> ExtraPropertiesExtension.setAll(map: Map<T, R>) =
    map.forEach { (k, v) -> set(k.toString(), v.toString()) }
