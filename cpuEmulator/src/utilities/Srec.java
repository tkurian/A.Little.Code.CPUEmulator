
/*
 * Filename:		Srec.java
 * Package:			utilities
 * Project:			padA2
 * By:				Justin Lang, Josh Wagler & Tina Kurian
 * Date:			Wednesday, March 7, 2012
 * Description:		Contains the Srec class
 */






package utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;





/*
 * Class Name:		Srec
 * Description:		Contains Srec parsing as well as validation. Returns the parsed and validated s1 records within a 
 * 					list of byte arrays.  
 * 					
 * 					Contains public methods which will either validate or parse the Srec file contents. Validation will occur
 * 					once a file has been loaded into a string. the parse method will be called after the validation method 
 * 					is invoked. 
 * 
 * 					Private methods handle validating that the srecord contains a valid count and checksum. Private methods
 * 					also handle creating a valid list of byte arrays holding only the address and data of the S1Records. 
 */
public class Srec 
{
	
	private List<Byte> data = new ArrayList<Byte>();
	private List<byte[]> bytearrays = new ArrayList<byte[]>();
	
	private short startingAddress;
	private final int S0 = -16; 
	private final int S1 = -15; 
	private final int S5 = -11; 
	private final int S9 = -7; 
	
	
	
	
	
	
	/*====================================================================== */
	/*========================= PARSING OF SRECODS ========================= */
	/*====================================================================== */
	
	
	
	public short getStartingAddress() {
		return startingAddress;
	}






	/*
	 * Method Name:		parseSREC
	 * Description:		Parses the SRecord file which is in the form of a string
	 * Parameters:		A list of S1 records within a string
	 * Return:			A list of S1 records as byte arrays if validation returned true, else
	 * 					an empty list of byte arrays. 
	 */	
	public List<byte[]> parseSREC(String srec)
	{
		System.out.println(srec); 
		boolean isSrecValid = false; 
		byte[] byteArray = null; 
		Byte[] arraysMinusLast = null; 
		Byte[] lastArray = null; 
		
		
		// check if SRecord is valid
		isSrecValid = srecValidation(srec); 
		
		if(isSrecValid)
		{
			//converting to byte array...
			for (int i = 0; i < srec.length(); i += 2) 
			{
				//grab byte interpretation of hex values (2 characters in SRecord make up a byte)
			    byte b = (byte) ((Character.digit(srec.charAt(i), 16) << 4) + Character.digit(srec.charAt(i + 1), 16));
			    
			    
			    // if the byte value received is an S1, S5 or S9, create a new byte array
			    if (b == S0 || b == S1 ||  b == S5 || b == S9) 
			    {
			    	// move index ahead 2 values so that the SRecord type is not copied into new array
			    	i = i+2; 
			    	
			    	//gets all but the last SRecord
			    	arraysMinusLast = data.toArray(new Byte[data.size()]); 
			    	byteArray = convertByte(arraysMinusLast); 
			    	if(byteArray.length != 0)
			    	{
			    		bytearrays.add(byteArray);
			    	}
			    	
			        data.clear();
			    }
			    
			    // if the byte is not S1, S5, or S11, and not the last byte in the SRecord copy the value into data
			    if(b!= -15 && i + 2 < srec.length() && (Character.digit(srec.charAt(i + 2), 16) << 4) + Character.digit(srec.charAt(i + 3), 16) != S0
			    		&& (Character.digit(srec.charAt(i + 2), 16) << 4) + Character.digit(srec.charAt(i + 3), 16) != S1
			    		&& (Character.digit(srec.charAt(i + 2), 16) << 4) + Character.digit(srec.charAt(i + 3), 16) != S5
			    		&& (Character.digit(srec.charAt(i + 2), 16) << 4) + Character.digit(srec.charAt(i + 3), 16) != S9)
			    {
			    	data.add(b);
			    }
			}
			
			// process last array in the SRecord
			lastArray = data.toArray(new Byte[data.size()]); 
			byteArray = convertByte(lastArray); 
	    	bytearrays.add(byteArray); 
			
		}
		// delete all records which we don't care about
		deleteOtherRecords(); 
		return bytearrays;
	}
	
	
	
	
	
	
	
	
	
	
	/*
	 * Method Name:		deleteOtherRecords
	 * Description:		Deletes every record which is empty or not an S1
	 * Parameters:		void
	 * Return:			void
	 */	
	private void deleteOtherRecords()
	{ 
		final Iterator<byte[]> iter = bytearrays.iterator();
		
		while (iter.hasNext()) 
		{
		    final byte[] temp = iter.next();
		    
		    if (temp.length != 0)
		    {
			    //remove all the SRecords we don't care about. Also, remove empty records
			    if (temp[0] == S9) 
			    {
			    	
			    	startingAddress = (short) temp[1];
			    	startingAddress = (short)((startingAddress << 8) | temp[2]);

			        iter.remove();
			    }
			    if(temp[0] == S5)
			    {
			    	iter.remove(); 
			    }
			    if(temp[0] == S0)
			    {
			    	iter.remove(); 
			    }
			    if(temp.length == 0)
			    {
			    	iter.remove(); 
			    }
		    }
		}
		
		
		
	}
	
	
	
	
	
	
	
	
	/*
	 * Method Name:		convertByte
	 * Description:		Takes a Byte[] and returns a byte[] 
	 * Parameters:		An array of Bytes
	 * Return:			An array of bytes
	 */	
	private byte[] convertByte(Byte[] bytes)
	{
		byte[] byteArray = new byte[bytes.length];
    	int index = 0;
    	

    	// for every byte within the byte array place it into the byte[]
    	for (byte c : bytes) 
    	{
    	    byteArray[index++] = c;
    	}
    	
    	
    	return byteArray; 
	}
	
	
	
	
	
	
	
	
	
	
	
	/*====================================================================== */
	/*====================== VALIDATION OF SRECODS ========================= */
	/*====================================================================== */
	
	
	
	/*
	 * Method Name:		srecValidation
	 * Description:		Determines if all the records within the SRecord are valid
	 * Parameters:		A list of S1 records as a String
	 * Return:			True if valid SRecord, false otherwise.
	 */	
	public Boolean srecValidation(String srec)
	{
		boolean isSrecValid = false; 
		String[] data = new String[srec.length()]; 
		srec = srec.replaceAll("(\\r|\\n)", "");
		
		// splitting the string on every new SRecord
		data = srec.split("S"); 
		
		for(int z = 1; z < data.length; z++)
		{
			// if the first character in the array is 0, 1, 5, 9, it is an SRecord and we want to parse it, else, SRecord is invalid
			if(data[z].charAt(0) == '0' || data[z].charAt(0) == '1' || data[z].charAt(0) == '5' || data[z].charAt(0) == '9')
			{
				isSrecValid = checksum(data,z); 
				if (isSrecValid ==false)
				{
					break; 
				}
			}
			else
			{
				isSrecValid = false; 
				break; 
			}
		}
		return isSrecValid; 
	}
	
	
	
	
	
	/*
	 * Method Name:		checksum
	 * Description:		Checks if the SRecord adds up to the checksum indicated
	 * Parameters:		An S1 record as a String arrays, index of SRecord
	 * Return:			True if checksum is correct, false otherwise.
	 */	
	private boolean checksum(String[] data, int z)
	{
		int len = data[z].length() -2; 
		int bytes = 0; 
		boolean isValidChecksum = false; 
		boolean isCountValid = false; 
		StringBuilder binaryChecksum = new StringBuilder();
		
		
		// checking to see if all the data in the record is there
		isCountValid = validateLength(data, z); 
		
		
		if(isCountValid == true)
		{
			// getting the hex checksum from srecord
			String hexCheckSum =  Character.toString(data[z].charAt(len)) + Character.toString(data[z].charAt(len + 1)); ; 
			
			 
			// getting all byte values to add together in order to check against checksum   
			for(int i = 1; i < len; i+=2)
			{
				try
				{
					// get two characters which are the hex representation of a byte
					String b = Character.toString(data[z].charAt(i)) + Character.toString(data[z].charAt(i + 1)); 
					
					//get the hex value as a byte
					bytes += Integer.parseInt(b, 16); 
				}
				catch(Exception e)
				{
					
				}
			}
			// convert bytes received into binary... perform ones compliment along the way!!!!
			 for (int i = 0; i < 8; i++)
		     {
				 binaryChecksum.append((bytes & 128) == 0 ? 1 : 0);
			        bytes <<= 1;
		     }
			 
			 // convert binary value into hex 
			 String totalHex = (Integer.toHexString(Integer.parseInt(binaryChecksum.toString(), 2)).toUpperCase()); 
			 
		
			 // check if checksum and one's compliment hex value are equal 
			 if(totalHex.equals(hexCheckSum.toUpperCase()))
			 {
				 isValidChecksum = true; 
			 }
		}
		printContents(); 
		return isValidChecksum; 
	}
	
	
	
	
	
	
	
	
	/*
	 * Method Name:		validateLength
	 * Description:		Checks if the SRecords count is correct
	 * Parameters:		An S1 records as a String arrays, index of SRecord
	 * Return:			True if count is valid, false otherwise
	 */	
		private boolean validateLength(String[] data, int z)
		{
			
			String b = null; 
			String count = null; 
			int value = 0; 
			boolean isValidLength = false; 
			
			
			//gets the two characters which together represent the hex value of the SRecord's count
			b = Character.toString(data[z].charAt(1)) + Character.toString(data[z].charAt(2));
			
			//method returns the actual count of the S1Record
			count = getCount(data, z); 
			int i = java.lang.Integer.parseInt(count);
			String hexstr = Integer.toString(i, 16);
			String hex = hexstr.toUpperCase(); 
			
			if(b.charAt(0) == '0')
			{
				b.replace('0', '-'); 
				b = hex.replaceAll("-", "");
			}
			//compare actual count with indicated count
			if(b.equals(hex))
			{
				isValidLength = true; 
			}

			return isValidLength; 
		}
	
		
		








		/*
		 * Method Name:		getCount
		 * Description:		Counts the number of bytes in an SRecord and pads the value
		 * Parameters:		An S1 records as a String arrays, index of SRecord
		 * Return:			Count as a string - padded if it's a single digit
		 */	
		private String getCount(String[] data, int z)
		{
			int counter = 1; 
			String unpaddedCounter = null; 
			String paddedCounter = null; 
			

			for (int i = 5; i < data[z].length(); i += 2) 
			{
				 counter++; 
			}
			
			unpaddedCounter = Integer.toString(counter); 
			paddedCounter = String.format("%02d", Integer.parseInt(unpaddedCounter));

		
			return paddedCounter; 
		}
	
	
	
		
		
		
		
		

	

	/*
	 * Method Name:		printContents
	 * Description:		Loops through S1Records within the list of byte arrays
	 * Parameters:		void
	 * Return:			void
	 */	
	private void printContents()
	{	

		final Iterator<byte[]> iter = bytearrays.iterator();

		while (iter.hasNext()) 
		{
		    final byte[] temp = iter.next();
		    //print the contents formatted in an array style
		    System.out.println(java.util.Arrays.toString(temp)); 
		}
	}
	
}
