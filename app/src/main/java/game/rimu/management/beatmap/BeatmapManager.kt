package game.rimu.management.beatmap

import com.reco1l.framework.android.logE
import com.reco1l.framework.lang.klass
import com.reco1l.framework.lang.nextOf
import com.reco1l.framework.lang.orCatch
import com.reco1l.framework.lang.previousOf
import com.reco1l.framework.lang.with
import com.reco1l.framework.management.IObservable
import com.reco1l.framework.management.forEachObserver
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


    // Using a different coroutine context.
    private val musicScope = CoroutineScope(Dispatchers.IO)


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

    // Called when the beatmap table has been changed.
    override suspend fun emit(value: List<BeatmapSet>)
    {
        musicScope.launch {

            val wasEmpty = !::sets.isInitialized || ::sets.isInitialized && sets.isEmpty()

            sets = value

            // Distinct beatmaps by its audio filename, this is exclusively used for the music
            // player to handle beatmaps sets with multiple songs
            songs = sets.flatMap { it.beatmaps }.distinctBy { it.audio }

            if (wasEmpty && sets.isNotEmpty())
                play(songs.random())
        }
    }


    private fun onCreateWorkingBeatmap(base: Beatmap): WorkingBeatmap?
    {
       return { WorkingBeatmap(ctx, base) }.orCatch {
           klass logE "Failed to load beatmap: ${base.hash} ${base.title}" with it
           null
       }
    }


    fun play(base: Beatmap? = current?.source) = musicScope.launch {

        val isDifferent = base != current?.source

        if (isDifferent)
        {
            current?.onRelease()
            current = base?.let { onCreateWorkingBeatmap(it) }

            forEachObserver { it.onMusicChange(current) }
        }

        current?.play(!isDifferent)
    }

    fun pause(instantly: Boolean = false) = musicScope.launch {

        current?.pause(instantly)
    }


    fun next() = musicScope.launch {

        current?.onRelease()
        play(songs.nextOf(current?.source))
    }

    fun previous() = musicScope.launch {

        current?.onRelease()
        play(songs.previousOf(current?.source))
    }
}