
/*
 * Filename:		Freescale6808Emulator.java
 * Package:			emulator
 * Project:			padA2
 * By:				Justin Lang, Josh Wagler & Tina Kurian
 * Date:			Wednesday, March 7, 2012
 * Description:		Contains the Freescale6808Emulator class
 */



package emulator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import utilities.UnsignedData;
import cpu.Freescale6808CPU;
import cpu.Freescale6808CPU.CCR_BIT;


/*
 * Class Name:		Freescale6808Emulator
 * Description:		Defines the 6808 emulator object.  Contains a 6808 CPU object, a byte array representing
 * 					the microcontroller memory.  
 * 					
 * 					Contains public methods to load the virtual memory with SREC data, and "single step" the emulator, 
 * 					which causes the emulator to process an instruction in memory based on the current CPU state 
 * 					and current memory values.  Other methods allow one or two bytes of memory to be accessed, and the entire 
 * 					CPU object may be retrieved or set at any time.
 * 
 * 					Private methods handle interpreting instruction opcodes and updating the CPU state.
 */
public class Freescale6808Emulator { 
	
	private Freescale6808CPU cpu;	
	private short originalPC;
	private List<byte[]> s1Records;

	private byte[] memory;
	public final static int MAX_ADDRESS_SPACE = 65536;	
	
	private byte[] map;
	private final static byte NOTHING_ATTACHED = 0;
	private final static byte RAM_MEMORY = 1;
	private final static byte ROM_MEMORY = 2;
	private final static byte HARDWARE_ACCESS = 3;
	
	private final static short START_OF_HARDWARE = (short)0x0000;
	private final static short END_OF_HARDWARE = (short)0x007F;
	private final static short START_OF_RAM = (short)0x0080;
	private final static short END_OF_RAM = (short)0x107F;
	private final static short START_OF_ROM = (short)0x1080;
	private final static short END_OF_ROM = (short)0xFFFF;
	
	private final static short PORT_A_DATA = (short)0x0000;
	private final static short PORT_A_PULLUPS = (short)0x0001;
	private final static short PORT_A_DATA_DIRECTION = (short)0x0003;
	
	public enum SWITCH_BIT {
		SWITCH_ONE,
		SWITCH_TWO,
		SWITCH_THREE,
		SWITCH_FOUR
	};
	
	private final static byte switchOneMask = (byte)0x10;
	private final static byte switchTwoMask = (byte)0x20;
	private final static byte switchThreeMask = (byte)0x40;
	private final static byte switchFourMask = (byte)0x80;
	
	
	
	/*
	 * Method Name:		Freescale6808Emulator
	 * Description:		Default constructor
	 * Parameters:		None
	 * Return:			None
	 */	
	public Freescale6808Emulator() {
		
		this.cpu = new Freescale6808CPU();
		originalPC = (short)0x0000;
		s1Records = new ArrayList<byte[]>();
		
		memory = new byte[MAX_ADDRESS_SPACE];
		map = new byte[MAX_ADDRESS_SPACE];
		
		// init memory
		for (int i = 0; i < MAX_ADDRESS_SPACE; i++) {
			memory[i] = 0;
			map[i] = 0;
		}
		
		// init map
		for (short i = START_OF_HARDWARE; i <= END_OF_HARDWARE; i++) {
			map[UnsignedData.convertShortToUnsignedShort(i)] = HARDWARE_ACCESS;
		}
		
		for (short i = START_OF_RAM; i <= END_OF_RAM; i++) {
			map[UnsignedData.convertShortToUnsignedShort(i)] = RAM_MEMORY;
		}
		
		for (short i = START_OF_ROM; i <= END_OF_ROM; i++) {
			map[UnsignedData.convertShortToUnsignedShort(i)] = ROM_MEMORY;
		}
	}
	


	/*
	 * Method Name:		loadSrecIntoMemory
	 * Description:		Loads S1 records into the virtual memory
	 * Parameters:		short pcAddress: The address retrieved from the S9 record
	 * 					List<byte[]> s1Records: A list of S1 records as byte arrays
	 * Return:			True if successfully loaded, false otherwise   
	 */	
	public boolean loadSrecIntoMemory(short pcAddress, List<byte[]> s1Records) throws Exception {
		boolean success = true;
		int s1RecordAddress = UnsignedData.convertShortToUnsignedShort(pcAddress);
		
		cpu.setPC(pcAddress);	
		originalPC = pcAddress;
		this.s1Records = s1Records;
		
		for (byte[] s1Record : s1Records) {
			
			for (int i = 0; i < s1Record.length; i++) {
				
				if (i == 0) {
					s1RecordAddress = (short)s1Record[i];
				}
				else if (i == 1) {
					s1RecordAddress = (short)((s1RecordAddress << 8) | s1Record[i]);
				}
				else {
					
					if (s1RecordAddress >= MAX_ADDRESS_SPACE) {
						throw new Exception("Memory out of bounds!");
					}
					else {
						memory[s1RecordAddress] = s1Record[i];
						s1RecordAddress++;
					}
				}				
			}
		}			
		
		return success;
	}
	
	
	
	/*
	 * Method Name:		singleStep
	 * Description:		Fetches an instruction from memory and processes it
	 * Parameters:		None
	 * Return:			None
	 */	
	public void singleStep() throws Exception {		
		
		//fetch
		byte instruction = fetchInstruction();		
		
		//decode & execute
		try {
			decodeInstruction(instruction);
		}
		catch (Exception ex) {
			throw ex;
		}
		
	}	

	
	
	/*
	 * Method Name:		fetchInstruction
	 * Description:		Gets an instruction from virtual memory and increments the PC
	 * Parameters:		None
	 * Return:			The instruction opcode from virtual memory
	 */	
	private byte fetchInstruction() {
		
		int pc = UnsignedData.convertShortToUnsignedShort(cpu.getPC());
		byte instruction = memory[pc];		
		incrementProgramCounter();
		
		return instruction;
	}
	
	
	
	/*
	 * Method Name:		decodeInstruction
	 * Description:		Given an instruction opcode, determines the addressing mode and the instruction. 
	 * 					Increments the PC and then executes the instruction with the data
	 * Parameters:		byte instruction: The instruction opcode from virtual memory
	 * Return:			None
	 */	
	private void decodeInstruction(byte instruction) throws Exception {			
		
		
		// make sure the instruction is in range...
		if (isValidAddressingMode(instruction)) {				
			
			int pc = UnsignedData.convertShortToUnsignedShort(cpu.getPC());
			
			// decode instruction
			
			// addressing mode is the high nibble
			byte mode = (byte)(instruction & (byte)0xf0);
			// instruction is the low nibble
			instruction &= (byte)0x0f;
			
			// store data and address depending on addressing mode
			byte data = 0;
			byte address = 0;
			
			// addressing mode - where do we get the data?
			switch (mode) {
			
				// immediate
				case (byte)0xa0:
					data = memory[pc];
					incrementProgramCounter();
					break;					
				// direct (8 bit)
				case (byte)0xb0:
					address = memory[pc];
					incrementProgramCounter();
					data = readByte(UnsignedData.convertByteToUnsignedByte(address));
					break;				
				default:
					throw new Exception("Invalid addressing mode!");
			}
			
			try {
				executeInstruction(instruction, data, address);	
			}
			catch (Exception ex) {
				throw ex;
			}
					
		}
		else {
			
			try {
				executeBranchInstruction(instruction);
			}
			catch (Exception ex) {
				throw ex;
			}
		}
	}
	
	
	
	/*
	 * Method Name:		executeInstruction
	 * Description:		Given the instruction, and the data and address retrieved from memory, executes the instruction
	 * Parameters:		byte instruction: The instruction opcode as a byte
	 *  				byte data: The data as a byte.  Pass in 0 if data is not available
	 *  				byte address: The address as a byte.  Pass in zero if an address is not available
	 * Return:			None
	 */	
	private void executeInstruction(byte instruction, byte data, byte address ) throws Exception {
		// execute instruction with the data
		switch (instruction) {
		
			// ADD
			case (byte)0x0b:					
				executeADD(data);					
				break;					
			// AND
			case (byte)0x04:
				executeAND(data);					
				break;				
			// LDA
			case (byte)0x06:
				executeLDA(data);					
				break;					
			// STA
			case (byte)0x07:
				executeSTA(address);
				break;					
			default:
				throw new Exception("Invalid opcode!");
		}
	}
	
	
	
	/*
	 * Method Name:		executeBranchInstruction
	 * Description:		Executes branch instructions 
	 * Parameters:		byte instruction: The instruction opcode as a byte
	 * Return:			None
	 */	
	private void executeBranchInstruction(byte instruction) throws Exception {
		
		int pc = UnsignedData.convertShortToUnsignedShort(cpu.getPC());
		
		// offset used for branching instructions
		byte offset = 0;
		
		// execute a branch instruction
		switch (instruction) { 
		
		// BRA 
		case (byte)0x20:
			offset = memory[pc];
			incrementProgramCounter();
			executeBRA(offset);
			break;
		
		// BEQ (zero flag high)
		case (byte)0x27:
			offset = memory[pc];
			incrementProgramCounter();
			executeBEQ(offset);
			break;
		
		default:
			throw new Exception("Invalid opcode!");
		}
	}
	
	
	
	/*
	 * Method Name:		executeSTA
	 * Description:		Given an address of virtual memory, stores the accumulator value at that address
	 * Parameters:		byte address: The address at which to store the accumulator
	 * Return:			None
	 */
	private void executeSTA(byte address) {
		// execute instruction
		if ((short)address < MAX_ADDRESS_SPACE) { // TODO how should emulator handle attempt to access outside of memory?
			writeByte(cpu.getA(), UnsignedData.convertByteToUnsignedByte(address));
			
			// manage ccr
			
			// clear overflow
			cpu.setCCRBit(false, CCR_BIT.OVERFLOW);
			
			// negative is set IF most significant bit of accumulator is 1, else clear
			if ((cpu.getA() & (byte)0x80) == (byte)0x80) {
				cpu.setCCRBit(true, CCR_BIT.NEGATIVE);
			}
			else {
				cpu.setCCRBit(false, CCR_BIT.NEGATIVE);
			}
			
			// zero is set IF accumulator is $00, else clear
			if (cpu.getA() == (byte)0x00) {
				cpu.setCCRBit(true, CCR_BIT.ZERO);
			}
			else {
				cpu.setCCRBit(false, CCR_BIT.ZERO);
			}
		}		
	}

	
	
	/*
	 * Method Name:		executeLDA
	 * Description:		Given data as a byte, stores it in the accumulator
	 * Parameters:		byte data: Some data as a byte
	 * Return:			None
	 */
	private void executeLDA(byte data) {
		// execute instruction
		cpu.setA(data);
		
		// manage ccr
		
		// clear overflow
		cpu.setCCRBit(false, CCR_BIT.OVERFLOW);
		
		// negative is set IF most significant bit of accumulator is 1, else clear
		if ((cpu.getA() & (byte)0x80) == (byte)0x80) {
			cpu.setCCRBit(true, CCR_BIT.NEGATIVE);
		}
		else {
			cpu.setCCRBit(false, CCR_BIT.NEGATIVE);
		}
		
		// zero is set IF accumulator is $00, else clear
		if (cpu.getA() == (byte)0x00) {
			cpu.setCCRBit(true, CCR_BIT.ZERO);
		}
		else {
			cpu.setCCRBit(false, CCR_BIT.ZERO);
		}
	}

	
	
	/*
	 * Method Name:		executeAND
	 * Description:		Given some data, perform a bitwise AND operation with the value in the accumulator
	 * 					and store the value in the accumulator
	 * Parameters:		byte data: Some data as a byte
	 * Return:			None
	 */
	private void executeAND(byte data) {
		
		// execute instruction
		byte a = cpu.getA();
		byte result = (byte)(a & data);
		cpu.setA(result);	
		
		// manage ccr
		
		// clear overflow
		cpu.setCCRBit(false, CCR_BIT.OVERFLOW);
		
		// negative is set IF most significant bit of accumulator is 1, else clear
		if ((cpu.getA() & (byte)0x80) == (byte)0x80) {
			cpu.setCCRBit(true, CCR_BIT.NEGATIVE);
		}
		else {
			cpu.setCCRBit(false, CCR_BIT.NEGATIVE);
		}
		
		// zero is set IF accumulator is $00, else clear
		if (cpu.getA() == (byte)0x00) {
			cpu.setCCRBit(true, CCR_BIT.ZERO);
		}
		else {
			cpu.setCCRBit(false, CCR_BIT.ZERO);
		}
	}

	
	
	/*
	 * Method Name:		executeADD
	 * Description:		Given some data, adds the data and the accumulator and stores the result in the
	 * 					accumulator
	 * Parameters:		byte data: Some data as a byte
	 * Return:			None
	 */
	private void executeADD(byte data) {
		byte a = cpu.getA();
		byte result = (byte)(a + data); //TODO does this work properly??
		cpu.setA(result);
		
		// manage ccr
		
		// overflow is set if two's complement overflow occurred, else clear
		// check if result MSB is high and both operand MSB were low
		if (((result & (byte)0x80) == (byte)0x80) && ((a & (byte)0x80) == (byte)0x00) && ((data & (byte)0x80) == (byte)0x00)) {
			cpu.setCCRBit(true, CCR_BIT.OVERFLOW);
		}
		// check if result MSB is low and both operand MSB were high
		else if (((result & (byte)0x80) == (byte)0x00) && ((a & (byte)0x80) == (byte)0x80) && ((data & (byte)0x80) == (byte)0x80)) {
			cpu.setCCRBit(true, CCR_BIT.OVERFLOW);
		}
		// otherwise clear
		else {
			cpu.setCCRBit(false, CCR_BIT.OVERFLOW);
		}
		
		// TODO fix
		// half carry is set if there was a carry from bit 3, else clear
		if ((((byte)(a & (byte)0x0f) + (data & (byte)0x0f)) & (byte)0x10) == (byte)0x10) {
			cpu.setCCRBit(true, CCR_BIT.HALF);
		}
		else {
			cpu.setCCRBit(false, CCR_BIT.HALF);
		}		
		
		// negative is set IF most significant bit of accumulator is 1, else clear
		if ((cpu.getA() & (byte)0x80) == (byte)0x80) {
			cpu.setCCRBit(true, CCR_BIT.NEGATIVE);
		}
		else {
			cpu.setCCRBit(false, CCR_BIT.NEGATIVE);
		}
		
		// zero is set IF accumulator is $00, else clear
		if (cpu.getA() == (byte)0x00) {
			cpu.setCCRBit(true, CCR_BIT.ZERO);
		}
		else {
			cpu.setCCRBit(false, CCR_BIT.ZERO);
		}
		
		// carry bit is set if there was a carry from most significant bit of result, else clear
		short accumulatorOverflow = UnsignedData.convertByteToUnsignedByte(a);
		short dataOverFlow = UnsignedData.convertByteToUnsignedByte(data);
		
		if ((short)(accumulatorOverflow + dataOverFlow) > (short)0x00ff) {
			cpu.setCCRBit(true, CCR_BIT.CARRY);
		}
		else {
			cpu.setCCRBit(false, CCR_BIT.CARRY);
		}
	}
	
	
	
	/*
	 * Method Name:		executeBEQ
	 * Description:		If the CCR zero flag is set, do a BRA
	 * Parameters:		byte offset: The offset to move the program counter
	 * Return:			None
	 */
	private void executeBEQ(byte offset) {		
		// only branch if zero flag set
		if (cpu.isCCRBit(CCR_BIT.ZERO)) {
			executeBRA(offset);			
		}
	}

	
	
	/*
	 * Method Name:		executeBRA
	 * Description:		Moves the program counter based on the given offset.  If the offset's
	 * 					signed bit is high, the offset is a negative value
	 * Parameters:		byte offset: The offset to move the program counter
	 * Return:			None
	 */		
	private void executeBRA(byte offset) {
		
		short pc = cpu.getPC();
		
		// check for signed bit in offset
		// signed bit high, take two's complement and branch back (subtract  offset)
		if ((offset & (byte)0x80) == (byte)0x80) {
			short trueOffset = (short)(~offset + (short)0x0001);
			trueOffset &= (short)0x00ff;
			
			// check for out of memory bounds
			if (pc - trueOffset < 0) {
				short newPC = (short)(MAX_ADDRESS_SPACE - 1 - (trueOffset - pc));
				cpu.setPC(newPC);
			}
			else {
				short newPC = (short)(pc - trueOffset);
				cpu.setPC(newPC);
			}
			
		}
		// signed bit low, branch forward (add offset)
		else {			
			short trueOffset = offset;
			trueOffset &= (short)0x00ff;
			
			// check for out of memory bounds
			if (pc + trueOffset >= MAX_ADDRESS_SPACE) {
				short newPC = (short)(trueOffset - (MAX_ADDRESS_SPACE - 1 - pc));
				cpu.setPC(newPC);
			}
			else {
				short newPC = (short)(pc + trueOffset);
				cpu.setPC(newPC);
			}
		}
	}
	
	
	
	/*
	 * Method Name:		readByte
	 * Description:		Reads a byte from virtual memory, checking the memory map to see if the address
	 * 					is a readable location
	 * Parameters:		int address: The address in virtual memory to read a byte
	 * Return:			The byte that was read from memory
	 */	
	private byte readByte(int address) {
		byte value = (byte)0x00;
		
		if (address >= 0 && address < MAX_ADDRESS_SPACE) {
			
			if (map[address] == HARDWARE_ACCESS) {
				
				switch (address) {
				
				case PORT_A_DATA:
					// TODO make specific bits readable based on data direction value?
					// allow reads if data direction is set to input (0x00)
					if (memory[UnsignedData.convertShortToUnsignedShort(PORT_A_DATA_DIRECTION)] == (byte)0x00) {
						value = memory[address];
					}
					else {
						// give error?
					}
					break;
					
				case PORT_A_DATA_DIRECTION:
					value = memory[address];
					break;
					
				default:
					value = memory[address];
					break;
				}
			}
			
			else if (map[address] == RAM_MEMORY) {
				value = memory[address];
			}
			
			else if (map[address] == ROM_MEMORY) {
				value = memory[address];
			}
			
			else if (map[address] == NOTHING_ATTACHED) {
				// give error?
			}
		}
		
		return value;
	}
	
	
	
	/*
	 * Method Name:		writeByte
	 * Description:		Write a byte to virtual memory, checking the memory map to see
	 * 					if the address is a writable location
	 * Parameters:		byte value: The value to write to memory
	 * 					int address: The address in virtual memory to write the byte value
	 * Return:
	 */	
	private void writeByte(byte value, int address) {
		
		if (address >= 0 && address < MAX_ADDRESS_SPACE) {
			if (map[address] == HARDWARE_ACCESS) {
				
				switch (address) {
				
				case PORT_A_DATA:
					// give error
					break;
					
				default:
					memory[address] = value;
					break;
				}
			}
			
			else if (map[address] == RAM_MEMORY) {
				memory[address] = value;
			}
			
			else if (map[address] == ROM_MEMORY) {
				memory[address] = value;
			}
			
			else if (map[address] == NOTHING_ATTACHED) {
				// give error
			}
		}
	}
	
	
	
	/*
	 * Method Name:		incrementProgramCounter
	 * Description:		Increments the PC by one byte
	 * Parameters:		None
	 * Return:			None
	 */
	private void incrementProgramCounter() {
		
		short pc = cpu.getPC();
		
		pc++;		
		if (pc >= MAX_ADDRESS_SPACE) {
			pc = 0;
		}		
		
		cpu.setPC(pc);
	}

	
	
	/*
	 * Method Name:		getMemoryByte
	 * Description:		Get a byte from virtual memory at the specified address
	 * Parameters:		int index: The index at which to retrieve a byte
	 * Return:			The byte from the index, otherwise 0 if the index was invalid
	 */
	public byte getMemoryByte(int index) {
		
		byte value = 0;		
		
		if (isValidMemoryIndex(index)) {
			value = memory[index];
		}
		
		return value;
	}	

	
	
	/*
	 * Method Name:		setMemory
	 * Description:		Set a byte in virtual memory at the specified index
	 * Parameters:		int index: The index at which to set a byte
	 * 					byte value: The byte value to set
	 * Return:			True if succesful, false otherwise
	 */
	public boolean setMemory(int index, byte value) {
		
		boolean set = false;
		
		if (isValidMemoryIndex(index)) {
			memory[index] = value;
			set = true;
		}
		
		return set;
	}
	
	
	
	/*
	 * Method Name:		getMemoryShort
	 * Description:		Get a short from virtual memory at the specified address
	 * Parameters:		int index: The index at which to retrieve a short
	 * Return:			The short from the index, otherwise 0 if the index was invalid
	 */
	public short getMemoryShort(int index) {
		
		short value = 0;
		
		// 6808 is big endian - the first byte found in memory is the high byte
		if (isValidMemoryIndex(index)) {
			value = memory[index];
			value = (short)(value << 8);
			value |= memory[index + 1];
		}	
		
		return value;
	}

	
	
	/*
	 * Method Name:		setMemory
	 * Description:		Set a short in virtual memory at the specified index
	 * Parameters:		int index: The index at which to set a short
	 * 					short value: The short value to set
	 * Return:			True if successful, false otherwise
	 */
	public boolean setMemory(int index, short value) {
		
		boolean set = false;
		
		if (isValidMemoryIndex(index)) {			
			// 6808 is big endian - store the high byte first
			memory[index] = (byte)(value >> 8);
			memory[index + 1] = (byte)(value & (short)0x00ff);
			set = true;
		}	
		
		return set;
	}
	
	
	
	/*
	 * Method Name:		getMemoryArray
	 * Description:		Gets the memory array
	 * Parameters:		None
	 * Return:			The virtual memory as byte array
	 */
	public byte[] getMemoryArray() {
		return memory;
	}
	
	
	
	/*
	 * Method Name:		setMemory
	 * Description:		Sets a new byte array as virtual memory
	 * Parameters:		byte[] memory: A new byte array representing virtual memory,
	 * 						must be the size of MAX_ADDRESS_SPACE
	 * Return:			True if successful, false otherwise
	 */
	public boolean setMemory(byte[] memory) {
		
		boolean set = false;
		
		if (memory.length == MAX_ADDRESS_SPACE) {
			this.memory = memory;
			set = true;
		}
		
		return set;
	}
	
	
	
	/*
	 * Method Name:		setSwitchData
	 * Description:		Sets the value at PORT_A_DATA based on the switch bits
	 * Parameters:		boolean state: The state of a the bits representing the switches
	 * 					SWITCH_BIT: The switch to set 
	 * Return:			None
	 */
	public void setSwitchData(boolean state, SWITCH_BIT switchBit) {
		byte switchMask = getSwitchMask(switchBit);
		int portADataAddress = UnsignedData.convertShortToUnsignedShort(PORT_A_DATA);
		
		memory[portADataAddress] = state ? (memory[portADataAddress] |= switchMask) : (memory[portADataAddress] &= ~switchMask);
	}
	
	
	
	/*
	 * Method Name:		getSwitchMask
	 * Description:		Given a SWITCH_BIT enum, returns the corresponding bit mask
	 * Parameters:		SWITCH_BIT switchBit: The switch as a enum
	 * Return:			The bit mask as a byte
	 */
	private byte getSwitchMask(SWITCH_BIT switchBit) {
		byte switchMask = 0x00;
		
		switch (switchBit) {
		case SWITCH_ONE:
			switchMask = switchOneMask;
			break;
		case SWITCH_TWO:
			switchMask = switchTwoMask;
			break;
		case SWITCH_THREE:
			switchMask = switchThreeMask;
			break;
		case SWITCH_FOUR:
			switchMask = switchFourMask;
			break;
		}
		
		return switchMask;
	}
	
	
	
	/*
	 * Method Name:		resetEmulator
	 * Description:		Create a new CPU, and wipes out the memory
	 * Parameters:		None
	 * Return:			None
	 */
	public void resetEmulator() throws Exception {
		
		
		cpu = new Freescale6808CPU();
		
		// init memory
		for (int i = 0; i < MAX_ADDRESS_SPACE; i++) {
			memory[i] = 0;
		}			
	}
	
	
	
	/*
	 * Method Name:		reloadS1Records
	 * Description:		Assuming some s1 records have already been loaded, reload them into memory
	 * Parameters:		None	
	 * Return:			None
	 */
	public void reloadS1Records() throws Exception {
		try {
			loadSrecIntoMemory(originalPC, s1Records);
		}
		catch (Exception ex) {
			throw ex;
		}	
	}
	
	
	
	/*
	 * Method Name:		sizeOfMemory
	 * Description:		Returns the max address space
	 * Parameters:		None
	 * Return:			The max address space as an int
	 */
	public int sizeOfMemory() {		
		return MAX_ADDRESS_SPACE;
	}
	
	
	
	/*
	 * Method Name:		isValidMemoryIndex
	 * Description:		Determine whether an index is a valid memory index
	 * Parameters:		int index: The memory index
	 * Return:			Returns true if valid, false otherwise
	 */
	private boolean isValidMemoryIndex(int index) {		
		return index >= 0 && index < MAX_ADDRESS_SPACE;
	}
	
	
	
	/*
	 * Method Name:		isValidAddressingMode
	 * Description:		Determine whether an opcode is a valid addressing mode
	 * Parameters:		The instruction as a short
	 * Return:			True if valid, false otherwise
	 */
	private boolean isValidAddressingMode(short instruction) {		
		return instruction >= (byte)0xa0 && instruction <= (byte)0xff;
	}
	
	
	
	/*
	 * Method Name:		getCpu
	 * Description:		Get the current CPU object
	 * Parameters:		None
	 * Return:			A Freescale6808CPU object
	 */
	public Freescale6808CPU getCpu() {		
		return cpu;
	}

	
	
	/*
	 * Method Name:		setCpu
	 * Description:		Set the CPU object inside the emulator
	 * Parameters:		Freescale6808CPU cpu: The new CPU to set
	 * Return:			None
	 */
	public void setCpu(Freescale6808CPU cpu) {		
		this.cpu = cpu;
	}	
}
