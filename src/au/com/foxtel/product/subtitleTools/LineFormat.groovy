package au.com.foxtel.product.subtitleTools

class LineFormat {
	String colour = "white"
	String background = "black"
	String text = ""
	
	def String toString()
	{
		"[Colour:" + this.colour + " Background:" + this.background + " Text:" + this.text + "]" 
	}
}
