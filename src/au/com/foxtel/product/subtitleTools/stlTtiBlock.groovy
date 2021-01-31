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


	def static getTimecode(List<Byte> data)
	{
		(data[0] * (3600 * 25)) + (data[1] * (60 * 25)) + (data[2] * 25) + data[3]
	}

	String toString()
	{
		"[sn:" + subtitleNumber + " eb:" + extensionBlockNumber + " tcin:" + timecodeIn + " tcout:" + timecodeOut + " vp:" + verticalPosition + " text:\"" + makePrintable(textField) + "\"]"
	}
	
	static String makePrintable(byte[] text) {
		String[] ebuColours = ['black', 'red', 'green', 'yellow', 'blue', 'magenta', 'cyan', 'white']
		String output = new String()

		text.each {
			int b = it & 0xff
			switch (b) {
				case 0x20..0x7e:
					String chr = new String(b as int[], 0, 1)
					output += chr
					break
				case 0x8f:
					// Remove padding bytes
					break
				case 0x8a:
					output += '\\n'
					break
				case 0x84..0x85:
					// Remove Boxing specifiers
					break
				case 0x00..0x07:
					output += "[#" + ebuColours[b] + "]"
					break
				default:
					output += "[#" + (b as byte[]).encodeHex() + "]"
			}
		}
		output
	}
}
