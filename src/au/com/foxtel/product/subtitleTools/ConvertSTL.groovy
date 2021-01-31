package au.com.foxtel.product.subtitleTools

//@GrabConfig(systemClassLoader=true)
//@Grab('info.picocli:picocli:4.2.0')
import groovy.cli.commons.CliBuilder
import groovy.xml.MarkupBuilder
import java.nio.ByteBuffer

class ConvertSTL {
	
	static void processFile(String stlFile, ArrayList captions, int log_level, int safe_area)
	{
		File file = new File(stlFile)
		ByteBuffer contents = ByteBuffer.wrap(file.getBytes())
		StlGsiBlock gsiBlock = new StlGsiBlock()
		byte[] gsiBytes = new byte[1024]
		contents.get(gsiBytes, 0, 1024)
		gsiBlock.parse(gsiBytes)

		if (log_level > 0) println(gsiBlock.toString())

		while (contents.position() < contents.capacity())
		{
			StlTtiBlock ttiBlock = new StlTtiBlock()
			while(ttiBlock.extensionBlockNumber != 0xff) {
				byte[] ttiBytes = new byte[128]
				contents.get(ttiBytes, 0, 128)
				ttiBlock.parse(ttiBytes)
			}
			CaptionMessage caption = new CaptionMessage(gsiBlock.maximumNumberOfRows, safe_area)

			if (log_level > 0) println(ttiBlock.toString())

			caption.ebuTextField(ttiBlock)
			if (!caption.lines.isEmpty()) {
				captions.add(caption)
			}
		}
	}
	
	static String getTTML(ArrayList<CaptionMessage> captions, int offset = 0)
	{
		def writer = new StringWriter()
		def xml = new MarkupBuilder(writer)
		xml.setDoubleQuotes(true)

		xml.mkp.xmlDeclaration(version:"1.0", encoding:"UTF-8")
		xml.tt(
			"xmlns:ttp":"http://www.w3.org/ns/ttml#parameter",
			'xmlns:tts':"http://www.w3.org/ns/ttml#syling",
			'xmlns':"http://www.w3.org/ns/ttml",
			'xmlns:ebutts':"urn:ebu:tt:style",
			'xmlns:xsi':"http://www.w3.org/2001/XMLSchema-instance",
			'ttp:timebase':"media",
			'xml:lang':"en",
			'ttp:cellResolution':"50 30"
			) {
			head() {
				styling() {
					style(
						'xml:id':"teletext", 
						'tts:fontFamily':"monospaceSansSerif", 
						'tts:fontSize':"125%",
						'tts:textAlign':"left",
						'tts:fontStyle':"normal",
						'tts:lineHeight':"normal",
						'tts:wrapOption':"noWrap",
						'tts:color':"#ffffff",
						'tts:showBackground':"whenActive",
						'tts:backgroundColor':"transparent",
						'ebutts:linePadding':'0.5c'
					)
				}
				layout() {
					region(
						'xml:id':"top",
						'tts:origin':"10% 10%",
						'tts:extent':"80% 80%",
						'tts:displayAlign':"before")
					region(
						'xml:id':"bottom",
						'tts:origin':"10% 10%",
						'tts:extent':"80% 80%",
						'tts:displayAlign':"after")
				}
			}
			body(style:"teletext") {
				div() {
					captions.each {
						it.toTTML(xml, offset)
					}
				}
			}
		}
			
		writer.toString()
	}

	static String getVTT(ArrayList<CaptionMessage> captions, int offset = 0, boolean styling)
	{
		def output = ""
		String style = '''\
STYLE
::cue {
  font-family: monospace;
  font-style: normal;
  font-weight: normal;
  background-color: black;
  color: white;
}
::cue(c.transparent) {
  color: transparent;
}
::cue(c.semi-transparent) {
  color: rgba(0, 0, 0, 0.5);
}
::cue(c.opaque) {
  color: rgba(0, 0, 0, 1);
}
::cue(c.blink) {
  text-decoration: blink;
}
::cue(c.white) {
  color: white;
}
::cue(c.red) {
  color: red;
}
::cue(c.green) {
  color: lime;
}
::cue(c.blue) {
  color: blue;
}
::cue(c.cyan) {
  color: cyan;
}
::cue(c.yellow) {
  color: yellow;
}
::cue(c.magenta) {
  color: magenta;
}
::cue(c.bg_transparent) {
  background-color: transparent;
}
::cue(c.bg_semi-transparent) {
  background-color: rgba(0, 0, 0, 0.5);
}
::cue(c.bg_opaque) {
  background-color: rgba(0, 0, 0, 1);
}
::cue(c.bg_white) {
  background-color: white;
}
::cue(c.bg_green) {
  background-color: lime;
}
::cue(c.bg_blue) {
  background-color: blue;
}
::cue(c.bg_cyan) {
  background-color: cyan;
}
::cue(c.bg_red) {
  background-color: red;
}
::cue(c.bg_yellow) {
  background-color: yellow;
}
::cue(c.bg_magenta) {
  background-color: magenta;
}
::cue(c.bg_black) {
  background-color: black;
}

'''
		output += "WEBVTT\n\n"
		if (styling) {
			output += style
		}
		captions.each {
			output += it.toVTT(offset, styling)
		}
		
		return output
	}

	static void main(args) {
		def cli = new CliBuilder(usage: 'STL2TTML -f file [-o offset] [-t false] [-v false] [-ns true] [-l 0] [-s 5]')
		cli.o(type: int, args:1, longOpt:'offset', defaultValue:"0",'set offset')
		cli.f(type: String, args:1, longOpt:'file', required:true, 'STL file to convert')
		cli.t(args:0, longOpt:'ttml', 'create a TTML file')
		cli.v(args:0, longOpt:'vtt', 'create a VTT file')
		cli.ns(args:0, longOpt: 'no-vtt-styling', 'Disable VTT styling')
		cli.l(type: int, args:1, longOpt:'logging-level', defaultValue:"0",'set the logging level 0 = off')
		cli.s(type: int, args:1, longOpt: 'safe-area-percent', defaultValue: "5", 'set the vertical safe area percentage')
		def options = cli.parse(args)

		ArrayList<CaptionMessage> captions = new ArrayList<CaptionMessage>()
		String file = options.f
		int offset = options.o
		boolean vttStyling = !options.ns
		boolean ttml = options.t
		boolean vtt = options.v
		int log_level = options.l
		int safe_area = options.s

		if (safe_area < 0 || safe_area > 50) {
			println('Safe area must be between 0 and 50')
			System.exit(1)
		}
		processFile(file, captions, log_level, safe_area)

		if (ttml) {
			def ttml_captions = getTTML(captions, offset)
			def ttmlFile = new File(file + ".ttml").newWriter()
			ttmlFile.println ttml_captions
			ttmlFile.close()
		}

		if (vtt) {
			def vtt_captions = getVTT(captions, offset, vttStyling)
			def vttFile = new File(file + ".vtt").newWriter()
			vttFile.println vtt_captions
			vttFile.close()
		}
	}
	
}
