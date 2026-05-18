package com.lucasalfare.flgf.core.song

import com.lucasalfare.flgf.core.game.Note
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 *
 * Utility object responsible for parsing song chart data from XML.
 *
 * Expected XML structure (loosely defined):
 *
 * - Root element containing:
 *
 * - Multiple <Note> elements
 *
 * - Optional <Properties> section
 *
 * Design goals:
 *
 * - Be tolerant to malformed or incomplete data
 *
 * - Skip invalid notes instead of failing the entire parsing process
 *
 * - Convert all time units to milliseconds for engine compatibility
 */
object SongXmlParser {

  /**
   *
   * Parses an XML input stream into a [SongData] object.
   *
   * Processing steps:
   *
   * 1. Build DOM document
   *
   * 2. Normalize XML structure
   *
   * 3. Extract notes
   *
   * 4. Extract metadata properties
   *
   * 5. Sort notes by time (guarantees engine correctness)
   *
   * @param input Input stream containing XML chart data.
   *
   * @return Parsed [SongData] ready for use in the game engine.
   *
   * Important:
   *
   * - This method does not close the input stream.
   *
   * - Any XML parsing exception will propagate to the caller.
   */
  fun parse(input: InputStream): SongData {

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)

    val root = doc.documentElement

    root.normalize()

    val notes = parseNotes(root)

    val (musicFileName, lengthMs) = parseProperties(root)

    return SongData(
      notes = notes.sortedBy { it.hitTime },
      musicFileName = musicFileName,
      lengthMs = lengthMs
    )
  }

  // ==================== NOTES PARSING ====================

  /**
   *
   * Extracts all <Note> elements from the XML root.
   *
   * Expected attributes per note:
   *
   * - time (seconds, required)
   *
   * - duration (seconds, optional, defaults to 0)
   *
   * - track (lane index, required)
   *
   * - special (optional flag, "1" = true)
   *
   * Error handling strategy:
   *
   * - Invalid or missing required fields cause the note to be skipped
   *
   * - Optional fields fallback to safe defaults
   *
   * Time conversion:
   *
   * - Input is in seconds (floating point)
   *
   * - Internally converted to milliseconds (Long)
   *
   * @param root Root XML element.
   *
   * @return List of parsed [Note] objects (unsorted).
   */
  private fun parseNotes(root: Element): List<Note> {

    val noteList = root.getElementsByTagName("Note")

    val result = mutableListOf<Note>()

    for (i in 0 until noteList.length) {

      val node = noteList.item(i) as? Element ?: continue

      /**
       *
       * Required: time (seconds)
       *
       * If invalid, skip the note entirely.
       */
      val timeSec = node.getAttribute("time").toDoubleOrNull() ?: continue

      /**
       *
       * Optional: duration (seconds)
       *
       * Defaults to 0 (tap note).
       */
      val durationSec = node.getAttribute("duration").toDoubleOrNull() ?: 0.0

      /**
       *
       * Required: track (lane index)
       *
       * If invalid, skip the note.
       */
      val lane = node.getAttribute("track").toIntOrNull() ?: continue

      /**
       *
       * Optional: special flag
       *
       * Convention: "1" means true, anything else is false.
       */
      val isSpecial = node.getAttribute("special") == "1"

      result.add(
        Note(
          hitTime = (timeSec * 1000).toLong(),
          lane = lane,
          duration = (durationSec * 1000).toLong(),
          isSpecial = isSpecial
        )
      )
    }

    return result
  }

  // ==================== METADATA PARSING ====================

  /**
   *
   * Extracts optional metadata from the <Properties> section.
   *
   * Expected structure:
   *
   * <Properties>
   *
   * <MusicFileName>...</MusicFileName>
   *
   * <Length>...</Length> <!-- seconds -->
   *
   * </Properties>
   *
   * Behavior:
   *
   * - If <Properties> is missing, returns null values
   *
   * - Missing individual fields are also treated as null
   *
   * @param root Root XML element.
   *
   * @return Pair of (musicFileName, lengthMs)
   */
  private fun parseProperties(root: Element): Pair<String?, Long?> {

    val propsList = root.getElementsByTagName("Properties")

    if (propsList.length == 0) return null to null

    val props = propsList.item(0) as? Element ?: return null to null

    /**
     *
     * Optional music file reference.
     */
    val musicFileName = props.getElementsByTagName("MusicFileName").item(0)?.textContent

    /**
     *
     * Optional song length in seconds.
     *
     * Converted to milliseconds if valid.
     */
    val lengthSec = props.getElementsByTagName("Length").item(0)?.textContent?.toDoubleOrNull()

    val lengthMs = lengthSec?.let { (it * 1000).toLong() }

    return musicFileName to lengthMs
  }
}
