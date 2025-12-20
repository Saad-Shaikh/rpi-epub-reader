package com.saadshaikh.epubreader.model

data class Image(
    val data: ByteArray? = null,
    val mimeType: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (!data.contentEquals(other.data)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data?.contentHashCode() ?: 0
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }
}
