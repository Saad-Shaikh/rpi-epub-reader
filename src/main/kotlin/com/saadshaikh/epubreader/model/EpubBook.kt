package com.saadshaikh.epubreader.model

/**
 * Container object representing a parsed EPUB book.
 * Contains metadata, chapters, and cover image.
 */
data class EpubBook(
    val metadata: Metadata? = null,
    val chapters: List<Chapter> = emptyList(),
    val coverImage: Image? = null,
)
