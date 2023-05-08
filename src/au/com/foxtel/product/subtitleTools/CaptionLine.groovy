package au.com.foxtel.product.subtitleTools

import groovy.xml.MarkupBuilder

import java.util.regex.Matcher


class CaptionLine {

	enum LineAlignment {
		NONE(0), LEFT(1), CENTRE(2), RIGHT(3)
		private int value
		private static Map map = new HashMap<>()

		private LineAlignment(int value) {
			this.value = value
		}

		static {
			for (LineAlignment alignment : values()) {
				map.put(alignment.value, alignment)
			}
		}

		static LineAlignment valueOf(int alignment) {
			return (LineAlignment) map.get(alignment)
		}

		int getValue() {
			value
		}
	}


	public LineAlignment align = LineAlignment.CENTRE
	public int charStart = 0

	int row
	protected ArrayList<LineFormat> text = new ArrayList<LineFormat>()
	private String[] ebuColours = ['black', 'red', 'lime', 'yellow', 'blue', 'magenta', 'cyan', 'white']

	def parseEbuText(int row, ArrayList<Byte> line)
	{
		this.row = row
		LineFormat format = new LineFormat()
		boolean backgroundChange = false
		
		line.each {
			switch ( (int) it & 0xff ) {
				case 0x00..0x07: // foreground colour
					// Change the formatting for the text block if there is already an associated style to text 
					if (!format.text.trim().empty) {
						this.text.add(format)
						format = new LineFormat()
					}
					
					if (backgroundChange) {
						format.background = ebuColours[it as byte]
						backgroundChange = false
					} else {
						format.colour = ebuColours[it as byte]
					}
					format.text += ' '
					break
					
				case 0x1d: // background colour change
				    backgroundChange = true
					format.text += ' '
					break
					
				case [0x0a, 0x8f, 0x8a]: // line termination
					if (!format.text.trim().empty) {
						this.text.add(format)
						format = new LineFormat()
					}
					break
					
				case {(0x20..0x7e).contains(it) || (0xa0..0xff).contains(it)}: // regular characters
					String chr = mapCharacter((int) it & 0xff)
					format.text += chr
					break
					
				default:
					// add a space to replace the control character
					format.text += ' '
					break
			}
		}
		
		if (!format.text.trim().empty) {
			this.text.add(format)
		}

		if (this.text.size() > 0) {
			String text = this.text[0].text
			Matcher m
			if(m = text =~ /(\s+)(.+)/) {
				this.charStart = m.group(1).size()
				this.text[0].text = m.group(2)
				//println("Row: ${this.row} Align: ${align} Offset: ${this.charStart} ${m.group(2)}")
			}
		}
	}
	
	String toString()
	{
		def output = ""
		text.each {
			output += it.toString() + " "
		}
		"row:" + row + " " + output
	}
	
	String toTTML(MarkupBuilder xml)
	{
		boolean removeSpace = true
		text.each {
			String caption = it.text
			Matcher m
			if (removeSpace) {
				if ((m = it.text =~ /(\s+)(.+)/)) {
					
					xml.span('xml:space':"preserve", m.group(1))
					caption = m.group(2)
				}
				removeSpace = false
			} 
			xml.span('tts:color':it.colour, 'tts:backgroundColor':it.background, 'xml:space':"preserve", caption)
		}
	}
	
	String toVTT(int start, boolean styling)
	{
		boolean first_unit = true
		String output = ""

		text.each {
			String caption = it.text

			if (align == LineAlignment.NONE && first_unit) {
				int length = this.charStart - start
				if (length > 1) {
					if (styling) {
						output += "<c.black>" + "." * length + "</c>"
					} else {
						output += "&nbsp;" + " " * length - 1
					}
				}
			}

			if (it.colour != "white" && styling) {
				output += "<c." + it.colour + ">" + caption + "</c>"
			} else {
				output += caption
			}
			first_unit = false
		}
		output += "\n"

		return output
	}

	static final Map<Integer, String>charMap = [
			0x24: "\u00a4",  // ¤
			0x26: "&amp;",
			0x3c: "&lt;",
			0x3e: "&gt;",

			0xa4: "\u0024",  // $
			0xa9: "\u2018",  // ‘
			0xaa: "\u201C",  // “
			0xab: "\u00AB",  // «
			0xac: "\u2190",  // ←
			0xad: "\u2191",  // ↑
			0xae: "\u2192",  // →
			0xaf: "\u2193",  // ↓

			0xb4: "\u00D7",  // ×
			0xb8: "\u00F7",  // ÷
			0xb9: "\u2019",  // ’
			0xba: "\u201D",  // ”
			0xbc: "\u00BC",  // ¼
			0xbd: "\u00BD",  // ½
			0xbe: "\u00BE",  // ¾
			0xbf: "\u00BF",  // ¿

/*
			0xc0: " ",       // blank
			0xc1: "`",       // '
			0xc2: "\u00B4",  // acute accent
			0xc3: "^",       // ^
			0xc4: "~",       // ~
			0xc5: "\u00AF",  // macron
			0xc6: "\u1D55",  // small bottom half O
			0xc7: "\u00B7",  // middle dot
			0xc8: "\u00A8",  // diaeresis
			0xc9: " ",       // blank
			0xca: "\u00B0",  // degree sign
			0xcb: "\u25DE",  // lower right quadrant
			0xcc: "\uA7F7",  // sideways I
			0xcd: "\u02AE",  // double apostrophe
			0xce: "\u25DF",  // lower left quadrant
			0xcf: "\u02EC",  // letter voicing
*/

			0xd0: "\u2015",  // ―
			0xd1: "\u00B9",  // ¹
			0xd2: "\u00AE",  // ®
			0xd3: "\u00A9",  // ©
			0xd4: "\u2122",  // ™
			0xd5: "\u266A",  // ♪
			0xd6: "\u00AC",  // ¬
			0xd7: "\u00A6",  // ¦
			0xdc: "\u215B",  // ⅛
			0xdd: "\u215C",  // ⅜
			0xde: "\u215D",  // ⅝
			0xdf: "\u215E",  // ⅞

			0xe0: "\u03A9",  // Ohm Ω
			0xe1: "\u00C6",  // Æ
			0xe2: "\u0110",  // Đ
			0xe3: "\u1EA1",  // ạ
			0xe4: "\u0126",  // Ħ
			0xe6: "\u0132",  // Ĳ
			0xe7: "\u013F",  // Ŀ
			0xe8: "\u0141",  // Ł
			0xe9: "\u00D8",  // Ø
			0xea: "\u0152",  // Œ
			0xeb: "\u1ECD",  // ọ
			0xec: "\u00DE",  // Þ
			0xed: "\u0166",  // Ŧ
			0xee: "\u014A",  // Ŋ
			0xef: "\u0149",  // ŉ

			0xf0: "\u0138",  // ĸ
			0xf1: "\u00E6",  // æ
			0xf2: "\u0111",  // đ
			0xf3: "\u00F0",  // ð
			0xf4: "\u0127",  // ħ
			0xf5: "\u0131",  // ı
			0xf6: "\u0133",  // ĳ
			0xf7: "\u0140",  // ŀ
			0xf8: "\u0142",  // ł
			0xf9: "\u00F8",  // ø
			0xfa: "\u0153",  // œ
			0xfb: "\u00DF",  // ß
			0xfc: "\u00FE",  // þ
			0xfd: "\u0167",  // ŧ
			0xfe: "\u014B",  // ŋ
			0xff: "\u00AD",  // Soft hyphen
	]
	// Fix character mapping differences between EBU Latin and regular Latin set
	static String mapCharacter(int chr)
	{
		String mapped = charMap.get(chr)
		if (mapped == null) {
			return new String( chr as int[], 0, 1)
		}
		return mapped
	}
}
