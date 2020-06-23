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
		this.revisionNumber = getString(data[236..237]).toInteger()
		this.totalNumberOfTtiBlocks = getString(data[238..242]).toInteger()
		this.totalNumberOfSubtitles = getString(data[243..247]).toInteger()
		this.totalNumberOfSubtitleGroups = getString(data[248..250]).toInteger()
		this.maximumNumberOfCharaters = getString(data[251..252]).toInteger()
		this.maximumNumberOfRows = getString(data[253..254]).toInteger()
		this.timecodeStatus = data[255].toInteger()
		this.startOfProgramme = StringToFrames(data[256..263] as byte[])
		this.firstInCue = StringToFrames(data[264..271] as byte[])
		this.totalNumberOfDisks = getCharAsString(data[272]).toInteger()
		this.diskSequenceNumber = getCharAsString(data[273]).toInteger()
		this.countryOfOrigin = getString(data[274..276])
		this.publisher = getString(data[277..308])
		this.editorsName = getString(data[309..340])
		this.editorsContactDetails = getString(data[341..372])
		this.userDefinedArea = getString(data[448..1023])
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
	
	static String getString(List<Byte> data)
	{
		String string = new String(data as byte[], "US-ASCII")
		//println "[" + string + "]"
		string.trim()
	}
	
	static String getCharAsString(Byte data)
	{
		String string = new String(data as byte[], "US-ASCII")
		//println "[" + string + "]"
		string.trim()
	}
	
	static int StringToFrames(byte[] data)
	{
		int hours = getString(data[0..1]).toInteger() 
		int minutes = getString(data[2..3]).toInteger() 
		int seconds = getString(data[4..5]).toInteger() 
		int frames = getString(data[6..7]).toInteger()
		
		(hours * (3600 * 25)) + (minutes * (60 * 25)) + (seconds * 25) + frames
	}
}
