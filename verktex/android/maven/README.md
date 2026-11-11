# Local Maven repository

Libraries that aren't published anywhere, so that this library module (which can't depend on a
bare `.aar` file) can use them. They keep the coordinates their upstream would publish them under.
Every project resolving `verktex:android`'s dependencies lists this repository, e.g. the root
`build.gradle`.

- `androidx.media3:media3-decoder-ffmpeg:1.4.1`: Media3's FFmpeg decoder extension, built from
  [androidx/media](https://github.com/androidx/media/tree/release/libraries/decoder_ffmpeg) 1.4.1,
  which Google doesn't publish. `ExoPlayerVideoDecoder` plays videos through it.
