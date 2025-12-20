package com.saadshaikh.epubreader.model

/**
 * Represents a single chapter or spine item in an EPUB.
 * Content is stored as byte array for memory efficiency and lazy loading support.
 */
data class Chapter(
    val id: String,
    val title: String?,
    val href: String,
    val sequenceNumber: Int,
    var content: ByteArray? = null // Mutable for lazy loading
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Chapter

        if (sequenceNumber != other.sequenceNumber) return false
        if (id != other.id) return false
        if (title != other.title) return false
        if (href != other.href) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sequenceNumber
        result = 31 * result + id.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + href.hashCode()
        result = 31 * result + (content?.contentHashCode() ?: 0)
        return result
    }
}
