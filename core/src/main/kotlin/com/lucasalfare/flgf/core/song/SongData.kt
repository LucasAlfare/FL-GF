package com.lucasalfare.flgf.core.song

import com.lucasalfare.flgf.core.game.Note

/**
 *
 * Represents all data required to play a song.
 *
 * @property notes List of parsed notes sorted by time.
 *
 * @property musicFileName Optional reference to the audio file associated with the chart.
 *
 * @property lengthMs Optional total song length in milliseconds.
 *
 * Notes:
 *
 * - Some chart formats may omit metadata, so nullable fields are expected.
 *
 * - The engine should not rely strictly on [lengthMs]; playback systems may define their own
 * timing.
 */
data class SongData(val notes: List<Note>, val musicFileName: String?, val lengthMs: Long?)
