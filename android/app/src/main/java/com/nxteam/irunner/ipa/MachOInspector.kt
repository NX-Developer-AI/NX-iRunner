package com.nxteam.irunner.ipa

import java.io.File
import java.io.RandomAccessFile

data class MachOSlice(
    val architecture: String,
    val cpuType: Int,
    val cpuSubtype: Int,
    val is64Bit: Boolean,
    val isEncrypted: Boolean,
)

data class MachOSummary(val slices: List<MachOSlice>) {
    val architectures: List<String> get() = slices.map { slice -> slice.architecture }
    val hasRunnable32BitArmSlice: Boolean
        get() = slices.any { slice -> slice.cpuType == MachOInspector.CPU_TYPE_ARM && !slice.isEncrypted }
    val has32BitArmSlice: Boolean
        get() = slices.any { slice -> slice.cpuType == MachOInspector.CPU_TYPE_ARM }
}

class MachOFormatException(message: String) : Exception(message)

object MachOInspector {
    const val CPU_TYPE_ARM = 12
    const val CPU_TYPE_ARM64 = 0x0100000C
    const val CPU_TYPE_X86 = 7
    const val CPU_TYPE_X86_64 = 0x01000007

    private const val FAT_MAGIC = 0xCAFEBABE.toInt()
    private const val FAT_MAGIC_64 = 0xCAFEBABF.toInt()
    private const val MACH_MAGIC_32 = 0xFEEDFACE.toInt()
    private const val MACH_MAGIC_64 = 0xFEEDFACF.toInt()
    private const val LOAD_COMMAND_ENCRYPTION_INFO = 0x21
    private const val LOAD_COMMAND_ENCRYPTION_INFO_64 = 0x2C
    private const val MAXIMUM_FAT_ARCHITECTURES = 64
    private const val MAXIMUM_LOAD_COMMANDS = 4096

    fun inspect(file: File): MachOSummary = RandomAccessFile(file, "r").use { reader ->
        val magic = readBigEndianInt(reader, 0)
        when (magic) {
            FAT_MAGIC, FAT_MAGIC_64 -> inspectFat(reader, magic == FAT_MAGIC_64)
            else -> MachOSummary(listOf(inspectThin(reader, 0)))
        }
    }

    private fun inspectFat(reader: RandomAccessFile, is64BitFat: Boolean): MachOSummary {
        val architectureCount = readBigEndianInt(reader, 4)
        if (architectureCount <= 0 || architectureCount > MAXIMUM_FAT_ARCHITECTURES) {
            throw MachOFormatException("Invalid fat architecture count: $architectureCount")
        }
        val entrySize = if (is64BitFat) 32 else 20
        val slices = (0 until architectureCount).map { index ->
            val entryOffset = 8L + index * entrySize
            val sliceOffset = if (is64BitFat) {
                readBigEndianLong(reader, entryOffset + 8)
            } else {
                readBigEndianInt(reader, entryOffset + 8).toLong() and 0xFFFFFFFFL
            }
            inspectThin(reader, sliceOffset)
        }
        return MachOSummary(slices)
    }

    private fun inspectThin(reader: RandomAccessFile, offset: Long): MachOSlice {
        val magic = readLittleEndianInt(reader, offset)
        val is64Bit = when (magic) {
            MACH_MAGIC_32 -> false
            MACH_MAGIC_64 -> true
            else -> throw MachOFormatException("Unknown Mach-O magic 0x${Integer.toHexString(magic)}")
        }
        val cpuType = readLittleEndianInt(reader, offset + 4)
        val cpuSubtype = readLittleEndianInt(reader, offset + 8) and 0x00FFFFFF
        val loadCommandCount = readLittleEndianInt(reader, offset + 16)
        if (loadCommandCount < 0 || loadCommandCount > MAXIMUM_LOAD_COMMANDS) {
            throw MachOFormatException("Invalid load command count: $loadCommandCount")
        }
        var commandOffset = offset + if (is64Bit) 32 else 28
        var isEncrypted = false
        repeat(loadCommandCount) {
            val command = readLittleEndianInt(reader, commandOffset)
            val commandSize = readLittleEndianInt(reader, commandOffset + 4)
            if (command == LOAD_COMMAND_ENCRYPTION_INFO || command == LOAD_COMMAND_ENCRYPTION_INFO_64) {
                val cryptIdentifier = readLittleEndianInt(reader, commandOffset + 16)
                if (cryptIdentifier != 0) {
                    isEncrypted = true
                }
            }
            if (commandSize < 8) {
                throw MachOFormatException("Invalid load command size: $commandSize")
            }
            commandOffset += commandSize
        }
        return MachOSlice(
            architecture = architectureName(cpuType, cpuSubtype),
            cpuType = cpuType,
            cpuSubtype = cpuSubtype,
            is64Bit = is64Bit,
            isEncrypted = isEncrypted,
        )
    }

    fun architectureName(cpuType: Int, cpuSubtype: Int): String = when (cpuType) {
        CPU_TYPE_ARM -> when (cpuSubtype) {
            5 -> "armv4t"
            6 -> "armv6"
            7 -> "armv5"
            8 -> "xscale"
            9 -> "armv7"
            10 -> "armv7f"
            11 -> "armv7s"
            12 -> "armv7k"
            14 -> "armv6m"
            15 -> "armv7m"
            16 -> "armv7em"
            else -> "arm($cpuSubtype)"
        }
        CPU_TYPE_ARM64 -> when (cpuSubtype) {
            2 -> "arm64e"
            else -> "arm64"
        }
        CPU_TYPE_X86 -> "i386"
        CPU_TYPE_X86_64 -> "x86_64"
        else -> "cpu($cpuType)"
    }

    private fun readBytes(reader: RandomAccessFile, position: Long, count: Int): ByteArray {
        if (position < 0 || position + count > reader.length()) {
            throw MachOFormatException("Read outside of file at $position")
        }
        val buffer = ByteArray(count)
        reader.seek(position)
        reader.readFully(buffer)
        return buffer
    }

    private fun readBigEndianInt(reader: RandomAccessFile, position: Long): Int {
        val bytes = readBytes(reader, position, 4)
        return ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or
            (bytes[3].toInt() and 0xFF)
    }

    private fun readBigEndianLong(reader: RandomAccessFile, position: Long): Long {
        val high = readBigEndianInt(reader, position).toLong() and 0xFFFFFFFFL
        val low = readBigEndianInt(reader, position + 4).toLong() and 0xFFFFFFFFL
        return (high shl 32) or low
    }

    private fun readLittleEndianInt(reader: RandomAccessFile, position: Long): Int {
        val bytes = readBytes(reader, position, 4)
        return (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8) or
            ((bytes[2].toInt() and 0xFF) shl 16) or
            ((bytes[3].toInt() and 0xFF) shl 24)
    }
}
