# EBU STL Convert
The goal of this project is to convert an EBU STL (Tech 3264) binary file and produce equivalent web centric 
captioning formats.

Output formats supported:
- TTML text captions using regions for vertical placement and in-line text colour styling with spans
- WebVTT with vertical placement and colour support using cue::color styling

```
STL2TTML Options:

 -f | -file              STL file to process
 -o | -offset            number of seconds to add to the STL timing
 -t | -ttml              Create TTML file
 -v | -vtt               Create VTT file
-ns | -no-vtt-styling    Disable all VTT Styling
 -l | -log-level         Logging level > 0 to produce output
 -s | -safe-area         VTT Vertical safe area precentage 0 .. 50
 -z | -zero-timecode     Remove any non-zero timecode reference from the STL captions
```
The safe-area option prevents the caption text from being rendered on the very top and bottom of the player area. 
The default value is set at 5% but can be disabled by setting -s 0 on the command line.

The zero-timecode option removes timecode offsets that are multiples of 1 hour added to the first valid timecode 
(firstInCue from the GsiBlock). Any text blocks before the firstInCue position are ignored when using this option flag.
# References
- https://tech.ebu.ch/docs/tech/tech3264.pdf
- https://tech.ebu.ch/docs/tech/tech3360.pdf
- https://tech.ebu.ch/docs/tech/tech3380.pdf

