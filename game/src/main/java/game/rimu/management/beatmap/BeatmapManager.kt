package game.rimu.management.beatmap

import android.util.Log
import com.reco1l.framework.kotlin.nextOf
import com.reco1l.framework.kotlin.orCatch
import com.reco1l.framework.kotlin.previousOf
import com.reco1l.framework.IObservable
import com.reco1l.framework.forEachObserver
import com.rian.osu.beatmap.parser.BeatmapDecoder
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.data.Beatmap
import game.rimu.data.BeatmapSet
import game.rimu.ui.layouts.Notification
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
     * The beatmap decoder used along for beatmap changes.
     */
    val decoder = BeatmapDecoder()

    /**
     * The current working beatmap.
     */
    var current: WorkingBeatmap? = null
        private set


    private var currentSource: Beatmap? = null

    private val musicScope = CoroutineScope(Dispatchers.IO)


    init
    {
        // Initializing library list with a Flow
        ctx.initializationTree!!.add {

            GlobalScope.launch {

                // The flow will update the list everytime the table is changed.
                ctx.database.beatmapTable.getParentSetFlow().collect(::onTableChange)
            }
        }
    }


    private fun onTableChange(value: List<BeatmapSet>)
    {
        musicScope.launch {

            val wasEmpty = !::sets.isInitialized || ::sets.isInitialized && sets.isEmpty()

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
                """.trimMargin(),
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

        if (source == currentSource && !forceReload)
            return@launch

        current?.onRelease()
        current = onCreateWorkingBeatmap(source)

        // Storing new source separately in case the working beatmap failed to create.
        currentSource = source

        // Notifying all registered observers.
        forEachObserver { it.onMusicChange(current) }

        // Starting the audio stream.
        current?.play()
        current?.stream?.onStreamEnd = { next() }
    }

    /**
     * Shifts to the next song in the playlist.
     */
    fun next() = songs.nextOf(currentSource, false)?.let { setCurrent(it) }

    /**
     * Shifts to the previous song in the playlist.
     */
    fun previous() = songs.previousOf(currentSource, false)?.let { setCurrent(it) }
}