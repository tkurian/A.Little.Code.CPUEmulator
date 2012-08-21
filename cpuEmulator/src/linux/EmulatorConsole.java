/*******************************************************************************************
*Source File Name: EmulatorConsole.java

*Programmer's Name: Josh Wagler, Justin Lang, Tina Kurian

*Date: March, 8th, 2012

*Class Description: This class handle the initialization and management of the console user
*interface representing the 6808 CPU emulator.
********************************************************************************************/

package linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import utilities.FileIO;
import utilities.Srec;
import cpu.Freescale6808CPU;
import emulator.Freescale6808Emulator;
import cpu.Freescale6808CPU.CCR_BIT;


/*
 * Class Name:		EmulatorConsole
 * Description:		This class manages the console user interface for the 6808 CPU
 * 					emulator. It displays the CPU registers, CCR flag values, and the memory
 * 					mapping. Functions include loading, resetting, and stepping among others.
 */
public class EmulatorConsole 
{
	private Freescale6808Emulator emulator;
	private Freescale6808CPU cpu;
	private boolean isFileLoaded;
	
	public EmulatorConsole()
	{
		cpu = new Freescale6808CPU();
		emulator = new Freescale6808Emulator();
	}
	
	public void runEmulatorConsole() {
		EmulatorConsole_Run();
	}
	
	
	/*
	 * Method Name: EmulatorConsole_Run
	 * Description: This method handles the commands until the QUIT command is found.
	 * Parameters: No parameters.
	 * Return: No return value.
	 */
	private void EmulatorConsole_Run()
	{
		String option = "";
		Boolean quit = false;
		FileIO srecFile = new FileIO();
		Srec validSrecFile = new Srec();
		List<byte[]> recordList = new ArrayList<byte[]>();
		InputStreamReader inConverter = new InputStreamReader(System.in);
		BufferedReader userInput = new BufferedReader(inConverter);
		short startingAddress = (short)0x0000;
		
		while(!quit) //loop until the quit flag is set
		{
			System.out.print(DisplayMainMenu());
			System.out.print("Enter Command: ");
			
			//get command input from the user
			try
			{
				option = userInput.readLine();
			}
			catch(Exception e)
			{
				System.out.println("Error with console input. 6808 Emulator shutting down.");
				option = "Q";
			}
			
			if(option.startsWith("L ") || option.startsWith("LOAD ")) //handle load command
			{
				String fileName = "";
				int startIndex = 0;
				int endIndex = 0;

				//extract out the file name associated with the load command
				startIndex = option.indexOf(" ");
				endIndex = option.length() - (startIndex - 1);
				fileName = option.substring(startIndex + 1, endIndex);
				
				//verify the file is an .srec file
				if(fileName.endsWith(".srec"))
				{
					String fileContents = srecFile.loadSREC(fileName);
					
					if(fileContents != null)
					{
						recordList = validSrecFile.parseSREC(fileContents);
						
						if(!recordList.isEmpty())
						{
							startingAddress = validSrecFile.getStartingAddress();
							
							try
							{
								emulator.resetEmulator();
								emulator.loadSrecIntoMemory(startingAddress, recordList);
								isFileLoaded = true;
								System.out.println("\n" + "File successfully loaded." + "\n");
							}
							catch (Exception ex) 
							{
								System.out.println("\n" + ex.getMessage() + "\n");
							}			
						}
						else
						{
							System.out.println("\nThe S-Record file contents were not valid. File load failed.\n");
						}
					}
					else
					{
						System.out.println("\nThe S-Record file was not a valid .srec file. File load failed.\n");
					}
					
					cpu = emulator.getCpu();
				}
				else
				{
					//error, file is not an S-Record file
					System.out.println("\nUsage : L or LOAD [srec_filename]\n");
				}
			}
			else if(option.equals("S") || option.equals("STEP")) //handle step command
			{
				if(isFileLoaded)
				{
					//execute single step in the emulator
					try
					{
						emulator.singleStep();
						cpu = emulator.getCpu();
						System.out.println("\nStep was successfully executed.\n");
					}
					catch (Exception ex)
					{
						System.out.println("\n" + ex.getMessage() + "\n");
					}					
				}
				else
				{
					System.out.println("\nA valid s-record file must be loaded before a step can be executed.\n");
				}
			}
			else if(option.equals("R") || option.equals("RESET")) //handle reset command
			{
				if(isFileLoaded)
				{
					//create new CPU and set the emulator CPU to the new one
					try
					{
						emulator.resetEmulator();
						emulator.reloadS1Records();
						cpu = emulator.getCpu();
						System.out.println("\nThe 6808 CPU was reset.\n");
					} 
					catch (Exception ex) 
					{
						System.out.println("\n" + ex.getMessage() + "\n");
					}
				}
				else
				{
					System.out.println("\nA valid s-record file must be loaded before a reset can occur.\n");
				}
			}
			else if(option.startsWith("A ") || option.startsWith("ALTER ")) //handle alter register command
			{
				String register = "";
				String value = "";
				int registerSIndex = 0;
				int registerEIndex = 0;
				int valueSIndex = 0;
				int valueEIndex = 0;
				int valueInt = 0;
				
				try
				{
					//extract out the register value and the number they wish to set that register too
					registerSIndex = option.indexOf(" ");
					valueSIndex = option.indexOf("=");
					registerEIndex = valueSIndex;
					valueEIndex = option.length();
					register = option.substring(registerSIndex + 1, registerEIndex);
					register = register.toLowerCase();
					value = option.substring(valueSIndex + 1, valueEIndex);
					valueInt = Integer.parseInt(value);
					
					
					if((register.equals("pc") && (valueInt >= 0 && valueInt <= 65535))) //if register is program counter and the value is valid
					{
						//get current CPU, set the program counter and return it back to emulator
						cpu = emulator.getCpu();
						cpu.setPC((short)valueInt);
						emulator.setCpu(cpu);
						System.out.println("\nProgram Counter Value Changed to " + valueInt + ".\n");
					}
					else if((register.equals("ac") && (valueInt >= 0 && valueInt <= 255))) //if register is accumulator and the value is valid
					{
						//get current CPU, set the accumulator and return it back to emulator
						cpu = emulator.getCpu();
						cpu.setA((byte)valueInt);
						emulator.setCpu(cpu);
						System.out.println("\nAccumulator Value Changed to " + valueInt + ".\n");
					}
					else //error, incorrect register and/or value
					{
						System.out.println("\nUsage : A or ALTER [register=value]");
						System.out.println("Register (Value) : PC (0 - 65535) or AC (0 - 255)\n");
					}
				}
				catch(Exception ex)
				{
					//error, incorrect register and/or value
					System.out.println("\nUsage : A or ALTER [register=value]");
					System.out.println("Register (Value) : PC (0 - 65535) or AC (0 - 255)\n");
				}
			}
			else if(option.equals("D") || option.equals("DUMP")) //handle dump registers command
			{
				System.out.println(DisplayCPURegisters());
				WaitKeyContinue(userInput);
			}
			else if(option.startsWith("M ") || option.startsWith("MEMORY ")) //handle display memory command
			{
				String memPosition = "";
				int position = 0;
				int startIndex = 0;
				int endIndex = 0;
				
				try
				{
					//parse out the memory address and check to see if it is a valid integer and then attempt to display memory
					startIndex = option.indexOf(" ");
					endIndex = option.length() - (startIndex - 1);
					memPosition = option.substring(startIndex + 1, endIndex);
					position = Integer.parseInt(memPosition);
					System.out.println("\n" + DisplayMemoryMap(position));
				}
				catch(NumberFormatException ex)
				{
					//error, invalid number
					System.out.println("\nUsage : M or MEMORY [address]\n");
				}
				
				WaitKeyContinue(userInput);
			}
			else if(option.equals("Q") || option.equals("QUIT")) //handle quit emulator command
			{
				//set quit flag and display closing message
				quit = true;
				System.out.println("\n6808 Emulator shutting down. Goodbye!\n");
			}
			else //invalid command entered
			{
				System.out.println("\nCommand entered is not supported.\n");
			}
		}
	}
	
	
	/*
	 * Method Name: DisplayMainMenu
	 * Description: This method constructs the string to display the main menu in a readable fashion.
	 * Parameters: No parameters.
	 * Return: Returns a string constructed of the available commands.
	 */
	private String DisplayMainMenu()
	{
		String mainMenu = "";
		
		//build the main menu, displaying the available commands
		mainMenu = "*****6808 Emulator*****\n\n";
		mainMenu += "L or LOAD SRecordFileName\n";
		mainMenu += "S or STEP\n";
		mainMenu += "R or RESET\n";
		mainMenu += "A or ALTER register=value\n";
		mainMenu += "D or DUMP\n";
		mainMenu += "M or MEMORY address\n";
		mainMenu += "Q or QUIT\n\n";
		
		return mainMenu;
	}
	
	
	/*
	 * Method Name: DisplayCPURegisters
	 * Description: This method constructs the string of current CPU register values in a readable fashion.
	 * Parameters: No parameters.
	 * Return: Returns a string constructed of the CPU registers.
	 */
	private String DisplayCPURegisters()
	{
		String registers = "";
		cpu = emulator.getCpu();
		
		//display current CPU register values
		registers = "\n*****6808 CPU Registers*****\n\n";
		registers += String.format("Accumulator: 0x%04X\n", cpu.getA());
		registers += String.format("Program Counter: 0x%04X\n", cpu.getPC());
		registers += String.format("Stack Pointer: 0x%04X\n", cpu.getSP());
		registers += String.format("Index Register Low: 0x%02X\n", cpu.getX());
		registers += String.format("Index Register High: 0x%02X\n\n", cpu.getH());
		
		//display current CCR flag values
		registers += String.format("CCR Value: 0x%02X\n", cpu.getCCR());
		registers += String.format("Carry Flag: %s\n", CCRFlagValue(cpu.isCCRBit(CCR_BIT.CARRY)));
		registers += String.format("Zero Flag: %s\n", CCRFlagValue(cpu.isCCRBit(CCR_BIT.ZERO)));
		registers += String.format("Negative Flag: %s\n", CCRFlagValue(cpu.isCCRBit(CCR_BIT.NEGATIVE)));
		registers += String.format("Interrupt Flag: %s\n", CCRFlagValue(cpu.isCCRBit(CCR_BIT.INTERRUPT)));
		registers += String.format("Half-Carry Flag: %s\n", CCRFlagValue(cpu.isCCRBit(CCR_BIT.HALF)));
		registers += String.format("Overflow Flag: %s\n", CCRFlagValue(cpu.isCCRBit(CCR_BIT.OVERFLOW)));
		
		return registers;
	}
	
	
	/*
	 * Method Name: DisplayMemoryMap
	 * Description: This method displays 256 bytes starting from where the user indicates via command.
	 * Parameters
	 * 	int position = represents where to start in memory
	 * Return: Returns a string constructed of the memory map.
	 */
	private String DisplayMemoryMap(int position)
	{
		String tempMemory = "";
		String memoryMap = "";
		byte cpuMemory[] = emulator.getMemoryArray();
		int x = 0;
		int y = 0;
		
		//confirm that the position is a valid memory address value
		if(position >= 0 && position <= 65535)
		{
			//loop until 256 bytes have been constructed or the value exceeds 65535
			for(x = position;(x < position + 256) && (x <= 65535);x += 10)
			{
				tempMemory = String.format("%04X  :  ", x);
				
				//loop until 10 bytes have been printed (10 addresses per line)
				for(y = 0; y < 10; y++)
				{
					//confirm value is within the 256 address range and it is less than or equal to address 65535
					if((x + y) < position + 256 && (x + y) <= 65535)
					{
						tempMemory += String.format("%02X  ", cpuMemory[x + y]);
					}
				}
				
				//build up memory map to display on the console
				memoryMap += tempMemory + "\n";
			}
		}
		else
		{
			//usage error due to invalid position value
			memoryMap = "\nUsage : M or MEMORY [address]\n";
		}
		
		return memoryMap;
	}


	/*
	 * Method Name: CCRFlagValue
	 * Description: This method checks the CCR flag values to indicate either 0 or 1.
	 * Parameters
	 * 	Boolean flagValue = represents the flag value to check for true or false
	 * Return: Returns a string indicating either 0x00 or 0x01.
	 */
	private String CCRFlagValue(Boolean flagValue)
	{
		String returnValue = "";
		
		//if flag value is set, return 1 otherwise 0
		if(flagValue)
		{
			returnValue = "0x01";
		}
		else
		{
			returnValue = "0x00";
		}
		
		return returnValue;
	}

	
	/*
	 * Method Name: WaitKeyContinue
	 * Description: This method waits for the user to press Enter before continuing.
	 * Parameters
	 * 	BufferedReader reader = represents stream to get user input
	 * Return: No return value.
	 */
	private void WaitKeyContinue(BufferedReader reader)
	{
		try
		{
			//wait for the Enter key from the user before continuing
			System.out.print("Press \"Enter\" key to continue...");
			reader.readLine();
			System.out.println();
		}
		catch(Exception ex)
		{
			System.out.print("Error while waiting for \"Enter\" key. Program will continue to run.");
		}
	}
}