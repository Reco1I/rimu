# Local Maven repository

Libraries that aren't published anywhere, so that this library module (which can't depend on a
bare `.aar` file) can use them. They keep the coordinates their upstream would publish them under.
Every build resolving `:android`'s dependencies lists this repository: Verktex's `settings.gradle.kts`, and
the `build.gradle` of an application including it.

- `androidx.media3:media3-decoder-ffmpeg:1.4.1`: Media3's FFmpeg decoder extension, built from
  [androidx/media](https://github.com/androidx/media/tree/release/libraries/decoder_ffmpeg) 1.4.1,
  which Google doesn't publish. `ExoPlayerVideoDecoder` plays videos through it.
