package game.rimu.management.beatmap

import com.reco1l.framework.lang.nextOf
import com.reco1l.framework.lang.orCatch
import com.reco1l.framework.lang.previousOf
import com.rian.osu.beatmap.parser.BeatmapDecoder
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.data.Beatmap
import game.rimu.data.BeatmapSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class BeatmapManager(override val ctx: RimuContext) :
    FlowCollector<List<BeatmapSet>>,
    IWithContext
{

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


    // Using a different coroutine context.
    private val changeScope = CoroutineScope(Dispatchers.IO)


    init
    {
        // Initializing library list with a Flow
        ctx.initializationTree!!.add {

            GlobalScope.launch {

                // The flow will update the list everytime the table is changed.
                ctx.database.getBeatmapSetsFlow().collect(this@BeatmapManager)
            }
        }
    }

    // onBeatmapTableChange
    override suspend fun emit(value: List<BeatmapSet>)
    {
        changeScope.launch {

            sets = value

            // Distinct beatmaps by its audio filename, this is exclusively used for the music
            // player to handle beatmaps sets with multiple songs
            songs = sets.flatMap { it.beatmaps }.distinctBy { it.audio }
        }
    }


    private fun onCreateWorkingBeatmap(base: Beatmap): WorkingBeatmap?
    {
        return { WorkingBeatmap(ctx, base) }.orCatch {

            // Inform beatmap load fail.

            null
        }
    }


    fun play(base: Beatmap) = changeScope.launch {

        // Releasing previous working beatmap.
        current?.onRelease()

        current = onCreateWorkingBeatmap(base) ?: return@launch
        current!!.play()
    }


    fun next() = changeScope.launch {

        current?.onRelease()

        val next = songs.nextOf(current) as? Beatmap ?: return@launch

        current = onCreateWorkingBeatmap(next) ?: return@launch
        current!!.play()
    }

    fun previous() = changeScope.launch {

        current?.onRelease()

        val previous = songs.previousOf(current) as? Beatmap ?: return@launch

        current = onCreateWorkingBeatmap(previous) ?: return@launch
        current!!.play()
    }
}