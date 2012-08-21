/*******************************************************************************************
*Source File Name: UIManager.java

*Programmer's Name: Josh Wagler, Justin Lang, Tina Kurian

*Date: March, 8th, 2012

*Class Description: This class manages the user interface by either launching the console or
*graphical user interface based on the current OS being used.
********************************************************************************************/

package management;

import windows.EmulatorGUI;
import linux.EmulatorConsole;


/*
 * Class Name:		EmulatorConsole
 * Description:		This class launches either the console or GUI based on the OS.
 */
public class UIManager 
{
	public static void main(String[] args) 
	{
		String whichOS = "";
		
		//gets the OS property name
		whichOS = System.getProperty("os.name").toLowerCase();
		
		try
		{
			//if Windows, launch GUI
			if(whichOS.startsWith("win"))
			{
				EmulatorGUI frame = new EmulatorGUI();
				frame.setVisible(true);
			}
			else //else, launch Console (Linux)
			{
				EmulatorConsole console = new EmulatorConsole();
				console.runEmulatorConsole();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
