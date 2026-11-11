# Local Maven repository

Libraries that aren't published anywhere, so that library modules (which can't depend on a bare
`.aar` file) can use them.

- `com.osudroid.thirdparty:media3-decoder-ffmpeg:1.4.1`: Media3's FFmpeg decoder extension, built
  from [androidx/media](https://github.com/androidx/media/tree/release/libraries/decoder_ffmpeg)
  1.4.1. `verktex:android` plays videos through it.
