package au.com.foxtel.product.subtitleTools

import groovy.xml.MarkupBuilder

class CaptionMessage {
	def lines = new ArrayList<CaptionLine>() 
	int startOfMessage = 0
	int endOfMessage = 0
	
	def ebuTextField(StlTtiBlock message)
	{
		int row = message.verticalPosition
		startOfMessage = message.timecodeIn
		endOfMessage = message.timecodeOut
		
		def rows = splitLines(message.textField)

		rows.each {
			if (!it.isEmpty()) {
				CaptionLine line = new CaptionLine()
				line.parseEbuText(row, it)
				if (!line.text.empty) {
					this.lines.add(line)
				}
			}
			row++
		}
	}
	
	def ArrayList<ArrayList<Byte>> splitLines(byte[] textField)
	{
		int row = 0
		ArrayList<ArrayList<Byte>> rows = new ArrayList<ArrayList<Byte>>()
		rows[0] = new ArrayList<Byte>()
		
		textField.each {
			switch((int) it & 0xff) {
				case 0x8a:
					row++
					rows[row] = new ArrayList<Byte>()
					break
				case 0x8f:
					break
				default:
					rows[row].add(it)
			}			
		}
		return rows
	}
	
 	def String toTTML(MarkupBuilder xml)
	{
		boolean topRegion = true
		int renderRow = 1;
		
		if (lines[0].row < 13) {
			topRegion = true
			renderRow = lines[0].row		
		} else {
			topRegion = false
			renderRow = lines.get(lines.size() -1).row
		}
		
		// Put the message into a paragraph
		xml.p(region:topRegion?"top":"bottom", begin:framesToIsoTime(startOfMessage), end:framesToIsoTime(endOfMessage)) {
			//mkp.comment("row:" + lines[0].row + " to row:" + lines.get(lines.size() -1).row)
			// Vertically position for a top region
			if (topRegion) { 
				for (def i = 1; i < renderRow; i++) {
					xml.br()
				}		
			}
			
			// Render the text
			lines.each {
				it.toTTML(xml)
				xml.br()
			}
			
			// Vertically position for a bottom region
			if (!topRegion) {
				for (def i = renderRow; i < 22; i++) {
					xml.br()
				}
			}
		}
	}
	
	def String toVTT()
	{
		def output = ""
		
		// Render the text
		output += framesToIsoTime(startOfMessage) + " --> " + 
				  framesToIsoTime(endOfMessage)   + " " +
				  "line:" + ((lines[0].row >> 1) + 1) + " " + 
				  "align:middle\n"
		lines.each {
			output += it.toVTT()
		}
		output += "\n"

		return output
	}
	
	def String toString()
	{
		def output = ""
		lines.each {
			output += it.toString() + "\n"
		}
		"SOM: " + framesToIsoTime(this.startOfMessage) + " EOM: " + framesToIsoTime(this.endOfMessage) + "\n" + output
	}
	
	def String framesToIsoTime(int frames)
	{

		String output = ""
		
		output += sprintf("%02d:", (int) (frames / (3600 * 25)))
		output += sprintf("%02d:", Math.round(frames / (60 * 25)).intValue() % 60)
		output += sprintf("%02d.", Math.round(frames / 25).intValue() % 60)
		output += sprintf("%03d", (frames % 25).intValue() * 40)
		
		output
	}
}
