package au.com.foxtel.product.subtitleTools

import java.util.List;

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
		this.subtitleGroupNumber = ucharToInt(record[0])
		this.subtitleNumber = ucharToInt(record[1]) + (ucharToInt(record[2]) << 8)
		this.extensionBlockNumber = ucharToInt(record[3])
		this.cumulativeStatus = ucharToInt(record[4])
		this.timecodeIn = getTimecode(record[5..8])
		this.timecodeOut = getTimecode(record[9..12])
		this.verticalPosition = ucharToInt(record[13])
		this.justificationCode = ucharToInt(record[14])
		this.commentFlag = ucharToInt(record[15])
		this.textField = record[16..127] as byte[]
	}
	
	def ucharToInt(byte b)
	{
		(int) (b & 0xff)
	}
	
	def getTimecode(List<Byte> data)
	{
		(data[0] * (3600 * 25)) + (data[1] * (60 * 25)) + (data[2] * 25) + data[3]
	}
	
	def String toString()
	{
		"[sn:" + subtitleNumber + " eb:" + extensionBlockNumber + " tcin:" + timecodeIn + " tcout:" + timecodeOut + " text:" + makePrintable(textField) + "]"
	}
	
	def String makePrintable(byte[] text)
	{
		def output = ""
		text.each {
			switch(ucharToInt(it))
			{
				case 0x20..0x7e:
					output += (char) it
					break
				default:
					output += "[#" + Integer.toHexString(it & 0xff) + "]"
			}
		}
		output
	}
}
