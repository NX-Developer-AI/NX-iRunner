package com.nxteam.irunner.ipa

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.Inflater

object CgbiPngConverter {
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )
    private const val COLOR_TYPE_RGB = 2
    private const val COLOR_TYPE_RGBA = 6

    fun isCgbi(data: ByteArray): Boolean =
        data.size > 16 && hasPngSignature(data) && chunkType(data, 8) == "CgBI"

    fun decodeBitmap(data: ByteArray): Bitmap? {
        if (!hasPngSignature(data)) {
            return null
        }
        return if (isCgbi(data)) {
            runCatching { decodeCgbi(data) }.getOrNull()
        } else {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        }
    }

    fun toStandardPng(data: ByteArray): ByteArray? {
        val bitmap = decodeBitmap(data) ?: return null
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun hasPngSignature(data: ByteArray): Boolean =
        data.size >= pngSignature.size && pngSignature.indices.all { index -> data[index] == pngSignature[index] }

    private fun chunkType(data: ByteArray, offset: Int): String =
        String(data, offset + 4, 4, Charsets.US_ASCII)

    private fun decodeCgbi(data: ByteArray): Bitmap? {
        val buffer = ByteBuffer.wrap(data)
        var offset = pngSignature.size
        var width = 0
        var height = 0
        var colorType = -1
        var interlaceMethod = 0
        val compressedImage = ByteArrayOutputStream()
        while (offset + 12 <= data.size) {
            val length = buffer.getInt(offset)
            if (length < 0 || offset + 12 + length > data.size) {
                return null
            }
            val type = chunkType(data, offset)
            val dataOffset = offset + 8
            when (type) {
                "IHDR" -> {
                    width = buffer.getInt(dataOffset)
                    height = buffer.getInt(dataOffset + 4)
                    val bitDepth = data[dataOffset + 8].toInt() and 0xFF
                    colorType = data[dataOffset + 9].toInt() and 0xFF
                    interlaceMethod = data[dataOffset + 12].toInt() and 0xFF
                    if (bitDepth != 8) {
                        return null
                    }
                }
                "IDAT" -> compressedImage.write(data, dataOffset, length)
                "IEND" -> break
            }
            offset += 12 + length
        }
        if (width <= 0 || height <= 0 || interlaceMethod != 0) {
            return null
        }
        val bytesPerPixel = when (colorType) {
            COLOR_TYPE_RGBA -> 4
            COLOR_TYPE_RGB -> 3
            else -> return null
        }
        val stride = width * bytesPerPixel
        val filtered = inflateRaw(compressedImage.toByteArray(), (stride + 1) * height) ?: return null
        val pixels = unfilter(filtered, width, height, bytesPerPixel) ?: return null
        val colors = IntArray(width * height)
        for (index in colors.indices) {
            val base = index * bytesPerPixel
            val blue = pixels[base].toInt() and 0xFF
            val green = pixels[base + 1].toInt() and 0xFF
            val red = pixels[base + 2].toInt() and 0xFF
            val alpha = if (bytesPerPixel == 4) pixels[base + 3].toInt() and 0xFF else 0xFF
            colors[index] = if (alpha == 0) {
                0
            } else if (alpha == 0xFF) {
                (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            } else {
                val straightRed = minOf(255, red * 255 / alpha)
                val straightGreen = minOf(255, green * 255 / alpha)
                val straightBlue = minOf(255, blue * 255 / alpha)
                (alpha shl 24) or (straightRed shl 16) or (straightGreen shl 8) or straightBlue
            }
        }
        return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun inflateRaw(compressed: ByteArray, expectedSize: Int): ByteArray? {
        val inflater = Inflater(true)
        return try {
            inflater.setInput(compressed)
            val output = ByteArray(expectedSize)
            var written = 0
            while (written < expectedSize && !inflater.finished()) {
                val count = inflater.inflate(output, written, expectedSize - written)
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break
                }
                written += count
            }
            if (written == expectedSize) output else null
        } finally {
            inflater.end()
        }
    }

    private fun unfilter(filtered: ByteArray, width: Int, height: Int, bytesPerPixel: Int): ByteArray? {
        val stride = width * bytesPerPixel
        val result = ByteArray(stride * height)
        for (row in 0 until height) {
            val filterType = filtered[row * (stride + 1)].toInt() and 0xFF
            val sourceOffset = row * (stride + 1) + 1
            val targetOffset = row * stride
            val previousOffset = targetOffset - stride
            for (column in 0 until stride) {
                val raw = filtered[sourceOffset + column].toInt() and 0xFF
                val left = if (column >= bytesPerPixel) result[targetOffset + column - bytesPerPixel].toInt() and 0xFF else 0
                val up = if (row > 0) result[previousOffset + column].toInt() and 0xFF else 0
                val upLeft = if (row > 0 && column >= bytesPerPixel) {
                    result[previousOffset + column - bytesPerPixel].toInt() and 0xFF
                } else {
                    0
                }
                val value = when (filterType) {
                    0 -> raw
                    1 -> raw + left
                    2 -> raw + up
                    3 -> raw + ((left + up) ushr 1)
                    4 -> raw + paethPredictor(left, up, upLeft)
                    else -> return null
                }
                result[targetOffset + column] = value.toByte()
            }
        }
        return result
    }

    private fun paethPredictor(left: Int, up: Int, upLeft: Int): Int {
        val estimate = left + up - upLeft
        val distanceLeft = kotlin.math.abs(estimate - left)
        val distanceUp = kotlin.math.abs(estimate - up)
        val distanceUpLeft = kotlin.math.abs(estimate - upLeft)
        return when {
            distanceLeft <= distanceUp && distanceLeft <= distanceUpLeft -> left
            distanceUp <= distanceUpLeft -> up
            else -> upLeft
        }
    }
}
