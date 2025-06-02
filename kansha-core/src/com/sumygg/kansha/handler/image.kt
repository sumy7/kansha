package com.sumygg.kansha.handler

import com.sumygg.kansha.getUInt16
import com.sumygg.kansha.getUInt32
import com.sumygg.kansha.getUInt8
import com.sumygg.kansha.parseViewBox
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.core.*

enum class ImageType(val mimeType: String) {
    AVIF("image/avif"),
    WEBP("image/webp"),
    APNG("image/apng"),
    PNG("image/png"),
    JPEG("image/jpeg"),
    GIF("image/gif"),
    SVG("image/svg+xml");

    companion object {
        fun fromMimeType(mimeType: String): ImageType? {
            return ImageType.entries.find { it.mimeType == mimeType }
        }
    }
}

data class ImageData(
    val type: ImageType,
    val width: Int,
    val height: Int,
    val blob: ByteArray
) {
    fun toDataUri(): String {
        val base64Data = blob.encodeBase64()
        return "data:${type.mimeType};base64,$base64Data"
    }
}

fun parseJPEG(buf: ByteArray): ImageData {
    // skip magic bytes
    var offset = 4
    val len = buf.size
    while (offset < len) {
        val i = buf.getUInt16(offset).toInt()
        if (i > len) {
            throw IllegalArgumentException("Invalid JPEG data")
        }
        val next = buf.getUInt8(i + 1 + offset).toInt()
        if (next == 0xc0 || next == 0xc1 || next == 0xc2) {
            return ImageData(
                type = ImageType.JPEG,
                width = buf.getUInt16(i + 7 + offset, false).toInt(),
                height = buf.getUInt16(i + 5 + offset, false).toInt(),
                blob = buf
            )
        }

        // TODO: Support orientations from EXIF.
        offset += i + 2
    }

    throw IllegalArgumentException("Invalid JPEG data")
}

fun parseGIF(buf: ByteArray): ImageData {
    val width = buf[6].toInt() or (buf[7].toInt() shl 8)
    val height = buf[8].toInt() or (buf[9].toInt() shl 8)

    return ImageData(
        type = ImageType.GIF,
        width = width,
        height = height,
        blob = buf
    )
}

fun parsePNG(buf: ByteArray): ImageData {
    return ImageData(
        type = if (detectAPNG(buf)) ImageType.APNG else ImageType.PNG,
        width = buf.getUInt16(18, false).toInt(),
        height = buf.getUInt16(22, false).toInt(),
        blob = buf
    )
}

fun parseSVG(buf: ByteArray): ImageData {
    val data = buf.decodeToString()
    // Parse the SVG image size
    val svgTagMatch = Regex("<svg[^>]*>").find(data)
    val svgTag =
        svgTagMatch?.value ?: throw IllegalArgumentException("Failed to parse SVG from ${data}: missing <svg> tag")

    val viewBoxStr = Regex("""viewBox=['"](.+?)['"]""").find(svgTag)
    val viewBox = viewBoxStr?.groups?.get(1)?.value?.let { parseViewBox(it) }

    val widthMatch = Regex("""width=['"](\d*\.\d+|\d+)['"]""").find(svgTag)
    val width = widthMatch?.groups?.get(1)?.value

    val heightMatch = Regex("""height=['"](\d*\.\d+|\d+)['"]""").find(svgTag)
    val height = heightMatch?.groups?.get(1)?.value

    if (viewBox == null && (width == null || height == null)) {
        throw IllegalArgumentException("Failed to parse SVG from ${data}: missing viewBox or width/height attributes")
    }

    val svgWidth = if (viewBox != null) viewBox[2] else width?.toInt() ?: 0
    val svgHeight = if (viewBox != null) viewBox[3] else height?.toInt() ?: 0
    val ratio = svgWidth / svgHeight

    val (finalWidth, finalHeight) = if (width != null && height != null) {
        width.toInt() to height.toInt()
    } else if (width != null) {
        width.toInt() to (width.toInt() / ratio).toInt()
    } else if (height != null) {
        (height.toInt() * ratio).toInt() to height.toInt()
    } else {
        svgWidth to svgHeight
    }

    return ImageData(
        type = ImageType.SVG,
        width = finalWidth,
        height = finalHeight,
        blob = buf
    )
}

fun detectJPEG(buf: ByteArray): Boolean {
    return buf.size >= 3 && buf[0] == 0xFF.toByte() && buf[1] == 0xD8.toByte() && buf[2] == 0xFF.toByte()
}

fun detectPNG(buf: ByteArray): Boolean {
    return buf.size >= 8 && buf[0] == 0x89.toByte() && buf[1] == 0x50.toByte() &&
            buf[2] == 0x4E.toByte() && buf[3] == 0x47.toByte() &&
            buf[4] == 0x0D.toByte() && buf[5] == 0x0A.toByte() &&
            buf[6] == 0x1A.toByte() && buf[7] == 0x0A.toByte()
}

fun detectAPNG(buf: ByteArray): Boolean {
    var type = ""
    var length = 0
    var off = 8
    var isAPNG = false
    while (!isAPNG && type != "IEND" && off < buf.size) {
        length = buf.getUInt32(off).toInt()
        val subBuf = buf.copyOfRange(off + 4, off + 8)
        type = subBuf.decodeToString()
        if (type == "acTL") {
            isAPNG = true
        }
        off += length + 12 // length + type + CRC
    }
    return isAPNG
}

fun detectGIF(buf: ByteArray): Boolean {
    return buf.size >= 4 && buf[0] === 0x47.toByte() && buf[1] == 0x49.toByte() &&
            buf[2] == 0x46.toByte() && (buf[3] == 0x38.toByte() || buf[3] == 0x39.toByte())
}

fun detectWEBP(buf: ByteArray): Boolean {
    return buf.size >= 12 && buf[0] == 0x52.toByte() && buf[1] == 0x49.toByte() &&
            buf[2] == 0x46.toByte() && buf[3] == 0x46.toByte() &&
            buf[8] == 0x57.toByte() && buf[9] == 0x45.toByte() &&
            buf[10] == 0x42.toByte() && buf[11] == 0x50.toByte()
}

fun detectSVG(buf: ByteArray): Boolean {
    // SVG files usually start with <svg or <?xml version="1.0" encoding="UTF-8"?>
    val data = buf.decodeToString()
    return data.startsWith("<svg") || data.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
}

fun detectAVIF(buf: ByteArray): Boolean {
    // AVIF files start with the magic number "AVIF" or "AV1F"
    return buf.size >= 12 &&
            buf[4] == 0x66.toByte() && buf[5] == 0x74.toByte() &&
            buf[6] == 0x79.toByte() && buf[7] == 0x70.toByte() &&
            (buf[8] == 0x61.toByte() || buf[8] == 0x41.toByte()) &&
            (buf[9] == 0x76.toByte() || buf[9] == 0x56.toByte()) &&
            (buf[10] == 0x69.toByte() || buf[10] == 0x49.toByte()) &&
            (buf[11] == 0x66.toByte() || buf[11] == 0x46.toByte())
}

/**
 * Inspects the first few bytes of a buffer to determine if
 * it matches the "magic number" of known file signatures.
 * https://en.wikipedia.org/wiki/List_of_file_signatures
 */
fun detectContentType(buffer: ByteArray): ImageType? {
    if (detectJPEG(buffer)) {
        return ImageType.JPEG
    }
    if (detectPNG(buffer)) {
        if (detectAPNG(buffer)) {
            return ImageType.APNG
        }
        return ImageType.PNG
    }
    if (detectGIF(buffer)) {
        return ImageType.GIF
    }
    if (detectSVG(buffer)) {
        return ImageType.SVG
    }
    if (detectWEBP(buffer)) {
        return ImageType.WEBP
    }
    if (detectAVIF(buffer)) {
        return ImageType.AVIF
    }
    return null
}


val ALLOWED_IMAGE_TYPES = arrayOf(ImageType.PNG, ImageType.APNG, ImageType.JPEG, ImageType.GIF, ImageType.SVG)


const val DATA_URI_REGEX = "data:(?<imageType>[a-z/+]+)(;[^;=]+=[^;=]+)*?(;(?<encodingType>[^;,]+))?,(?<dataString>.*)"

data class DecodedDataUri(
    val imageType: String,
    val encodingType: String,
    val dataString: String
)

private fun parseDataUri(data: String): DecodedDataUri {
    val regex = DATA_URI_REGEX.toRegex()
    val matchResult = regex.find(data)
    if (matchResult != null) {
        val imageType = matchResult.groups["imageType"]?.value ?: ""
        val encodingType = matchResult.groups["encodingType"]?.value ?: ""
        val dataString = matchResult.groups["dataString"]?.value ?: ""

        return DecodedDataUri(imageType, encodingType, dataString)
    } else {
        throw IllegalArgumentException("Invalid data URI format.")
    }
}

fun parseByteArray(data: ByteArray): ImageData {
    val imageType = detectContentType(data)
    if (!ALLOWED_IMAGE_TYPES.contains(imageType)) {
        throw IllegalArgumentException("Unsupported image type: ${imageType ?: "unknown"}")
    }
    return when (imageType) {
        ImageType.JPEG -> parseJPEG(data)
        ImageType.PNG -> parsePNG(data)
        ImageType.APNG -> parsePNG(data) // APNG is a PNG with animation, but we treat it as PNG for now
        ImageType.GIF -> parseGIF(data)
        ImageType.SVG -> parseSVG(data)
        else -> throw IllegalArgumentException("Unsupported image type: $imageType")
    }
}

fun resolveImageSrc(src: ByteArray?): ImageData {
    src ?: throw IllegalArgumentException("Image source is not provided.")
    return parseByteArray(src)
}


/**
 * 解析 image 标签的 src 属性
 */
suspend fun resolveImageSrc(src: String?): ImageData {
    if (src.isNullOrBlank()) {
        throw IllegalArgumentException("Image source is not provided.")
    }

    // src 属性只支持 http 链接和 base64 编码的图片
    if (!src.startsWith("http") && !src.startsWith("data:")) {
        throw IllegalArgumentException("Image source must be an absolute URL: $src")
    }

    if (src.startsWith("data:")) {
        val decodedDataUri = parseDataUri(src)
        val imageType = decodedDataUri.imageType
        val encodingType = decodedDataUri.encodingType
        val dataString = decodedDataUri.dataString

        if (imageType == ImageType.SVG.mimeType) {
            val data = if (encodingType === "base64") {
                dataString.decodeBase64Bytes()
            } else {
                dataString.replace(" ", "%20").decodeURLQueryComponent().toByteArray()
            }
            return parseSVG(data)
        } else if (encodingType == "base64") {
            val data = dataString.decodeBase64Bytes()
            return when (imageType) {
                ImageType.PNG.mimeType -> parsePNG(data)
                ImageType.APNG.mimeType -> parsePNG(data)
                ImageType.GIF.mimeType -> parseGIF(data)
                ImageType.JPEG.mimeType -> parseJPEG(data)
                else -> throw IllegalArgumentException("Unsupported image type: $imageType")
            }
        } else {
            throw IllegalArgumentException("Image data URI resolved no size: $src")
        }
    }

    val url = src.trim()
    val response = HttpClient().get(url)
    val byteArray = response.body<ByteArray>()
    return parseByteArray(byteArray)
}
