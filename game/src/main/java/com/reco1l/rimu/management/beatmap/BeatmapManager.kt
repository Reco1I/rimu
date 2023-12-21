package com.reco1l.rimu.management.beatmap

import android.util.Log
import com.reco1l.toolkt.kotlin.nextOf
import com.reco1l.toolkt.kotlin.orCatch
import com.reco1l.toolkt.kotlin.previousOf
import com.reco1l.toolkt.IObservable
import com.reco1l.toolkt.forEachObserver
import com.rian.osu.beatmap.parser.BeatmapDecoder
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.data.Beatmap
import com.reco1l.rimu.data.BeatmapSet
import com.reco1l.rimu.ui.layouts.Notification
import com.reco1l.toolkt.kotlin.BoundConflict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class BeatmapManager(override val ctx: MainContext) :
    IWithContext,
    IObservable<IBeatmapObserver>
{

    override val observers = mutableListOf<IBeatmapObserver>()

    /**
     * The list of [BeatmapSet].
     */
    lateinit var sets: List<BeatmapSet>

    /**
     * List of beatmaps distinct by audio filename.
     */
    lateinit var songs: List<Beatmap>


    /**
     * The beatmap importer.
     */
    val importer = BeatmapImporter(ctx)

    /**
     * The current working beatmap.
     */
    var current: WorkingBeatmap? = null
        private set


    private val musicScope = CoroutineScope(Dispatchers.IO)


    init
    {
        // Initializing library list with a Flow
        ctx.onPostInitialization {

            GlobalScope.launch {

                // The flow will update the list everytime the table is changed.
                ctx.database.beatmapTable.getParentSetFlow().collect(::onTableChange)
            }
        }
    }


    private fun onTableChange(value: List<BeatmapSet>)
    {
        musicScope.launch {

            val wasEmpty = if (::sets.isInitialized) sets.isEmpty() else true

            sets = value

            // Distinct beatmaps by its audio filename, this is exclusively used for the music
            // player to handle beatmaps sets with multiple songs
            songs = sets.flatMap { set -> set.beatmaps.distinctBy { it.audio } }.shuffled()

            if (wasEmpty && sets.isNotEmpty())
                setCurrent(songs.random(), true)
        }
    }

    private fun onCreateWorkingBeatmap(source: Beatmap) = { WorkingBeatmap(ctx, source) }.orCatch {

        Notification(
            header = "Error",
            message = """
                Unable to load beatmap "${source.title} by ${source.artist} - ${source.version} mapped by ${source.creator}"
                Cause: ${it.javaClass} - ${it.message}
            """.trimIndent(),
            icon = ""
        ).show(ctx)

        Log.e(javaClass.simpleName, "Error while loading beatmap.", it)
        null
    }


    /**
     * Changes to a new WorkingBeatmap defined by the [source][source] parameter.
     *
     * @param forceReload If `true` the beatmap will be changed no matter if it's the same.
     */
    fun setCurrent(source: Beatmap, forceReload: Boolean = false): Job = musicScope.launch {

        val last = current

        if (last != null)
        {
            if (source == last.source && !forceReload)
                return@launch

            last.onRelease()
        }

        current = onCreateWorkingBeatmap(source)

        // Notifying all registered observers.
        forEachObserver { it.onMusicChange(current) }

        // Starting the audio stream.
        current?.play()
        current?.stream?.onStreamEnd = {

            musicScope.launch {

                forEachObserver { it.onMusicEnd() }
            }
        }
    }

    /**
     * Shifts to the next song in the playlist.
     */
    fun next() = songs.nextOf(current?.source ?: songs.random(), BoundConflict.START_END)?.let { setCurrent(it) }

    /**
     * Shifts to the previous song in the playlist.
     */
    fun previous() = songs.previousOf(current?.source ?: songs.random(), BoundConflict.START_END)?.let { setCurrent(it) }
}