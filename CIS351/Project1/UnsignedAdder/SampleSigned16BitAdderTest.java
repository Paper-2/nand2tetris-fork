package CIS351.Project1.UnsignedAdder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.BeforeClass;

import Hack.HardwareSimulator.HardwareSimulator;
import Hack.Controller.CommandException;
import Hack.Controller.ProgramException;
import Hack.Controller.VariableException;
import Hack.Gates.GatesManager;



public class SampleSigned16BitAdderTest {

  private static HardwareSimulator sim;
  private static final String DEFAULT_HDL =
      System.getenv("HDL") != null && !System.getenv("HDL").trim().isEmpty()
          ? System.getenv("HDL").trim()
          : "C:\\Users\\holac\\source\\bin\\nand2tetris\\projects\\signed CLA\\CLA16.hdl";


  @BeforeClass
  public static void init() {
    String hdl = System.getProperty("hdl");
    if (hdl == null || hdl.trim().isEmpty()) {
      hdl = DEFAULT_HDL;
    }
    sim = new HardwareSimulator();
    try {
      // Ensure built-in chips (Xor, And, etc.) are found when running headless
      // Points to the repo's built-in chips folder relative to the workspace root
      // Set the simulator working directory to the HDL file's directory and load by filename only
      java.io.File hdlFile = new java.io.File(hdl);
      sim.setWorkingDir(hdlFile);
      sim.doCommand(new String[]{"load", hdlFile.getName()});
    } catch (CommandException | ProgramException | VariableException e) {
      throw new RuntimeException("Failed to load HDL: " + hdl, e);
    }
  }

  // Wrapper helpers over HardwareSimulator
  private static void run() {
    try {
      sim.doCommand(new String[]{"eval"});
    } catch (CommandException | ProgramException | VariableException e) {
      throw new RuntimeException("Failed to eval", e);
    }
  }

  private static void setPin(String name, Object value) {
    try {
      if (value instanceof Boolean) {
        sim.setValue(name, ((Boolean) value) ? "1" : "0");
      } else if (value instanceof Number) {
        sim.setValue(name, value.toString());
      } else {
        throw new IllegalArgumentException("Unsupported value type for pin: " + value.getClass());
      }
    } catch (VariableException e) {
      throw new RuntimeException("Failed to set pin '" + name + "'", e);
    }
  }

  // setPinSigned: send signed 16-bit value as a Java short string (sim expects -32768..32767)
  private static void setPinSigned(String name, long signed16) {
    short s = (short) signed16; // two's complement wrap
    try {
      sim.setValue(name, Short.toString(s));
    } catch (VariableException e) {
      throw new RuntimeException("Failed to set pin '" + name + "'", e);
    }
  }

  private static long readPinSigned(String name) {
    try {
      String s = sim.getValue(name);
      return Long.parseLong(s); // simulator returns signed short value as decimal
    } catch (VariableException e) {
      throw new RuntimeException("Failed to read pin '" + name + "'", e);
    }
  }

  private static long readPin(String name) {
    try {
      String s = sim.getValue(name);
      return Long.parseLong(s);
    } catch (VariableException e) {
      throw new RuntimeException("Failed to read pin '" + name + "'", e);
    }
  }

  public static final long testIntegers[] = {-32768, -32767, 0, 1, 2, 13, 127, 128, 129, 0x5555, 32766, 32767};

  protected static void verify(long a, long b, boolean carryIn) {
    long carryInAsInt = (carryIn ? 1 : 0);
    long expected = a + b + carryInAsInt;
    boolean expectedOverflow = ((expected >= (1 << 15)) || (expected < -(1 << 15)));
    if (expectedOverflow && expected > 0) {
      expected -= 65536;
    } else if (expectedOverflow && expected < 0) {
      expected += 65536;
    }
    // Unsigned carry-out from bit 15
    boolean expectedCarryOut = (((a & 0xFFFF) + (b & 0xFFFF) + carryInAsInt) > 0xFFFF);
    setPinSigned("a", a);
    setPinSigned("b", b);
    setPin("cin", carryIn);
    run();
    String message = "of " + a + " + " + b + " with " + (carryIn ? "a " : " no ") + " carry in";
    Assert.assertEquals("Output " + message, expected, readPinSigned("sum"));
    Assert.assertEquals("Overflow " + message, expectedOverflow, readPin("overflow") != 0);
    Assert.assertEquals("carry out " + message, expectedCarryOut, readPin("carryout") != 0);
  }

  @Test
  public void zero_zero_false() {
    verify(0, 0, false);
  }

  @Test
  public void zero_one_false() {
    verify(0, 1, false);
  }

  @Test
  public void zero_zero_true() {
    verify(0, 0, true);
  }

  @Test
  public void zero_one_true() {
    verify(0, 1, true);
  }

  @Test
  public void testAll() {
    for (long a : testIntegers) {
      for (long b : testIntegers) {
        verify(a, b, false);
        verify(a, b, true);
      }
    }
  }
}
