package au.com.foxtel.product.subtitleTools

import groovy.xml.MarkupBuilder

import java.nio.ByteBuffer

import groovy.xml.XmlUtil

class ConvertSTL {
	
	static void processFile(String stlFile, ArrayList captions)
	{
		File file = new File(stlFile)
		ByteBuffer contents = ByteBuffer.wrap(file.getBytes())
		StlGsiBlock gsiBlock = new StlGsiBlock()
		byte[] gsiBytes = new byte[1024]
		contents.get(gsiBytes, 0, 1024)
		gsiBlock.parse(gsiBytes)

		while (contents.position() < contents.capacity())
		{
			StlTtiBlock ttiBlock = new StlTtiBlock()
			while(ttiBlock.extensionBlockNumber != 0xff) {
				byte[] ttiBytes = new byte[128]
				contents.get(ttiBytes, 0, 128)
				ttiBlock.parse(ttiBytes)
			}
			CaptionMessage caption = new CaptionMessage()
			caption.ebuTextField(ttiBlock)
			if (!caption.lines.isEmpty()) {
				captions.add(caption)
			}
		}
	}
	
	static String getTTML(ArrayList captions, int offset = 0)
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

	static String getVTT(ArrayList captions, int offset = 0)
	{
		def output = ""
		
		output += "WEBVTT\n\n"
		captions.each {
			output += it.toVTT(offset)
		}
		
		return output
	}

	static void main(args) {
		
		def captions = new ArrayList<CaptionMessage>()
		def file = args[0]
		def offset = args[1] ?: "0"
		offset = Integer.parseInt(offset)
		
		processFile(file, captions)
		
		def ttml_captions = getTTML(captions, offset)
		def ttml = new File(args[0] + ".ttml").newWriter()
		ttml.println ttml_captions
		ttml.close()

		def vtt_captions = getVTT(captions, offset)
		def vtt = new File(args[0] + ".vtt").newWriter()
		vtt.println vtt_captions
		vtt.close()
	}
	
}
