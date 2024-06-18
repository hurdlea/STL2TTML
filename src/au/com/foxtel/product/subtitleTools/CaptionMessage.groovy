package au.com.foxtel.product.subtitleTools

import groovy.xml.MarkupBuilder

class CaptionMessage {
	def lines = new ArrayList<CaptionLine>() 
	int startOfMessage = 0
	int endOfMessage = 0
	int maximumRowHeight = 0
	int safeAreaRows = 0
	int frame_rate

	CaptionLine.LineAlignment align

	CaptionMessage(int max_rows, int safe_area, int frame_rate) {
		this.maximumRowHeight = max_rows
		this.frame_rate = frame_rate
		if (safe_area > 0 && safe_area < 100) {
			this.safeAreaRows = (int) Math.ceil((double) max_rows * (double) safe_area / 100)
		} else {
			this.safeAreaRows = 0
		}
	}

	void ebuTextField(StlTtiBlock message)
	{
		int row = message.verticalPosition
		startOfMessage = message.timecodeIn
		endOfMessage = message.timecodeOut
		align = CaptionLine.LineAlignment.valueOf(message.justificationCode)
		
		def rows = splitLines(message.textField)

		rows.each {
			if (!it.isEmpty()) {
				CaptionLine line = new CaptionLine()
				line.align = align
				line.parseEbuText(row, it)
				if (!line.text.empty) {
					this.lines.add(line)
				}
			}
			row++
		}
	}
	
	static ArrayList<ArrayList<Byte>> splitLines(byte[] textField)
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
	
 	String toTTML(MarkupBuilder xml, int offset)
	{
		boolean topRegion = true
		int renderRow = 1
		
		if (lines[0].row < 13) {
			topRegion = true
			renderRow = lines[0].row		
		} else {
			topRegion = false
			renderRow = lines.get(lines.size() -1).row
		}
		
		// Put the message into a paragraph
		xml.p(region:topRegion?"top":"bottom", begin:framesToIsoTime(startOfMessage + offset), end:framesToIsoTime(endOfMessage + offset)) {
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

	// Format object for the VTT reformatter renderer
	class CaptionFormat {
		boolean positionalSpeaker = false
		boolean colourSpeaker = false
		String speakerColour = 'white'
		String text = ''
		int start = 0
		int end = 0
		boolean newline = true
		int lineLength = 0
	}

	// Jira VT-4861 Implement Business formatting of STL captions
	// Implement the business reformatting rules
	String[] toVTTWithReformatting(int offset, String speakerColour, boolean styling) {
		String output = ""
		ArrayList<CaptionFormat> rows
		rows = new ArrayList<CaptionFormat>()
		// A set of leading characters of a caption line that are not candidates
		// for speaker identification.
		final String noSpeakerCases = "\u2015-<>['#("
		// NOTE: Changed the speaker identification character from horizontal Bar \u2015
		//       to hyphen "-" as Unicode is not supported on Hubbl. 18-06-24 AH
		//       This is part of Jira VT-4861 and should be unwound when Unicode is
		//       supported on the Hubbl devices.
		final String speakerCharacter = "-"

		// Determine the required processing for the VTT Cue based on Foxtel Business rules
		boolean centered = this.align == CaptionLine.LineAlignment.CENTRE

		int previousStart
		int previousEnd

		CaptionFormat row = new CaptionFormat()

		lines.each {
			// Each line has a number of format units that need to be assessed

			int start = it.charStart
			int end = it.charStart + it.lineLength
			if (rows.size() > 0 && !centered && it.text.size() == 1) {
				// Identify use of horizontal positioning for speaker identification
				int index = rows.size() - 1
				previousStart = rows[index].start
				previousEnd = rows[index].end
				if (
				// Identify blocks of text that are positionally offset to indicate
				// a speaker. Only do this for plain white blocks without other formatting!
						(it.text[0].colour == 'white' && !rows[index].colourSpeaker && noSpeakerCases.indexOf(it.text[0].text[0]) == -1) &&
								((previousStart < start && previousEnd < end && start < previousEnd && Math.abs(previousStart - start) > 2) ||
										(start < previousStart && end < previousEnd && previousStart < end  && Math.abs(previousStart - start) > 2) ||
										(start >= previousEnd) ||
										(end <= previousStart))
				) {
					// We are detecting horizontal positioning detecting two different speakers
					// This means the initial case has to be 2 identified speaker lines
					if (index == 0 && noSpeakerCases.indexOf(rows[index].text[0]) == -1) {
						rows[index].positionalSpeaker = true
					}
					row.positionalSpeaker = true
					//println("Positional Speaker :[" + it.text[0].colour + "]"+ it.text[0].text)
				}

			}

			it.text.each {
				// If there is a change in speaker colour capture it here and
				// mark the CaptionFormat for speaker identification
				if (it.colour != speakerColour && !row.positionalSpeaker && noSpeakerCases.indexOf(it.text[0]) == -1) {
					row.colourSpeaker = true
					speakerColour = it.colour
					//println("Colour Speaker :[" + it.colour + "]" + it.text)
				}
				row.text = it.text
				row.speakerColour = it.colour
				row.start = start
				row.end = start + row.text.length()
				row.lineLength = it.text.length()
				rows.add(row)
				start = row.end + 1

				row = new CaptionFormat()
			}
		}

		int maxCueLines = 3
		int activeRows = rows.size()
		if (rows.size() > maxCueLines) {
			// Reduce the number of rows to the desired business requirements
			// Look for consecutive rows that are < 39 chars that can be merged
			int merges = rows.size() - maxCueLines

			for (int index = 1; index < rows.size(); index++) {
				if (rows[index - 1].lineLength + rows[index].lineLength < 40) {
					rows[index - 1].newline = false
					rows[index].lineLength += rows[index].lineLength + 1
					merges--
					activeRows--
					if (merges == 0) break
				}
			}
		}

		// Detect lines below the bottom of the screen
		int multiplier = 2
		if (maximumRowHeight < 15) multiplier = 1
		if (lines[0].row + (activeRows * multiplier) > maximumRowHeight) {
			// Shift up so that all captions are visible
			lines[0].row = maximumRowHeight - (activeRows * multiplier)
		}

		// Render the VTT Cue Message
		String startTime = framesToIsoTime(startOfMessage + offset)
		String endTime = framesToIsoTime(endOfMessage + offset)
		// Have a 10% safe area caption viewport
		long line = Math.round(((lines[0].row) / maximumRowHeight) * 80) + 10
		output += "${startTime} --> ${endTime} line:${line}% align:center size:80%\n"

		// Now render out the captions lines
		rows.each {
			if (it.colourSpeaker || it.positionalSpeaker) {
				output += speakerCharacter + it.text
			} else {
				output += it.text
			}
			if (it.newline) output += "\n"
		}
		output += "\n"

		return [output, rows.last().speakerColour]
	}

	String toVTT(int offset, boolean styling)
	{
		String output = ""
		int start = 0

		if (align == CaptionLine.LineAlignment.NONE) {
			start = 1000
			// Determine the lowest start position of the text
			lines.each {
				if (start > it.charStart) {
					start = it.charStart
				}
			}
		}

		//println("Start: ${start} ${lines.toString()}")
		String formatting

		switch (align) {
			case CaptionLine.LineAlignment.NONE:
				long position = Math.round(((5 + start)/50) * 100)
				formatting  = "align:left"
				formatting += " position:${position}%"
				//formatting += " size:${80 - position}%"
				break

			case CaptionLine.LineAlignment.LEFT:
				formatting = 'align:left size:80% position:10%'
				break

			case CaptionLine.LineAlignment.RIGHT:
				formatting = 'align:right size:80% position:90%'
				break

			default:
				formatting = 'align:middle position:50% size:80%'
		}

		// Render the text
		output += framesToIsoTime(startOfMessage + offset) + " --> " + 
				  framesToIsoTime(endOfMessage + offset)   + " " +
				  "line:" + Math.round((lines[0].row + this.safeAreaRows) / (maximumRowHeight + (this.safeAreaRows * 2) + 1) * 100) + "% " +
				  "${formatting}\n"
		lines.each {
			output += it.toVTT(start, styling)
		}
		output += "\n"

		return output
	}
	
	String toString()
	{
		def output = ""
		lines.each {
			output += it.toString() + "\n"
		}
		"SOM: " + framesToIsoTime(this.startOfMessage) + " EOM: " + framesToIsoTime(this.endOfMessage) + "\n" + output
	}
	
	String framesToIsoTime(int frames)
	{

		String output = ""
		
		output += sprintf("%02d:", (int) (frames / (3600 * this.frame_rate)))
		output += sprintf("%02d:", (int) (frames / (60 * this.frame_rate)).intValue() % 60)
		output += sprintf("%02d.", (int) (frames / this.frame_rate).intValue() % 60)
		output += sprintf("%03d",  ((frames % this.frame_rate) * (1000.0 / this.frame_rate)).intValue())
		
		output
	}
}
