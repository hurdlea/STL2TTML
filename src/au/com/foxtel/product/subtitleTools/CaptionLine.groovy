package au.com.foxtel.product.subtitleTools

import groovy.xml.MarkupBuilder


class CaptionLine {
	Boolean empty = true
	int row
	ArrayList<LineFormat> text = new ArrayList<LineFormat>()
	String[] ebuColours = ['black', 'red', 'green', 'yellow', 'blue', 'magenta', 'cyan', 'white']
	
	def parseEbuText(int row, ArrayList<Byte> line)
	{
		this.row = row
		def format = new LineFormat()
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
					break;
					
				case 0x1d: // background colour change
				    backgroundChange = true
					format.text += ' '
					break;
					
				case [0x0a, 0x8f, 0x8a]: // line termination
					if (!format.text.trim().empty) {
						this.text.add(format)
						format = new LineFormat()
					}
					break;
					
				case 0x20..0x7e:
				case 0xa0..0xff: // regular characters
					int chr = mapCharacter((int) it & 0xff)
					format.text += new String( chr as int[], 0, 1)
					break;
					
				default:
					// add a space to replace the control character
					format.text += ' '
					break;
			}
		}
		
		if (!format.text.trim().empty) {
			this.text.add(format)
		}
	}
	
	def String toString()
	{
		def output = ""
		text.each {
			output += it.toString() + " "
		}
		"row:" + row + " " + output
	}
	
	def String toTTML(MarkupBuilder xml)
	{
		boolean removeSpace = true
		text.each {
			String caption = it.text
			def m
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
	
	def String toVTT()
	{
		def output = ""
		text.each {
			String caption = it.text
			
			// Trim any leading spaces
			def m
			if ((m = it.text =~ /(\s*)(.+)/)) {
				caption = m.group(2)
			}

			if (it.colour != "white") {
				output += "<c." + it.colour + ">" + caption + "</c>"
			} else {
				output += caption
			}
			output += "\n"
		}
		
		return output
	}
	
	// Fix character mapping differences between EBU Latin and regular Latin set
	def int mapCharacter(int chr)
	{
		int mapped = 0
		switch(chr) {
			case 0x24: // Swap currency symbol for $ sign 
				mapped = 0xa4
				break
			case 0xa4: // Swap $ sign for currency symbol
				mapped = 0x24
				break
			default:
				mapped = chr
				 
		}
		return mapped
	}
}
