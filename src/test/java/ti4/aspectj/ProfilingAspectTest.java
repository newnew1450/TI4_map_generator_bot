package ti4.aspectj;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProfilingAspectTest {

  @Test
  public void test() {
    TestClass testClass = new TestClass();
    testClass.test();
  }

}