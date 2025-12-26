package com.saadshaikh.epubreader.service

import com.saadshaikh.epubreader.model.Chapter
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist
import org.slf4j.LoggerFactory

/**
 * Extracts plain text from HTML chapter content.
 * Uses JSoup for HTML parsing and handles malformed HTML gracefully.
 */
class TextExtractor {

    /**
     * Extracts plain text from a Chapter object.
     *
     * @param chapter the chapter containing HTML content
     * @return plain text
     * @throws IllegalArgumentException if chapter content is not loaded
     */
    fun extractTextFromChapter(chapter: Chapter): String {
        require(chapter.isContentLoaded()) { "Chapter content not loaded: ${chapter.title}" }

        val htmlContent = chapter.content!!.decodeToString()
        return extractTextFromHTML(htmlContent)
    }

    /**
     * Extracts plain text from HTML content string.
     *
     * @param htmlContent the HTML content
     * @return plain text
     */
    private fun extractTextFromHTML(htmlContent: String): String {
        if (htmlContent.isBlank()) return ""

        return runCatching {
            Jsoup.parse(htmlContent).body()?.let { body ->
                buildString { extractTextFromElement(body, this) }
                    .cleanWhitespace()
            } ?: ""
        }.getOrElse { e ->
            logger.warn("Failed to extract text from HTML: ${e.message}")
            Jsoup.clean(htmlContent, Safelist.none())
        }
    }

    /**
     * Recursively extracts text
     */
    private fun extractTextFromElement(element: Element, text: StringBuilder) {
        element.childNodes().forEach { node ->
            when (node) {
                is TextNode -> {
                    val nodeText = node.text()
                    if (nodeText.isNotBlank()) {
                        text.append(nodeText)
                    }
                }

                is Element -> {
                    val tagName = node.tagName().lowercase()

                    when {
                        tagName == "br" -> text.append("\n")

                        else -> extractTextFromElement(node, text)
                    }
                }
            }
        }
    }

    /**
     * Cleans up excessive whitespace
     */
    private fun String.cleanWhitespace(): String =
        replace(Regex(" +"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .replace(Regex(" *\n *"), "\n")
            .trim()

    companion object {
        private val logger = LoggerFactory.getLogger(TextExtractor::class.java)
    }
}
