package com.saadshaikh.epubreader

import com.saadshaikh.epubreader.service.EpubParser
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        return
    }

    val epubPath = args[0]
    val epubFile = File(epubPath)

    if (!epubFile.exists()) {
        System.err.println("ERROR: epub file does not exist: ${epubFile.absolutePath}")
        exitProcess(1)
    }

    val epubParser = EpubParser()
    val book = epubParser.parse(epubFile)

    println("Epub parsed. Title: ${book.metadata?.title}")
}
