package com.amd.aparapi.test.runtime;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.internal.kernel.KernelRunner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExplicitTransferTest {

   private class ExplicitKernel extends Kernel {
      private int[] values;

      private ExplicitKernel(int[] values) {
         this.values = values;
      }

      @Override
      public void run() {
         values[getGlobalId()] += 1;
      }
   }

   @Test
   public void testExplicitPutGet() {
      int[] values = new int[512];
      for (int i = 0; i < values.length; i++) values[i] = 1;

      KernelRunner kernelRunner = new KernelRunner();
      kernelRunner.setExplicit(true);

      kernelRunner.put(values);
      ExplicitKernel kernel = new ExplicitKernel(values);

      kernelRunner.execute(kernel, values.length);
      kernelRunner.get(values);

      for (int value : values) {
         assertEquals(2, value);
      }

      kernelRunner.execute(kernel, values.length);
      for (int value : values) {
         assertEquals(2, value);
      }

      kernelRunner.get(values);
      for (int value : values) {
         assertEquals(3, value);
      }
   }
}
