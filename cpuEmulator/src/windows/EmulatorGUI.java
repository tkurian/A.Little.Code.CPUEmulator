/*******************************************************************************************
*Source File Name: EmulatorGUI.java

*Programmer's Name: Josh Wagler, Justin Lang, Tina Kurian

*Date: March, 8th, 2012

*Class Description: This class handle the initialization and management of the graphical user
*interface representing the 6808 CPU emulator.
********************************************************************************************/

package windows;

import cpu.Freescale6808CPU;
import cpu.Freescale6808CPU.CCR_BIT;
import emulator.Freescale6808Emulator;
import emulator.Freescale6808Emulator.SWITCH_BIT;

import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import java.awt.Color;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Toolkit;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import java.awt.SystemColor;
import javax.swing.JTextArea;
import javax.swing.JSlider;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;
import utilities.FileIO;
import utilities.Srec;
import java.util.concurrent.atomic.AtomicBoolean;


/*
 * Class Name:		EmulatorGUI
 * Description:		This class manages the graphical user interface for the 6808 CPU
 * 					emulator. It displays the CPU registers, switches, and the memory
 * 					map. Functions include loading, resetting, and stepping.
 */
public class EmulatorGUI extends JFrame implements ActionListener, ChangeListener
{
	private Freescale6808Emulator emulator;
	private Freescale6808CPU cpu;
	private boolean isFileLoaded;
	private AtomicBoolean isProgramRunning;
	private AtomicBoolean executeError;
	private String errorMessage;
	private Timer runTimer;
	
	private JPanel processorPanel;
	private JPanel cpuPanel;
	private JButton stepButton;
	private JButton runButton;
	private JButton stopButton;
	private JSlider memorySlider;
	
	private JCheckBox switchOneBox;
	private JCheckBox switchTwoBox;
	private JCheckBox switchThreeBox;
	private JCheckBox switchFourBox;
	private JCheckBox carryBox;
	private JCheckBox zeroBox;
	private JCheckBox negativeBox;
	private JCheckBox interruptBox;
	private JCheckBox halfBox;
	private JCheckBox overflowBox;
	
	private JTextField ccrValue;
	private JTextField accumulatorValue;
	private JTextField pcValue;
	private JTextField spValue;
	private JTextField regLowValue;
	private JTextField regHighValue;
	private JTextArea memoryMapText;
	
	private JMenuItem loadMenuItem;
	private JMenuItem resetMenuItem;
	private JMenuItem exitMenuItem;
	
	
	public EmulatorGUI() 
	{
		//initialize emulator, CPU, and GUI
		setIconImage(Toolkit.getDefaultToolkit().getImage(EmulatorGUI.class.getResource("/javax/swing/plaf/metal/icons/ocean/menu.gif")));
		emulator = new Freescale6808Emulator();
		isFileLoaded = false;
		EmulatorGUI_Init();
		isProgramRunning = new AtomicBoolean(false);
		executeError = new AtomicBoolean(false);		
		runTimer = new Timer(100, this);
		cpu = new Freescale6808CPU();
		emulator.setCpu(cpu);
		UpdateCPUValues();
	}
	
	
	/*
	 * Method Name: EmulatorGUI_Init
	 * Description: This method initializes the GUI, emulator, and CPU and starts it up.
	 * Parameters: No parameters.
	 * Return: No return value.
	 */
	private void EmulatorGUI_Init()
	{
		//initialize the main GUI frame
		setResizable(false);
		setTitle("6808 Emulator");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 752, 500);
		processorPanel = new JPanel();
		processorPanel.setBackground(SystemColor.inactiveCaption);
		processorPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(processorPanel);
		processorPanel.setLayout(null);
		
		//initialize additional panels used on the GUI
		cpuPanel = new JPanel();
		cpuPanel.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		cpuPanel.setBackground(Color.WHITE);
		cpuPanel.setBounds(479, 71, 256, 378);
		processorPanel.add(cpuPanel);
		cpuPanel.setLayout(null);
		
		//initialize buttons and switches with action listeners
		stepButton = new JButton("Perform Step");
		stepButton.setToolTipText("CPU Program Step");
		stepButton.setIcon(new ImageIcon(EmulatorGUI.class.getResource("/javax/swing/plaf/metal/icons/ocean/minimize-pressed.gif")));
		stepButton.setFont(new Font("Calibri", Font.PLAIN, 12));
		stepButton.setBounds(338, 33, 131, 23);
		processorPanel.add(stepButton);
		stepButton.addActionListener(this);
		
		switchOneBox = new JCheckBox("Switch 1");
		switchOneBox.setBackground(Color.WHITE);
		switchOneBox.setFont(new Font("Calibri", Font.PLAIN, 10));
		switchOneBox.setBounds(2, 8, 59, 23);
		switchOneBox.addActionListener(this);
		cpuPanel.add(switchOneBox);
		
		switchTwoBox = new JCheckBox("Switch 2");
		switchTwoBox.setBackground(Color.WHITE);
		switchTwoBox.setFont(new Font("Calibri", Font.PLAIN, 10));
		switchTwoBox.setBounds(67, 8, 65, 23);
		switchTwoBox.addActionListener(this);
		cpuPanel.add(switchTwoBox);
		
		switchThreeBox = new JCheckBox("Switch 3");
		switchThreeBox.setBackground(Color.WHITE);
		switchThreeBox.setFont(new Font("Calibri", Font.PLAIN, 10));
		switchThreeBox.setBounds(129, 8, 65, 23);
		switchThreeBox.addActionListener(this);
		cpuPanel.add(switchThreeBox);
		
		switchFourBox = new JCheckBox("Switch 4");
		switchFourBox.setBackground(Color.WHITE);
		switchFourBox.setFont(new Font("Calibri", Font.PLAIN, 10));
		switchFourBox.setBounds(190, 8, 65, 23);
		switchFourBox.addActionListener(this);
		cpuPanel.add(switchFourBox);
		
		//initialize the line seperators used to break up components
		JSeparator switchSeperator = new JSeparator();
		switchSeperator.setForeground(Color.GRAY);
		switchSeperator.setBounds(0, 37, 256, 2);
		cpuPanel.add(switchSeperator);
		
		JSeparator flagSeperator = new JSeparator();
		flagSeperator.setForeground(Color.GRAY);
		flagSeperator.setBounds(0, 191, 256, 2);
		cpuPanel.add(flagSeperator);
		
		//initialize all the labels on the GUI
		JLabel ccrLabel = new JLabel("CCR");
		ccrLabel.setFont(new Font("Calibri", Font.BOLD, 12));
		ccrLabel.setBounds(123, 203, 32, 14);
		cpuPanel.add(ccrLabel);
		
		JLabel lblCpu = new JLabel("6808 CPU");
		lblCpu.setFont(new Font("Calibri", Font.BOLD, 32));
		lblCpu.setBounds(549, 33, 144, 35);
		processorPanel.add(lblCpu);
		
		JLabel accumulatorLabel = new JLabel("Accumulator:");
		accumulatorLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		accumulatorLabel.setBounds(10, 66, 80, 14);
		cpuPanel.add(accumulatorLabel);
		
		JLabel pcLabel = new JLabel("Program Counter:");
		pcLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		pcLabel.setBounds(10, 91, 98, 14);
		cpuPanel.add(pcLabel);
		
		JLabel spLabel = new JLabel("Stack Pointer:");
		spLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		spLabel.setBounds(10, 116, 80, 14);
		cpuPanel.add(spLabel);
		
		JLabel regLowLabel = new JLabel("Index Register Low:");
		regLowLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		regLowLabel.setBounds(10, 141, 113, 14);
		cpuPanel.add(regLowLabel);
		
		JLabel regHighLabel = new JLabel("Index Register High:");
		regHighLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		regHighLabel.setBounds(10, 166, 113, 14);
		cpuPanel.add(regHighLabel);
		
		JLabel carryLabel = new JLabel("Carry Flag:");
		carryLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		carryLabel.setBounds(14, 228, 113, 14);
		cpuPanel.add(carryLabel);
		
		JLabel zeroLabel = new JLabel("Zero Flag:");
		zeroLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		zeroLabel.setBounds(14, 253, 113, 14);
		cpuPanel.add(zeroLabel);
		
		JLabel negativeLabel = new JLabel("Negative Flag:");
		negativeLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		negativeLabel.setBounds(14, 278, 113, 14);
		cpuPanel.add(negativeLabel);
		
		JLabel interruptLabel = new JLabel("Interrupt Flag:");
		interruptLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		interruptLabel.setBounds(14, 303, 113, 14);
		cpuPanel.add(interruptLabel);
		
		JLabel halfLabel = new JLabel("Half-Carry Flag:");
		halfLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		halfLabel.setBounds(14, 328, 113, 14);
		cpuPanel.add(halfLabel);
		
		JLabel overflowLabel = new JLabel("Overflow Flag:");
		overflowLabel.setFont(new Font("Calibri", Font.PLAIN, 12));
		overflowLabel.setBounds(14, 353, 113, 14);
		cpuPanel.add(overflowLabel);
		
		JLabel statusFlagsLabel = new JLabel("Status Flags");
		statusFlagsLabel.setFont(new Font("Calibri", Font.BOLD, 14));
		statusFlagsLabel.setBounds(10, 203, 80, 14);
		cpuPanel.add(statusFlagsLabel);
		
		JLabel registersLabel = new JLabel("Registers");
		registersLabel.setFont(new Font("Calibri", Font.BOLD, 14));
		registersLabel.setBounds(10, 41, 80, 14);
		cpuPanel.add(registersLabel);
		
		JLabel zeroSlider = new JLabel("200");
		zeroSlider.setFont(new Font("Calibri", Font.BOLD, 20));
		zeroSlider.setBounds(10, 452, 36, 21);
		processorPanel.add(zeroSlider);
		
		JLabel label = new JLabel("65535");
		label.setFont(new Font("Calibri", Font.BOLD, 20));
		label.setBounds(419, 452, 50, 21);
		processorPanel.add(label);
		
		JLabel memoryLabel = new JLabel("Memory Map");
		memoryLabel.setFont(new Font("Calibri", Font.BOLD, 32));
		memoryLabel.setBounds(131, 33, 197, 35);
		processorPanel.add(memoryLabel);
		
		JLabel sliderMsgLabel = new JLabel("Adjust Slider to Increase Displayed Memory");
		sliderMsgLabel.setFont(new Font("Calibri", Font.BOLD, 12));
		sliderMsgLabel.setBounds(118, 454, 226, 21);
		processorPanel.add(sliderMsgLabel);
		
		//initialize check boxes for the CCR values
		carryBox = new JCheckBox("");
		carryBox.setEnabled(false);
		carryBox.setBackground(Color.WHITE);
		carryBox.setBounds(158, 223, 97, 23);
		cpuPanel.add(carryBox);
		
		zeroBox = new JCheckBox("");
		zeroBox.setBackground(Color.WHITE);
		zeroBox.setEnabled(false);
		zeroBox.setBounds(158, 248, 97, 23);
		cpuPanel.add(zeroBox);
		
		negativeBox = new JCheckBox("");
		negativeBox.setBackground(Color.WHITE);
		negativeBox.setEnabled(false);
		negativeBox.setBounds(158, 273, 97, 23);
		cpuPanel.add(negativeBox);
		
		interruptBox = new JCheckBox("");
		interruptBox.setBackground(Color.WHITE);
		interruptBox.setEnabled(false);
		interruptBox.setBounds(158, 298, 97, 23);
		cpuPanel.add(interruptBox);
		
		halfBox = new JCheckBox("");
		halfBox.setBackground(Color.WHITE);
		halfBox.setEnabled(false);
		halfBox.setBounds(158, 323, 97, 23);
		cpuPanel.add(halfBox);
		
		overflowBox = new JCheckBox("");
		overflowBox.setBackground(Color.WHITE);
		overflowBox.setEnabled(false);
		overflowBox.setBounds(158, 348, 97, 23);
		cpuPanel.add(overflowBox);
		
		//initialize the register values found on the CPU panel
		ccrValue = new JTextField();
		ccrValue.setEditable(false);
		ccrValue.setBackground(Color.WHITE);
		ccrValue.setHorizontalAlignment(SwingConstants.CENTER);
		ccrValue.setFont(new Font("Calibri", Font.PLAIN, 12));
		ccrValue.setText("0x00");
		ccrValue.setBounds(148, 200, 45, 20);
		cpuPanel.add(ccrValue);
		ccrValue.setColumns(10);
		
		accumulatorValue = new JTextField();
		accumulatorValue.setHorizontalAlignment(SwingConstants.CENTER);
		accumulatorValue.setFont(new Font("Calibri", Font.PLAIN, 12));
		accumulatorValue.setEditable(false);
		accumulatorValue.setBackground(Color.WHITE);
		accumulatorValue.setText("0x0000");
		accumulatorValue.setBounds(148, 62, 45, 20);
		cpuPanel.add(accumulatorValue);
		accumulatorValue.setColumns(10);
		
		pcValue = new JTextField();
		pcValue.setHorizontalAlignment(SwingConstants.CENTER);
		pcValue.setEditable(false);
		pcValue.setFont(new Font("Calibri", Font.PLAIN, 12));
		pcValue.setText("0x0000");
		pcValue.setColumns(10);
		pcValue.setBackground(Color.WHITE);
		pcValue.setBounds(148, 87, 45, 20);
		cpuPanel.add(pcValue);
		
		spValue = new JTextField();
		spValue.setHorizontalAlignment(SwingConstants.CENTER);
		spValue.setFont(new Font("Calibri", Font.PLAIN, 12));
		spValue.setEditable(false);
		spValue.setText("0x0000");
		spValue.setColumns(10);
		spValue.setBackground(Color.WHITE);
		spValue.setBounds(148, 112, 45, 20);
		cpuPanel.add(spValue);
		
		regLowValue = new JTextField();
		regLowValue.setHorizontalAlignment(SwingConstants.CENTER);
		regLowValue.setFont(new Font("Calibri", Font.PLAIN, 12));
		regLowValue.setEditable(false);
		regLowValue.setText("0x00");
		regLowValue.setColumns(10);
		regLowValue.setBackground(Color.WHITE);
		regLowValue.setBounds(148, 137, 45, 20);
		cpuPanel.add(regLowValue);
		
		regHighValue = new JTextField();
		regHighValue.setHorizontalAlignment(SwingConstants.CENTER);
		regHighValue.setFont(new Font("Calibri", Font.PLAIN, 12));
		regHighValue.setEditable(false);
		regHighValue.setText("0x00");
		regHighValue.setColumns(10);
		regHighValue.setBackground(Color.WHITE);
		regHighValue.setBounds(148, 160, 45, 20);
		cpuPanel.add(regHighValue);
		
		//initialize all elements found in the menu bar at the top
		JMenuBar mainMenu = new JMenuBar();
		mainMenu.setBackground(UIManager.getColor("Button.shadow"));
		mainMenu.setBounds(0, 0, 746, 21);
		processorPanel.add(mainMenu);
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.setIcon(new ImageIcon(EmulatorGUI.class.getResource("/com/sun/java/swing/plaf/windows/icons/NewFolder.gif")));
		fileMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		mainMenu.add(fileMenu);
		
		loadMenuItem = new JMenuItem("Load S-REC File");
		loadMenuItem.setIcon(new ImageIcon(EmulatorGUI.class.getResource("/javax/swing/plaf/metal/icons/ocean/file.gif")));
		loadMenuItem.addActionListener(this);
		fileMenu.add(loadMenuItem);
		
		resetMenuItem = new JMenuItem("Reset CPU");
		resetMenuItem.setIcon(new ImageIcon(EmulatorGUI.class.getResource("/com/sun/java/swing/plaf/windows/icons/FloppyDrive.gif")));
		resetMenuItem.addActionListener(this);
		fileMenu.add(resetMenuItem);
		
		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setIcon(new ImageIcon(EmulatorGUI.class.getResource("/javax/swing/plaf/metal/icons/ocean/close-pressed.gif")));
		exitMenuItem.addActionListener(this);
		fileMenu.add(exitMenuItem);
		
		//initialize the memory mapping elements on the GUI
		memorySlider = new JSlider();
		memorySlider.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		memorySlider.setToolTipText("Memory Slider");
		memorySlider.setForeground(SystemColor.desktop);
		memorySlider.setFont(new Font("Calibri", Font.PLAIN, 12));
		memorySlider.setPaintLabels(true);
		memorySlider.setBackground(Color.WHITE);
		memorySlider.setMinimum(200);
		memorySlider.setValue(200);
		memorySlider.setMaximum(65535);
		memorySlider.setBounds(10, 426, 459, 23);
		memorySlider.addChangeListener(this);
		processorPanel.add(memorySlider);
		
		JScrollPane memoryScroll = new JScrollPane();
		memoryScroll.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
		memoryScroll.setBounds(10, 71, 459, 355);
		processorPanel.add(memoryScroll);
		
		memoryMapText = new JTextArea();
		memoryMapText.setBackground(Color.WHITE);
		memoryMapText.setWrapStyleWord(true);
		memoryScroll.setViewportView(memoryMapText);
		memoryMapText.setFont(new Font("Calibri", Font.PLAIN, 11));
		memoryMapText.setEditable(false);
		
		runButton = new JButton("Run");
		runButton.setIcon(new ImageIcon(EmulatorGUI.class.getResource("/com/sun/java/swing/plaf/windows/icons/TreeLeaf.gif")));
		runButton.setFont(new Font("Calibri", Font.PLAIN, 12));
		runButton.setBounds(10, 21, 89, 23);
		runButton.addActionListener(this);
		processorPanel.add(runButton);
		
		stopButton = new JButton("Stop");
		stopButton.setIcon(new ImageIcon(EmulatorGUI.class.getResource("/javax/swing/plaf/metal/icons/ocean/close.gif")));
		stopButton.setFont(new Font("Calibri", Font.PLAIN, 12));
		stopButton.setBounds(10, 43, 89, 23);
		stopButton.setEnabled(false);
		stopButton.addActionListener(this);
		processorPanel.add(stopButton);
	}

	
	/*
	 * Method Name: UpdateCPUValues
	 * Description: This method updates the CPU register values based on emulator CPU.
	 * Parameters: No parameters.
	 * Return: No return value.
	 */
	private void UpdateCPUValues()
	{
		String prefix = "0x";
		String memoryMap = "";
		
		//get the current CPU from the emulator, and update the memory map
		cpu = emulator.getCpu();
		memoryMap = UpdateMemoryMap();
		memoryMapText.setText(memoryMap);
		memoryMapText.setCaretPosition(0);
		
		//display the current register values
		accumulatorValue.setText(prefix + String.format("%04X", cpu.getA()));
		pcValue.setText(prefix + String.format("%04X", cpu.getPC()));
		spValue.setText(prefix + String.format("%04X", cpu.getSP()));
		regLowValue.setText(prefix + String.format("%02X", cpu.getX()));
		regHighValue.setText(prefix + String.format("%02X", cpu.getH()));
		
		//display the current CCR values including the flag values
		ccrValue.setText(prefix + String.format("%02X", cpu.getCCR()));
		carryBox.setSelected(cpu.isCCRBit(CCR_BIT.CARRY));
		zeroBox.setSelected(cpu.isCCRBit(CCR_BIT.ZERO));
		negativeBox.setSelected(cpu.isCCRBit(CCR_BIT.NEGATIVE));
		interruptBox.setSelected(cpu.isCCRBit(CCR_BIT.INTERRUPT));
		halfBox.setSelected(cpu.isCCRBit(CCR_BIT.HALF));
		overflowBox.setSelected(cpu.isCCRBit(CCR_BIT.OVERFLOW));
	}
	
	
	/*
	 * Method Name: UpdateMemoryMap
	 * Description: This method updates the memory map based on current slider value.
	 * Parameters: No parameters.
	 * Return: Returns a formatted string with the memory map.
	 */
	private String UpdateMemoryMap()
	{
		String tempMemory = "";
		String memoryMap = "";
		byte cpuMemory[] = emulator.getMemoryArray();
		int x = 0;
		int y = 0;
		
		//loop until the current slider value is met
		for(x = 0;x < (memorySlider.getValue());x += 16)
		{
			//parse the current 10 byte value
			tempMemory = String.format("%04X\t:\t", x);
			
			//loop until 10 bytes have been printed
			for(y = 0; y < 16; y++)
			{
				//only print if the value is less than 65535 (MAX_ADDRESS_SPACE
				if((x + y) <= 65535)
				{
					tempMemory += String.format("%02X  ", cpuMemory[x + y]);
				}
			}
			
			//build the memory map string of all memory values
			memoryMap += tempMemory + "\n";
		}
		
		return memoryMap;
	}
	
	
	
	/*
	 * Method Name: stateChanged
	 * Description: This method updates the memory map based on current slider value.
	 * Parameters: ChangeEvent e
	 * Return:		None
	 */
	public void stateChanged(ChangeEvent e)
	{
		JSlider source = (JSlider)e.getSource();
	    if (!source.getValueIsAdjusting()) 
	    {
	    	memoryMapText.setText(UpdateMemoryMap());
	    	memoryMapText.setCaretPosition(0);
	    }
	}
	
	
	
	
	/*
	 * Method Name: actionPerformed
	 * Description: This method implements the action listener handling the button clicks triggered from the GUI.
	 * Parameters
	 * 	ActionEvent e = event arguments for the button clicked
	 * Return: No return value.
	 */
	public void actionPerformed(ActionEvent e)
	{
		Object buttonClicked = e.getSource();
		FileIO srecFile = new FileIO();
		Srec validSrecFile = new Srec();
		List<byte[]> recordList = new ArrayList<byte[]>();
		short startingAddress = (short)0x0000;
		
		if(buttonClicked == loadMenuItem) //handle load button click
		{
			//show the user a file dialog and only let them select .srec files
			int fileResult = 0;
			JFileChooser ofd = new JFileChooser("C:\\");
			ofd.setFileFilter(new FileNameExtensionFilter("S-Record File (*.srec)", "srec"));
			fileResult = ofd.showOpenDialog(this);
			
			//only if the OK button is clicked, load / parse / load memory / update CPU values
			if(fileResult == JFileChooser.APPROVE_OPTION)
			{
				String fileContents = srecFile.loadSREC(ofd.getSelectedFile().getAbsolutePath());
				
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
							UpdateCPUValues();
							isFileLoaded = true;
						}
						catch (Exception ex) 
						{
							JOptionPane.showMessageDialog(this, ex.getMessage(), "File Load Failed", JOptionPane.OK_OPTION);
						}						
					}
					else
					{
						JOptionPane.showMessageDialog(this, "The S-Record file contents were not valid. File load failed.", "File Load Failed", JOptionPane.OK_OPTION);
					}
				}
			}
			else
			{
				JOptionPane.showMessageDialog(this, "The S-Record file was not a valid .srec file. File load failed.", "File Load Failed", JOptionPane.OK_OPTION);
			}
		}
		else if(buttonClicked == resetMenuItem) //handle reset button click
		{
			if (isFileLoaded)
			{
				//reset CPU and store it as the new emulator CPU
				try
				{
					emulator.resetEmulator();
					emulator.reloadS1Records();
					UpdateCPUValues();
				} 
				catch (Exception ex) 
				{
					JOptionPane.showMessageDialog(this, ex.getMessage(), "Reset Failed", JOptionPane.OK_OPTION);
				}
			}
		}
		else if(buttonClicked == exitMenuItem) //handle exit button click
		{
			//confirm with the user that they want to exit
			int exitResult = 0;
			exitResult = JOptionPane.showOptionDialog(this, "Are you sure you wish to exit the emulator?", "Exit 6808 Emulator", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			
			//if YES was selected, indicating exit emulator
			if(exitResult == 0)
			{
				System.exit(0);
			}
		}
		else if(buttonClicked == stepButton) //handle step button click
		{
			//isFileLoaded = true;
			if(isFileLoaded)
			{
				//perform a step in the emulator and update the CPU values on the GUI
				try
				{
					emulator.singleStep();
					UpdateCPUValues();
				}
				catch (Exception ex) 
				{
					JOptionPane.showMessageDialog(this, ex.getMessage(), "Execute Step Failed", JOptionPane.OK_OPTION);
					handleEmulatorError();
				}
			}
			else
			{
				JOptionPane.showMessageDialog(this, "A valid s-record file must be loaded before a step can be executed.", "Execute Step Failed", JOptionPane.OK_OPTION);
			}
		}
		else if(buttonClicked == switchOneBox) //handle switch one button click
		{
			//0 or 1 to indicate switch one status
			emulator.setSwitchData(switchOneBox.isSelected(), SWITCH_BIT.SWITCH_ONE);
			UpdateCPUValues();
		}
		else if(buttonClicked == switchTwoBox) //handle switch two button click
		{
			//0 or 1 to indicate switch two status
			emulator.setSwitchData(switchTwoBox.isSelected(), SWITCH_BIT.SWITCH_TWO);
			UpdateCPUValues();
		}
		else if(buttonClicked == switchThreeBox) //handle switch three button click
		{
			//0 or 1 to indicate switch three status
			emulator.setSwitchData(switchThreeBox.isSelected(), SWITCH_BIT.SWITCH_THREE);
			UpdateCPUValues();
		}
		else if(buttonClicked == switchFourBox) //handle switch four button click
		{
			//0 or 1 to indicate switch four status
			emulator.setSwitchData(switchFourBox.isSelected(), SWITCH_BIT.SWITCH_FOUR);
			UpdateCPUValues();
		}
		else if(buttonClicked == runButton)
		{
			if (isFileLoaded)
			{
				isProgramRunning.set(true);
				stepButton.setEnabled(false);
				runButton.setEnabled(false);
				resetMenuItem.setEnabled(false);
				loadMenuItem.setEnabled(false);
				stopButton.setEnabled(true);
				runTimer.start();
				
				SwingWorker worker = new SwingWorker()
				{
					public Object doInBackground()
					{					
						while(isProgramRunning.get() == true)
						{
							try
							{
								emulator.singleStep();
								UpdateCPUValues();
							}
							catch (Exception ex) 
							{
								errorMessage = ex.getMessage();
								executeError.set(true);
								stopButton.setEnabled(false);
								isProgramRunning.set(false);
							}
						}
						
						return new Object();
					}
				};
				
				worker.execute();
			}
			else
			{
				JOptionPane.showMessageDialog(this, "A valid s-record file must be loaded before execution.", "Run Program Failed", JOptionPane.OK_OPTION);
			}
		}
		else if(buttonClicked == stopButton)
		{
			isProgramRunning.set(false);
			runButton.setEnabled(true);
			stepButton.setEnabled(true);
			resetMenuItem.setEnabled(true);
			loadMenuItem.setEnabled(true);
			stopButton.setEnabled(false);
			runTimer.stop();
			UpdateCPUValues();
		}
		else if(buttonClicked == runTimer)
		{
			UpdateCPUValues();
			if (executeError.get() == true)
			{
				isProgramRunning.set(false);
				runButton.setEnabled(true);
				stepButton.setEnabled(true);
				resetMenuItem.setEnabled(true);
				loadMenuItem.setEnabled(true);
				stopButton.setEnabled(false);
				runTimer.stop();
				UpdateCPUValues();
				
				JOptionPane.showMessageDialog(this, errorMessage, "Execute Step Failed", JOptionPane.OK_OPTION);
				
				handleEmulatorError();
			}
		}
	}
	
	private void handleEmulatorError() 
	{
		executeError.set(false);
		try 
		{
			emulator.resetEmulator();
			emulator.reloadS1Records();
			UpdateCPUValues();	
		} 
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, ex.getMessage(), "Execution Failed", JOptionPane.OK_OPTION);
		}	
	}		
}