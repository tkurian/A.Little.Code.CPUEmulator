
/*
 * Filename:		FileIO.java
 * Package:			utilities
 * Project:			padA2
 * By:				Justin Lang, Josh Wagler & Tina Kurian
 * Date:			Wednesday, March 7, 2012
 * Description:		Contains the FileIO class
 */






package utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;




/*
 * Class Name:		FileIO
 * Description:		Contains the method for opening and reading in a file.  
 * 					
 * 					In the case that the file extension does not contain a .srec, no file is opened. 
 * 					This method will also throw an exception if an error occurs while attempting to open, read or
 * 					close the file indicated. 
 */
public class FileIO 
{

	private final String srec = ".srec"; 
	
	
	
	
	
	
	/*
	 * Method Name:		loadSREC
	 * Description:		Loads the SRecord file into a string
	 * Parameters:		String containing the path of the SRecord
	 * Return:			String containing the contents within the SRecord file
	 */	
	public String loadSREC(String filename) 
	{
		boolean isSrec = false; 
		FileInputStream stream = null;
		MappedByteBuffer bytebuffer = null;
		String file = null;
		
		
		
		// load srec file into memory
		isSrec = filename.indexOf(srec) != -1; 
		
		if(isSrec == true)
		{
			
			try 
			{
				stream = new FileInputStream(new File(filename));
				FileChannel filechannel = stream.getChannel();
				bytebuffer = filechannel.map(FileChannel.MapMode.READ_ONLY, 0, filechannel.size());
				file = Charset.defaultCharset().decode(bytebuffer).toString();
				
			} 
			catch (FileNotFoundException e) 
			{
				//handle FileNotFoundException
			} 
			catch (IOException e) 
			{
				//handle IOException
			}
			finally 
			{
			    try 
			    {
					stream.close();
				} 
			    catch (Exception e) 
			    {
			    	//Handle exception error closing file stream
				}
			}
		}
		return file;
	}
	
	
	
}
