package com.reco1l.rimu.management.beatmap

import com.reco1l.rimu.MainContext
import com.reco1l.rimu.data.Beatmap
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.parser.BeatmapDecoder


class PlayableBeatmap(ctx: MainContext, source: Beatmap) : WorkingBeatmap(ctx, source)
{


    override var data = BeatmapDecoder().decode(source.toFile(ctx), true)

}