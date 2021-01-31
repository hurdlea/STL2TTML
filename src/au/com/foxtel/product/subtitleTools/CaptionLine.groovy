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
					
				case 0x20..0x7e:
				case 0xa0..0xff: // regular characters
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
	
	// Fix character mapping differences between EBU Latin and regular Latin set
	static String mapCharacter(int chr)
	{
		String mapped
		switch(chr) {
			case 0x26:
				mapped = '&amp;'
				break
			case 0x3c:
				mapped = '&lt;'
				break
			case 0x3e:
				mapped = '&gt;'
				break
			case 0x24: // Swap currency symbol for $ sign 
				mapped = '$'
				break
			case 0xa4: // Swap $ sign for currency symbol
				mapped = '£'
				break
			default:
				mapped = new String( chr as int[], 0, 1)
				 
		}
		return mapped
	}
}
