package com.saadshaikh.epubreader.model

import java.time.LocalDate

/**
 * Immutable value object containing book metadata extracted from EPUB.
 */
data class Metadata(
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val language: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val publicationDate: LocalDate? = null,
    val subjects: List<String> = emptyList()
)
