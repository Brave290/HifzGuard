package com.example

import org.junit.Test
import java.io.File
import org.json.JSONArray

class QuranDataTest {
    @Test
    fun checkFileEnd() {
        val file = File("src/main/assets/translation_en.json")
        val content = file.readText()
        println("File size: ${content.length}")
        println("End of file: " + content.substring((content.length - 100).coerceAtLeast(0)))
        
        try {
            val idx = 1504450
            if (content.length > idx) {
                println("Around 1504481: " + content.substring(idx.coerceAtLeast(0), (idx + 100).coerceAtMost(content.length)))
            }
        } catch (e: Exception) {
            println(e)
        }
    }
}
