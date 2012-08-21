
/*
 * Filename:		UnsignedData.java
 * Package:			utilities
 * Project:			padA2
 * By:				Justin Lang, Josh Wagler & Tina Kurian
 * Date:			March 8, 2012
 * Description:		Contains the UnsignedData class
 */



package utilities;



/*
 * Class Name:		UnsignedData
 * Description:		Contains methods that "convert" data types to unsigned versions, by copying them into
 * 					larger sized data types to avoid having the high bit used as a signed bit
 */
public class UnsignedData {
	
	
	
	/*
	 * Method Name:		convertByteToUnsignedByte
	 * Description:		Puts a byte value into a short and clears the high byte
	 * Parameters:		byte signedByte: The byte to convert
	 * Return:			The short value
	 */	
	public static short convertByteToUnsignedByte(byte signedByte) {
		short unsignedByte = signedByte;
		return (unsignedByte &= (short)0x00ff);
	}
	
	
	
	/*
	 * Method Name:		convertShortToUnsignedShort
	 * Description:		Puts a short value into an int and clears the two high bytes
	 * Parameters:		short signedShort: The short to convert
	 * Return:			The int value
	 */	
	public static int convertShortToUnsignedShort(short signedShort) {
		int unsignedShort = signedShort;
		return (unsignedShort &= (int)0x0000ffff);
	}
}
