package com.amd.aparapi.test.runtime;

import com.amd.aparapi.EXECUTION_MODE;
import com.amd.aparapi.Kernel;
import com.amd.aparapi.internal.kernel.KernelRunner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MultipleKernelCall {

   class ArrayIncrement extends Kernel {
      public int[] someArray;

      ArrayIncrement(int[] someArray) {
         this.someArray = someArray;
      }

      public void run() {
         int index = getGlobalId();
         someArray[index] += 2;
      }
   }

   class ArrayDecrement extends Kernel {
      public int[] someArray;

      ArrayDecrement(int[] someArray) {
         this.someArray = someArray;
      }

      public void run() {
         int index = getGlobalId();
         someArray[index] -= 1;
      }
   }

   @Test
   public void testMultipleKernelCall() {
      KernelRunner kernelRunner = new KernelRunner();
      kernelRunner.setExecutionMode(EXECUTION_MODE.GPU);

      int[] values = new int[1000];
      for (int i = 0; i < values.length; i++) {
         values[i] = 10;
      }

      kernelRunner.execute(new ArrayIncrement(values), values.length);
      assertValuesGet(values, 10 + 2);

      kernelRunner.execute(new ArrayDecrement(values), values.length);
      assertValuesGet(values, 10 + 2 - 1);

      assertEquals(kernelRunner.getExecutionMode(), EXECUTION_MODE.GPU);
   }

   @Test
   public void testMultipleKernelCallExplicit() {
      KernelRunner kernelRunner = new KernelRunner();
      kernelRunner.setExplicit(true);
      kernelRunner.setExecutionMode(EXECUTION_MODE.GPU);

      int[] values = new int[1000];
      for (int i = 0; i < values.length; i++) {
         values[i] = 10;
      }
      kernelRunner.put(values);

      kernelRunner.execute(new ArrayIncrement(values), values.length);
      kernelRunner.get(values);
      assertValuesGet(values, 10 + 2);

      kernelRunner.execute(new ArrayDecrement(values), values.length);
      kernelRunner.get(values);
      assertValuesGet(values, 10 + 2 - 1);

      assertEquals(kernelRunner.getExecutionMode(), EXECUTION_MODE.GPU);
   }

   @Test
   public void testMultipleKernelCallExplicitMultipleRuns() {
      KernelRunner kernelRunner = new KernelRunner();
      kernelRunner.setExplicit(true);
      kernelRunner.setExecutionMode(EXECUTION_MODE.GPU);


      int[] values = new int[20000];
      for (int i = 0; i < values.length; i++) {
         values[i] = 10;
      }
      kernelRunner.put(values);

      ArrayDecrement decrement = new ArrayDecrement(values);
      ArrayIncrement increment = new ArrayIncrement(values);

      int runs = 10000;
      for (int r = 0; r < runs; r++) {
         kernelRunner.execute(increment, values.length);
         kernelRunner.execute(decrement, values.length);

         assertEquals(kernelRunner.getExecutionMode(), EXECUTION_MODE.GPU);
      }

      kernelRunner.get(values);
      assertValuesGet(values, 10 + runs);
   }

   private void assertValuesGet(int[] values, int expected) {
      for (int value : values) {
         assertEquals(expected, value);
      }
   }
}
