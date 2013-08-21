package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.EXECUTION_MODE;
import com.amd.aparapi.internal.kernel.KernelRunner;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ObjectOfArraysTestMultipleHolder {

   @Test
   public void testObjectOfArrays() {
      KernelRunner kernelRunner = new KernelRunner();
      kernelRunner.setExplicit(true);

      int[] data1 = new int[] { 3, 5, 7, 9, 11, 12};
      int[] data2 = Arrays.copyOf(data1, data1.length);

      Holder holder1 = new Holder(data1);
      holder1.put(kernelRunner);

      Holder holder2 = new Holder(data2);
      holder2.put(kernelRunner);

      Holder target = new Holder(new int[data1.length]);
      target.put(kernelRunner);

      HolderSumKernel kernel = new HolderSumKernel(holder1, holder2, target, data1.length);
      kernelRunner.execute(kernel, data1.length);

      target.get(kernelRunner);

      for (int i = 0; i < data1.length; i++) {
         int i1 = data1[i];
         int i2 = data2[i];

         assertThat(target.getData()[i], is(i1 + i2));
      }

      assertThat(kernelRunner.getExecutionMode(), is(EXECUTION_MODE.GPU));
   }
}
