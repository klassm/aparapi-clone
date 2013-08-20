package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.EXECUTION_MODE;
import com.amd.aparapi.internal.kernel.KernelRunner;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ObjectOfArraysTest {

   @Test
   public void testObjectOfArrays() {
      KernelRunner kernelRunner = new KernelRunner();

      int[] data = new int[] { 3, 5, 7, 9, 11, 12};
      int[] dataCopy = Arrays.copyOf(data, data.length);

      Holder holder = new Holder(data);
      holder.put(kernelRunner);

      ObjectOfArraysIncrementKernel kernel = new ObjectOfArraysIncrementKernel(holder, data.length);
      kernelRunner.execute(kernel, data.length);

      holder.get(kernelRunner);

      for (int i = 0; i < dataCopy.length; i++) {
         assertThat(data[i], is(dataCopy[i] + 1));
      }

      assertThat(kernelRunner.getExecutionMode(), is(EXECUTION_MODE.GPU));
   }
}
