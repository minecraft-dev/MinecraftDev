package com.demonwav.mcdev.creator

import com.demonwav.mcdev.update.PluginUtil
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import java.io.IOException
import java.util.function.Predicate
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent

@Throws(IOException::class)
suspend fun collectMavenVersions(url: String, filter: Predicate<String> = Predicate { true }): List<String> {
    val manager = FuelManager()
    manager.proxy = selectProxy(url)

    val response = manager.get(url)
        .header("User-Agent", PluginUtil.useragent)
        .allowRedirects(true)
        .suspendable()
        .await()

    val result = mutableListOf<String>()
    response.body().toStream().use { stream ->
        val inputFactory = XMLInputFactory.newInstance()

        @Suppress("UNCHECKED_CAST")
        val reader = inputFactory.createXMLEventReader(stream) as Iterator<XMLEvent>
        for (event in reader) {
            if (!event.isStartElement) {
                continue
            }
            val start = event.asStartElement()
            val name = start.name.localPart
            if (name != "version") {
                continue
            }

            val versionEvent = reader.next()
            if (!versionEvent.isCharacters) {
                continue
            }

            val version = versionEvent.asCharacters().data
            if (filter.test(version)) {
                result += version
            }
        }
    }

    return result
}
