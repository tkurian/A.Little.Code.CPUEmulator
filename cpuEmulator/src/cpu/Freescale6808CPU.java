
/*
 * Filename:		Freescale6808CPU.java
 * Package:			cpu
 * Project:			padA2
 * By:				Justin Lang, Josh Wagler & Tina Kurian
 * Date:			March 7, 2012
 * Description:		Contains the Freescale6808CPU class
 */



package cpu;



/*
 * Class Name:		Freescale6808CPU
 * Description:		This class is a virtual representation of a 6808 CPU, containing
 * 					attributes which map to the various registers of the CPU, and methods
 * 					to get/set the register values.
 */
public class Freescale6808CPU {
	
	private byte accumulator;
	private short programCounter;
	private short stackPointer;
	private short indexRegister;
	private byte indexRegisterLow;
	private byte indexRegisterHigh;
	
	private byte conditionCodeRegister;	
	private final static byte carryMask = (byte)0x01;
	private final static byte zeroMask = (byte)0x02;
	private final static byte negativeMask = (byte)0x04;
	private final static byte interruptMask = (byte)0x08;
	private final static byte halfMask = (byte)0x10;
	private final static byte overflowMask = (byte)0x80;
	
	
	
	/*
	 * Enum Name: 		ccrBit
	 * Description:		Used to get/set CCR bits using the methods isCCRBit or setCCRBit
	 */	
	public enum CCR_BIT {
		CARRY, ZERO, NEGATIVE, INTERRUPT, HALF, OVERFLOW
	}
	
	
	
	/*
	 * Method Name:		Freescale6808CPU
	 * Description:		Default constructor
	 * Parameters:		None
	 * Return:			None
	 */	
	public Freescale6808CPU() {
		accumulator = 0;
		programCounter = 0;
		stackPointer = 0;
		indexRegister = 0;
		indexRegisterHigh = 0;
		indexRegisterLow = 0;
		conditionCodeRegister = 0;
	}

	
	
	/*
	 * Method Name:		getA
	 * Description:		Gets the accumulator value
	 * Parameters:		None
	 * Return:			The accumulator value as a byte
	 */	
	public byte getA() {
		return accumulator;
	}

	
	
	/*
	 * Method Name:		setA
	 * Description:		Sets the accumulator value
	 * Parameters:		byte accumulator: The new accumulator value as a byte
	 * Return:			None
	 */	
	public void setA(byte accumulator) {
		this.accumulator = accumulator;
	}

	
	
	/*
	 * Method Name:		getPC
	 * Description:		Gets the program counter value
	 * Parameters:		None
	 * Return:			The program counter value as a short
	 */	
	public short getPC() {
		return programCounter;
	}

	
	
	/*
	 * Method Name:		setPC
	 * Description:		Sets the program counter value
	 * Parameters:		short programCounter: The new program counter value as a short
	 * Return:			None
	 */	
	public void setPC(short programCounter) {
		this.programCounter = programCounter;
	}

	
	
	/*
	 * Method Name:		getSP
	 * Description:		Gets the stack pointer value
	 * Parameters:		None
	 * Return:			The stack pointer value as a short
	 */	
	public short getSP() {
		return stackPointer;
	}

	
	
	/*
	 * Method Name:		setSP
	 * Description:		Sets the stack pointer value
	 * Parameters:		short stackPointer: The new stack pointer value as a short
	 * Return:			None
	 */		
	public void setSP(short stackPointer) {
		this.stackPointer = stackPointer;
	}

	
	
	/*
	 * Method Name:		getHX
	 * Description:		Gets the 16-bit index register value
	 * Parameters:		None
	 * Return:			The 16-bit index register value as a short
	 */	
	public short getHX() {
		return indexRegister;
	}

	
	
	/*
	 * Method Name:		setHX
	 * Description:		Sets the 16-bit index register value.  Modifies both 8-bit index registers
	 * Parameters:		short indexRegister: The 16-bit index register value as a short
	 * Return:			None
	 */	
	public void setHX(short indexRegister) {				
		this.indexRegister = indexRegister;			
		this.indexRegisterHigh = (byte)((indexRegister >> 8) & (short)0x00ff);
		this.indexRegisterLow = (byte)(indexRegister & (short)0x00ff);		
	}

	
	
	/*
	 * Method Name:		getX
	 * Description:		Gets the low 8-bit index register value
	 * Parameters:		None
	 * Return:			The low 8-bit index register value as a byte
	 */	
	public byte getX() {
		return indexRegisterLow;
	}

	
	
	/*
	 * Method Name:		setX
	 * Description:		Sets the low 8-bit index register value.  Modifies the 16-bit index register
	 * Parameters:		byte indexRegisterLow: The low 8-bit index register value as a byte
	 * Return:			None
	 */	
	public void setX(byte indexRegisterLow) {
		this.indexRegisterLow = indexRegisterLow;
		this.indexRegister &= (short)0xff00;
		this.indexRegister |= indexRegisterLow;
	}

	
	
	/*
	 * Method Name:		getH
	 * Description:		Gets the high 8-bit index register value
	 * Parameters:		None
	 * Return:			The high 8-bit register value as a byte
	 */	
	public byte getH() {
		return indexRegisterHigh;
	}

	
	
	/*
	 * Method Name:		setH
	 * Description:		Sets the high 8-bit register value.  Modifies the 16-bit register
	 * Parameters:		byte indexRegisterHigh: The high 8-bit register value as a byte
	 * Return:			None
	 */	
	public void setH(byte indexRegisterHigh) {
		this.indexRegisterHigh = indexRegisterHigh;
		this.indexRegister &= (short)0x00ff;
		this.indexRegister |= (indexRegisterHigh << 8);
	}

	
	
	/*
	 * Method Name:		getCCR
	 * Description:		Gets the ccr register value as a byte
	 * Parameters:		None
	 * Return:			The ccr register value as a byte
	 */	
	public byte getCCR() {
		return conditionCodeRegister;
	}

	
	
	/*
	 * Method Name:		setCCR
	 * Description:		Sets the ccr register value
	 * Parameters:		byte conditionCodeRegister: The new ccr register value as a byte
	 * Return:			None
	 */	
	public void setCCR(byte conditionCodeRegister) {
		this.conditionCodeRegister = conditionCodeRegister;
	}
	
	
	
	/*
	 * Method Name:		isCCRBit
	 * Description:		Given the specific ccr bit enum value, returns whether the bit is on or off
	 * Parameters:		CCR_BIT bit: The ccr bit enum value
	 * Return:			Returns true if the bit is on, false otherwise
	 */	
	public boolean isCCRBit(CCR_BIT bit) {		
		byte mask = getMask(bit);
		return ((conditionCodeRegister & mask) == mask);
	}
	
	
	
	/*
	 * Method Name:		setCCRBit
	 * Description:		Given the specific ccr bit enum value, and a true/false value, sets the specified bit appropriately
	 * Parameters:		boolean state: The new state for the bit
	 * 					CCR_BIT: The ccr bit enum value 
	 * Return:			None
	 */	
	public void setCCRBit(boolean state, CCR_BIT bit) {		
		byte mask = getMask(bit);		
		conditionCodeRegister = state ? (conditionCodeRegister |= mask) : (conditionCodeRegister &= ~mask);
	}
	
	
	
	/*
	 * Method Name:		getMask
	 * Description:		Given a specific ccr bit enum value, returns a byte with the correct bit mask
	 * 					to use against the condition code register value to determine if the bit is set or not
	 * Parameters:		CCR_BIT bit: The specific ccr bit enum value
	 * Return:			A byte containing the corresponding bit mask
	 */	
	private byte getMask(CCR_BIT bit) {		
		byte mask = 0;
		
		switch (bit) {
			case CARRY:
				mask = carryMask;
				break;
			case ZERO:
				mask = zeroMask;
				break;
			case NEGATIVE:
				mask = negativeMask;
				break;
			case INTERRUPT:
				mask = interruptMask;
				break;
			case HALF:
				mask = halfMask;
				break;
			case OVERFLOW:
				mask = overflowMask;
				break;
		}
		
		return mask;
	}
}
