package au.com.foxtel.product.subtitleTools

class StlTtiBlock {
	int subtitleGroupNumber
	int subtitleNumber
	int extensionBlockNumber
	int cumulativeStatus
	int timecodeIn
	int timecodeOut
	int verticalPosition
	int justificationCode
	int commentFlag
	byte[] textField
	int timecodeOffset
	int frameRate

	StlTtiBlock(int offset, int frameRate = 25) {
		this.timecodeOffset = offset
		this.frameRate = frameRate
	}

	void parse(byte[] record)
	{
		if (textField)
		this.subtitleGroupNumber = ucharToInt(record[0])
		this.subtitleNumber = ucharToInt(record[1]) + (ucharToInt(record[2]) << 8)
		this.extensionBlockNumber = ucharToInt(record[3])
		this.cumulativeStatus = ucharToInt(record[4])
		this.timecodeIn = getTimecode(record[5..8])
		this.timecodeOut = getTimecode(record[9..12])
		this.verticalPosition = ucharToInt(record[13])
		this.justificationCode = ucharToInt(record[14])
		this.commentFlag = ucharToInt(record[15])
		// When there are extension blocks accumulate the text portion
		if (this.textField) {
			byte[] buffer = new byte[this.textField.length + 112]
			System.arraycopy(this.textField, 0, buffer, 0, this.textField.length)
			System.arraycopy(record, 16, buffer, this.textField.length, 112)
			this.textField = buffer

			//println("Multiple text blocks ${makePrintable(buffer)}")
		} else {
			this.textField = record[16..127] as byte[]
		}
	}
	
	def static ucharToInt(byte b)
	{
		(int) (b & 0x00ff)
	}


	def getTimecode(List<Byte> data)
	{
		((data[0] * (3600 * this.frameRate)) + (data[1] * (60 * this.frameRate)) + (data[2] * this.frameRate) + data[3]) - timecodeOffset
	}

	String toString()
	{
		"[sn:" + subtitleNumber.toString().padLeft(5) +
		" eb:" + extensionBlockNumber.toString().padLeft(3) +
		" tcin:" + framesToIsoTime(timecodeIn) +
		" tcout:" + framesToIsoTime(timecodeOut) +
		" vp:" + verticalPosition.toString().padLeft(2) +
		" text:\"" + makePrintable(textField) + "\"]"
	}
	
	static String makePrintable(byte[] text) {
		String[] ebuColours = ['black', 'red', 'green', 'yellow', 'blue', 'magenta', 'cyan', 'white']
		String output = new String()

		text.each {
			int b = it & 0xff
			switch (b) {
				case 0x8f:
					// Remove padding bytes
					break
				case 0x8a:
					output += '\\n'
					break
				case 0x80..0x85:
					// Remove Italics, Underline and boxing specifiers
					break
				case 0x00..0x07:
					output += "[#" + ebuColours[b] + "]"
					break
				case {(0xa0..0xff).contains(it) || (0x20..0x7e).contains(it)}:
					output += CaptionLine.mapCharacter(b)
					break
				default:
					//output += "[#" + (b as byte[]).encodeHex() + "]"
					break
			}
		}
		output
	}

	String framesToIsoTime(int frames)
	{

		String output = ""

		output += sprintf("%02d:", (int) (frames / (3600 * this.frameRate)))
		output += sprintf("%02d:", (int) (frames / (60 * this.frameRate)).intValue() % 60)
		output += sprintf("%02d.", (int) (frames / this.frameRate).intValue() % 60)
		output += sprintf("%03d",  ((frames % this.frameRate) * (1000.0 / this.frameRate)).intValue())

		output
	}
}
