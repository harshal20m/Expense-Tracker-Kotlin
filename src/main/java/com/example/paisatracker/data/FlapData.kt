package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single quick note stored inside the flap.
 * Serialized as "id|text|timestamp" — separator is unlikely to appear in note text.
 */
data class FlapNote(
    val id: String,          // UUID string
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Single-row Room table (id always = 1) storing all flap state.
 *
 * Notes are stored as a JSON-like serialized string:
 *   each note = "id\u001Ftext\u001Ftimestamp"   (unit separator char = \u001F)
 *   notes are joined with \u001E  (record separator char)
 *
 * Calculator history = newline-delimited list of entries.
 */
@Entity(tableName = "flap_data")
data class FlapData(
    @PrimaryKey
    val id: Int = 1,
    val notesSerialized: String = "",           // replaces old notesText
    val calcHistorySerialized: String = "",
    val calcDisplay: String = "0",
    val calcExpression: String = "",
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    fun calcHistoryList(): List<String> =
        if (calcHistorySerialized.isBlank()) emptyList()
        else calcHistorySerialized.split("\n").filter { it.isNotBlank() }

    fun notesList(): List<FlapNote> =
        if (notesSerialized.isBlank()) emptyList()
        else notesSerialized.split("\u001E").filter { it.isNotBlank() }.mapNotNull { entry ->
            val parts = entry.split("\u001F")
            if (parts.size >= 3) FlapNote(
                id = parts[0],
                text = parts[1],
                createdAt = parts[2].toLongOrNull() ?: System.currentTimeMillis()
            ) else null
        }
}

fun List<String>.serializeHistory(): String = joinToString("\n")

fun List<FlapNote>.serializeNotes(): String =
    joinToString("\u001E") { note ->
        "${note.id}\u001F${note.text}\u001F${note.createdAt}"
    }