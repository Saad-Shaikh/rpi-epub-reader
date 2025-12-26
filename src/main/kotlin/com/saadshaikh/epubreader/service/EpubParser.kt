package com.saadshaikh.epubreader.service

import com.saadshaikh.epubreader.model.Chapter
import com.saadshaikh.epubreader.model.EpubBook
import com.saadshaikh.epubreader.model.Image
import com.saadshaikh.epubreader.model.Metadata
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Facade for parsing EPUB files using Epublib.
 * Implements lazy loading to minimize memory footprint.
 */
class EpubParser {

    /**
     * Parses an EPUB file and returns an EpubBook object.
     *
     * @param epubFile the EPUB file to parse
     * @return parsed EpubBook object
     * @throws EpubParsingException if parsing fails
     */
    fun parse(epubFile: File): EpubBook {
        require(epubFile.exists()) { "EPUB file does not exist: $epubFile" }

        logger.info("Parsing EPUB: ${epubFile.absolutePath}")

        return epubFile.inputStream().use { parse(it) }
    }

    /**
     * Parses an EPUB from an input stream.
     *
     * @param inputStream the input stream containing EPUB data
     * @return parsed EpubBook object
     * @throws EpubParsingException if parsing fails
     */
    private fun parse(inputStream: java.io.InputStream): EpubBook = runCatching {
        val epublibBook = EpubReader().readEpub(inputStream)

        val metadata = extractMetadata(epublibBook)
        val chapters = extractChapters(epublibBook)
        val coverImage = extractCoverImage(epublibBook)

        logger.info("Successfully parsed EPUB: ${metadata.title} (${chapters.size} chapters)")

        EpubBook(
            metadata = metadata,
            chapters = chapters,
            coverImage = coverImage
        )
    }.getOrElse { throw EpubParsingException("Failed to parse EPUB", it) }

    /**
     * Extracts metadata from the EPUB's OPF package.
     */
    private fun extractMetadata(book: Book): Metadata = book.metadata.let { metadata ->
        Metadata(
            title = metadata.firstTitle?.takeIf(String::isNotBlank) ?: "Unknown Title",
            authors = metadata.authors.orEmpty()
                .map { "${it.firstname} ${it.lastname}".trim() }
                .filter(String::isNotBlank),
            publishers = metadata.publishers.orEmpty().filter(String::isNotBlank),
            language = metadata.language?.takeIf(String::isNotBlank),
            isbn = metadata.identifiers.orEmpty()
                .find { it.scheme.equals("ISBN", ignoreCase = true) }
                ?.value,
            description = metadata.descriptions.orEmpty()
                .firstOrNull()
                ?.takeIf(String::isNotBlank),
            publicationDate = metadata.dates.orEmpty()
                .firstOrNull()
                ?.value
                ?.let(::parseDate),
            subjects = metadata.subjects.orEmpty().filter(String::isNotBlank)
        )
    }

    /**
     * Extracts chapters from the EPUB spine.
     * Content is loaded immediately (lazy loading can be implemented in future phases).
     */
    private fun extractChapters(book: Book): List<Chapter> =
        book.spine.spineReferences.mapIndexed { index, spineRef ->
            spineRef.resource.let { resource ->
                Chapter(
                    id = resource.id,
                    title = extractChapterTitle(resource, index + 1),
                    href = resource.href,
                    sequenceNumber = index,
                    content = runCatching { resource.data }
                        .onFailure { logger.warn("Failed to load content for chapter $index: ${it.message}") }
                        .getOrNull()
                )
            }
        }

    /**
     * Attempts to extract a chapter title from the resource.
     * Falls back to generic title if not available.
     */
    private fun extractChapterTitle(resource: Resource, chapterNumber: Int): String =
        resource.title?.takeIf(String::isNotBlank)
            ?: resource.href
                ?.substringAfterLast('/')
                ?.substringBeforeLast('.')
                ?.takeIf(String::isNotBlank)
            ?: "Chapter $chapterNumber"

    /**
     * Extracts the cover image from the EPUB.
     */
    private fun extractCoverImage(book: Book): Image? = runCatching {
        book.coverImage?.let { coverResource ->
            Image(
                data = coverResource.data,
                mimeType = coverResource.mediaType?.name ?: "image/jpeg"
            )
        }
    }.onFailure { logger.warn("Failed to extract cover image: ${it.message}") }
        .getOrNull()

    /**
     * Parses a date string using multiple format patterns.
     */
    private fun parseDate(dateStr: String): LocalDate? =
        dateStr.takeIf(String::isNotBlank)?.let {
            DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
                runCatching { LocalDate.parse(dateStr, formatter) }.getOrNull()
            }.also { result ->
                if (result == null) logger.warn("Failed to parse date: $dateStr")
            }
        }

    /**
     * Exception thrown when EPUB parsing fails.
     */
    class EpubParsingException(message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        private val logger = LoggerFactory.getLogger(EpubParser::class.java)
        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy")
        )
    }
}
