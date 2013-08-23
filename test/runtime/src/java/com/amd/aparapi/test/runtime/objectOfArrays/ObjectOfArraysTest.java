package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.EXECUTION_MODE;
import com.amd.aparapi.internal.kernel.KernelRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ObjectOfArraysTest {

   private KernelRunner kernelRunner;

   @Before
   public void before() {
      kernelRunner = new KernelRunner();
      kernelRunner.setExplicit(true);
   }

   @Test
   public void testObjectOfArrays() {
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

   @Test
   public void testObjectOfArraysMultipleHolder() {
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

   @Test
   public void testObjectOfArraysHolderWithInternalMethodCalls() {
      int[] data1 = new int[] { 3, 5, 7, 9, 11, 12};

      HolderWithInternalMethodCall holder = new HolderWithInternalMethodCall(data1);
      holder.put(kernelRunner);

      kernelRunner.execute(new HolderWithInternalMethodCallKernel(holder, data1.length), data1.length);

      holder.get(kernelRunner);

      assertThat(kernelRunner.getExecutionMode(), is(EXECUTION_MODE.GPU));
   }

   @Test
   public void testLinearGrid() {
      double[] values1 = new double[10 * 10 * 10];
      for (int i = 0; i < values1.length; i++) values1[i] = 1;

      double[] values2 = Arrays.copyOf(values1, values1.length);

      LinearGrid grid1 = new LinearGrid(10, 10, 10, values1);
      LinearGrid grid2 = new LinearGrid(10, 10, 10, values2);
      grid1.put(kernelRunner);
      grid2.put(kernelRunner);

      LinearGridKernel kernel = new LinearGridKernel(grid1, grid2);
      kernelRunner.execute(kernel, grid1.getSize());

      grid1.get(kernelRunner);

      for (int x = 0; x < grid1.getDimY(); x++) {
         for (int y = 0; y < grid1.getDimY(); y++) {
            for (int z = 0; z < grid1.getDimZ(); z++) {
               assertEquals(grid1.valueAt(x, y, z), 2, 0.0001);
            }
         }
      }

      assertThat(kernelRunner.getExecutionMode(), is(EXECUTION_MODE.GPU));
   }

   @After
   public void after() {
      kernelRunner.dispose();
   }
}
