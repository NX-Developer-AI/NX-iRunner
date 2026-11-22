package com.nxteam.irunner.ipa

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSNumber
import com.dd.plist.NSObject
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class IpaMetadata(
    val bundleIdentifier: String,
    val displayName: String,
    val bundleVersion: String,
    val shortVersion: String?,
    val minimumOsVersion: String?,
    val executableName: String,
    val deviceFamilies: List<Int>,
    val machO: MachOSummary?,
    val iconPng: ByteArray?,
    val bundleDirectory: String,
) {
    val visibleVersion: String get() = shortVersion ?: bundleVersion
}

class IpaFormatException(val reason: Reason, message: String) : Exception(message) {
    enum class Reason {
        NOT_A_ZIP,
        MISSING_APP_BUNDLE,
        MISSING_INFO_PLIST,
        INVALID_INFO_PLIST,
        MISSING_EXECUTABLE,
    }
}

object IpaInspector {
    private val infoPlistPattern = Regex("^Payload/([^/]+\\.app)/Info\\.plist$", RegexOption.IGNORE_CASE)
    private const val MAXIMUM_ICON_BYTES = 4 * 1024 * 1024

    fun inspect(ipaFile: File, workingDirectory: File): IpaMetadata {
        val zipFile = try {
            ZipFile(ipaFile)
        } catch (exception: Exception) {
            throw IpaFormatException(IpaFormatException.Reason.NOT_A_ZIP, exception.message ?: "Not a zip archive")
        }
        zipFile.use { archive ->
            val entries = archive.entries().toList()
            val infoPlistEntry = entries.firstOrNull { entry -> infoPlistPattern.matches(entry.name) }
                ?: throw if (entries.any { entry -> entry.name.startsWith("Payload/") && entry.name.contains(".app/") }) {
                    IpaFormatException(IpaFormatException.Reason.MISSING_INFO_PLIST, "Info.plist not found")
                } else {
                    IpaFormatException(IpaFormatException.Reason.MISSING_APP_BUNDLE, "Payload/*.app not found")
                }
            val bundleDirectory = infoPlistEntry.name.substringBeforeLast("Info.plist")
            val properties = parseDictionary(archive.getInputStream(infoPlistEntry).use { stream -> stream.readBytes() })

            val executableName = properties.stringValue("CFBundleExecutable")
                ?: throw IpaFormatException(IpaFormatException.Reason.MISSING_EXECUTABLE, "CFBundleExecutable missing")
            val bundleIdentifier = properties.stringValue("CFBundleIdentifier")
                ?: bundleDirectory.removePrefix("Payload/").removeSuffix(".app/")
            val displayName = properties.stringValue("CFBundleDisplayName")
                ?: properties.stringValue("CFBundleName")
                ?: executableName
            val bundleVersion = properties.stringValue("CFBundleVersion") ?: "?"
            val shortVersion = properties.stringValue("CFBundleShortVersionString")
            val minimumOsVersion = properties.stringValue("MinimumOSVersion")
                ?: properties.stringValue("LSMinimumSystemVersion")
            val deviceFamilies = properties.integerList("UIDeviceFamily")

            val executableEntry = archive.getEntry(bundleDirectory + executableName)
            val machO = executableEntry?.let { entry -> inspectExecutable(archive, entry, workingDirectory) }

            val iconPng = findIconBytes(archive, entries, bundleDirectory, properties)
                ?.let { bytes -> CgbiPngConverter.toStandardPng(bytes) }

            return IpaMetadata(
                bundleIdentifier = bundleIdentifier,
                displayName = displayName,
                bundleVersion = bundleVersion,
                shortVersion = shortVersion,
                minimumOsVersion = minimumOsVersion,
                executableName = executableName,
                deviceFamilies = deviceFamilies,
                machO = machO,
                iconPng = iconPng,
                bundleDirectory = bundleDirectory,
            )
        }
    }

    private fun parseDictionary(bytes: ByteArray): NSDictionary {
        val parsed = try {
            PropertyListParser.parse(bytes)
        } catch (exception: Exception) {
            throw IpaFormatException(IpaFormatException.Reason.INVALID_INFO_PLIST, exception.message ?: "Invalid plist")
        }
        return parsed as? NSDictionary
            ?: throw IpaFormatException(IpaFormatException.Reason.INVALID_INFO_PLIST, "Info.plist root is not a dictionary")
    }

    private fun inspectExecutable(archive: ZipFile, entry: ZipEntry, workingDirectory: File): MachOSummary? {
        workingDirectory.mkdirs()
        val temporaryFile = File.createTempFile("executable", ".macho", workingDirectory)
        return try {
            archive.getInputStream(entry).use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            }
            runCatching { MachOInspector.inspect(temporaryFile) }.getOrNull()
        } finally {
            temporaryFile.delete()
        }
    }

    private fun findIconBytes(
        archive: ZipFile,
        entries: List<ZipEntry>,
        bundleDirectory: String,
        properties: NSDictionary,
    ): ByteArray? {
        val declaredNames = buildList {
            addAll(properties.stringList("CFBundleIconFiles"))
            val primaryIcon = (properties.objectForKey("CFBundleIcons") as? NSDictionary)
                ?.objectForKey("CFBundlePrimaryIcon") as? NSDictionary
            primaryIcon?.let { addAll(it.stringList("CFBundleIconFiles")) }
            properties.stringValue("CFBundleIconFile")?.let { add(it) }
            add("Icon.png")
            add("icon.png")
        }
        val bundleEntries = entries.filter { entry ->
            !entry.isDirectory &&
                entry.name.startsWith(bundleDirectory) &&
                !entry.name.removePrefix(bundleDirectory).contains('/')
        }
        val candidates = mutableListOf<ZipEntry>()
        for (name in declaredNames) {
            val baseName = name.removeSuffix(".png").removeSuffix(".PNG")
            val matches = bundleEntries.filter { entry ->
                val fileName = entry.name.removePrefix(bundleDirectory)
                fileName.equals("$baseName.png", ignoreCase = true) ||
                    (fileName.startsWith(baseName, ignoreCase = true) && fileName.endsWith(".png", ignoreCase = true))
            }
            candidates.addAll(matches)
        }
        if (candidates.isEmpty()) {
            candidates.addAll(
                bundleEntries.filter { entry ->
                    val fileName = entry.name.removePrefix(bundleDirectory)
                    fileName.startsWith("Icon", ignoreCase = true) && fileName.endsWith(".png", ignoreCase = true)
                },
            )
        }
        val chosen = candidates
            .filter { entry -> entry.size in 1..MAXIMUM_ICON_BYTES.toLong() || entry.size == -1L }
            .maxByOrNull { entry -> entry.size }
            ?: return null
        return archive.getInputStream(chosen).use { stream -> stream.readBytes() }
    }

    private fun NSDictionary.stringValue(key: String): String? =
        (objectForKey(key) as? NSString)?.content?.takeIf { value -> value.isNotBlank() }

    private fun NSDictionary.stringList(key: String): List<String> =
        when (val value: NSObject? = objectForKey(key)) {
            is NSArray -> value.array.mapNotNull { item -> (item as? NSString)?.content }
            is NSString -> listOf(value.content)
            else -> emptyList()
        }

    private fun NSDictionary.integerList(key: String): List<Int> =
        when (val value: NSObject? = objectForKey(key)) {
            is NSArray -> value.array.mapNotNull { item ->
                when (item) {
                    is NSNumber -> item.intValue()
                    is NSString -> item.content.toIntOrNull()
                    else -> null
                }
            }
            is NSNumber -> listOf(value.intValue())
            else -> emptyList()
        }
}
