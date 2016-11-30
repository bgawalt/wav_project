# WAVProject

This project takes two raw ingredients:

1. A long snippet of audio (.WAV format)
2. A shorter snippet of audio (also .WAV format)

and layers repeated echoes of (2), scaled and shifted,
to try and approximate (1).  Along the way, it writes
some of these successive approximations out to their
own .WAV files.

Let's call (1) the *target* audio, and (2) the *basis*
audio.

## How to make a WAVProject

From this directory, run this command:

```
sbt/sbt 'runMain com.gawalt.wav_project.WavProject 
 path/to/long_target_audio.wav
 path/to/short_basis_audio.wav
 path/to/output_filename_base
```

This will perform an approximation of the target audio by
layering scaled and shifted echoes of the basis audio.
The approximation is computed iteratively, one basis echo
per iteration.  

First run went slow:

1 update: 9:52 PM
2 updates: 9:54 PM
4 updates: 9:58 PM
8 updates: 10:04 PM
16 updates: 10:18 PM
32 updates: 10:43 PM
64 updates: 11:35 PM
128 updates: 1:15 AM
256 updates: 4:31 AM
512 updates: 11:10 AM
1024 updates: 12:16 AM
