# EBU STL Convert
The goal of this project is to convert an EBU STL (Tech 3264) binary file and produce equavalent web centric captioning formats.

Outptut formats supported:
- TTML text captions using regions for vertical placement and in-line text colour styling with spans
- WebVTT with vertical placement and colour support using cue::color styling

```
STL2TTML Options:

 -f | -file              STL file to process
 -o | -offset            number of seconds to add to the STL timing
 -t | -ttml              Create TTML file
 -v | -vtt               Create VTT file
-ns | -no-vtt-styling    Disable all VTT Styling
```
 	
# References
- https://tech.ebu.ch/docs/tech/tech3264.pdf
- https://tech.ebu.ch/docs/tech/tech3360.pdf
- https://tech.ebu.ch/docs/tech/tech3380.pdf

