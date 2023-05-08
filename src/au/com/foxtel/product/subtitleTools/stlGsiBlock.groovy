package au.com.foxtel.product.subtitleTools

import java.text.SimpleDateFormat

class StlGsiBlock {
	String codePageNumber
	String diskFormatCode
	String displayStandardCode
	String characterCodeTable
	String languageCode
	String originalProgrammeTitle
	String originalEpisodeTitle
	String translatedProgrammeTitle
	String translatedEpisodeTitle
	String translatorsName
	String translatorContactDetails
	String subtitleListReferenceCode
	Date   creationDate
	Date   revisionDate
	int    revisionNumber
	int    totalNumberOfTtiBlocks
	int    totalNumberOfSubtitles
	int    totalNumberOfSubtitleGroups
	int    maximumNumberOfCharaters
	int    maximumNumberOfRows
	int    timecodeStatus
	int    startOfProgramme
	int    firstInCue
	int    totalNumberOfDisks
	int    diskSequenceNumber
	String countryOfOrigin
	String publisher
	String editorsName
	String editorsContactDetails
	String userDefinedArea
	int    frameRate
	int    autoZeroCueTime
	
	void parse(byte[] data)
	{
		this.codePageNumber = getString(data[0..2])
		this.diskFormatCode = getString(data[3..10])
		this.displayStandardCode = new String(data[11])
		this.characterCodeTable = getString(data[12..13])
		this.languageCode = getString(data[14..15])
		this.originalProgrammeTitle = getString(data[16..47])
		this.originalEpisodeTitle = getString(data[48..79])
		this.translatedProgrammeTitle = getString(data[80..111])
		this.translatedEpisodeTitle = getString(data[112..143])
		this.translatorsName = getString(data[144..175])
		this.translatorContactDetails = getString(data[176..207])
		this.subtitleListReferenceCode = getString(data[208..223])
		this.creationDate = getDate(data[224..229])
		this.revisionDate = getDate(data[230..235])
		this.revisionNumber = getInteger(data[236..237], 'GSI:revisionNumber')
		this.totalNumberOfTtiBlocks = getInteger(data[238..242], 'GSI:numberOfTtiBlocks')
		this.totalNumberOfSubtitles = getInteger(data[243..247], 'GSI:numberOfSubtitles')
		this.totalNumberOfSubtitleGroups = getInteger(data[248..250], 'GSI:numberOfSubtitleGroups')
		this.maximumNumberOfCharaters = getInteger(data[251..252], 'GSI:numberOfCharacters')
		this.maximumNumberOfRows = getInteger(data[253..254], 'GSI:maximumNumberOfRows')
		this.timecodeStatus = data[255].toInteger()
		this.startOfProgramme = StringToFrames(data[256..263] as byte[], 'GSI:startOfProgramme')
		this.firstInCue = StringToFrames(data[264..271] as byte[], 'GSI:firstInCue')
		this.totalNumberOfDisks = getIntFromChar(data[272], 'GSI:totalNumberOfDisks')
		this.diskSequenceNumber = getIntFromChar(data[273], 'GSI:diskSequenceNumber')
		this.countryOfOrigin = getString(data[274..276])
		this.publisher = getString(data[277..308])
		this.editorsName = getString(data[309..340])
		this.editorsContactDetails = getString(data[341..372])
		this.userDefinedArea = getString(data[448..1023])

		switch (this.diskFormatCode == 'STL25.01') {
			case 'STL25.01':
				this.frameRate = 25
				break
			case 'STL30.01':
				this.frameRate = 30
				break
			case 'STL50.01':
				this.frameRate = 50
				break
			case 'STL60.01':
				this.frameRate = 60
				break
			default:
				this.frameRate = 25
		}

		this.autoZeroCueTime = (int)(firstInCue / 3600) * 3600
	}
	
	static Date getDate(List<Byte> data)
	{
		def string = getString(data)
		if (string == "")
		{
			string = "140101"
		}
		def format = new SimpleDateFormat("yyMMdd")
		format.parse(string)
	}

	static int getInteger(List<Byte> data, String field)
	{
		String string = new String(data as byte[], "US-ASCII")
		string.trim()
		return stringToInteger(string, field)
	}

	static String getString(List<Byte> data)
	{
		String string = new String(data as byte[], "US-ASCII")
		string.trim()
	}

	static int getIntFromChar(Byte data, String field)
	{
		String string = new String(data as byte[], "US-ASCII")
		string.trim()
		return stringToInteger(string, field)
	}

	static int stringToInteger(String string, String field) {
		if (string == '') {
			System.err.println('ERROR: Tried to convert field: ' + field + ' with null value (spaces) to integer')
			return 0
		}

		try {
			return string.toInteger()
		} catch (NumberFormatException e) {
			System.err.println(
					'ERROR: Tried to convert field: ' + field +
					' to integer with value [' + string + '] - assuming 0 value. Exception: ' +	e.toString()
			)
			return 0
		}
	}
	
	static int StringToFrames(byte[] data, String field)
	{
		int hours = getInteger(data[0..1], field + ':hours')
		int minutes = getInteger(data[2..3], field + ':minutes')
		int seconds = getInteger(data[4..5], field + ':seconds')
		int frames = getInteger(data[6..7], field + ':frames')
		
		(hours * (3600 * 25)) + (minutes * (60 * 25)) + (seconds * 25) + frames
	}

	String toString() {
		'[maxNumRows:' + maximumNumberOfRows + ' maxChars:' + maximumNumberOfCharaters + ']'
	}
}
