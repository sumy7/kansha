package com.sumygg.kansha.handler

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
    val width: Int,
    val height: Int,
    val blob: ByteArray
)

fun parseJPEG(buf: ByteArray): ImageData {
    // skip magic bytes
    var offset = 4
    val len = buf.size
    while (offset < len) {
        val i = buf[offset].toInt() and 0xFF
        if (i > len) {
            throw IllegalArgumentException("Invalid JPEG data")
        }
        val next = buf[i + 1 + offset]
        if (next == 0xc0.toByte() || next == 0xc1.toByte() || next == 0xc2.toByte()) {
            return ImageData(
                width = buf[i + 7 + offset].toInt() and 0xFF,
                height = buf[i + 5 + offset].toInt() and 0xFF,
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
        width = width,
        height = height,
        blob = buf
    )
}

fun parsePNG(buf: ByteArray): ImageData {
    return ImageData(
        width = buf[18].toInt() and 0xFF,
        height = buf[22].toInt() and 0xFF,
        blob = buf
    )
}

//fun parseSVG(buf: ByteArray): ImageData {
//    val data = buf.decodeToString()
//    val svgTag =  data.
//    val svgTag = data.matches("<svg[^>]*>".toRegex())
//
//}

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


/**
 * 解析 image 标签的 src 属性
 */
fun resolveImageSrc(src: String?): ImageData {
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

        } else if (encodingType == "base64") {

        }
    }

    // todo parse image data
    return parseGIF(byteArrayOf())
}