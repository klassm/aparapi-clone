package com.amd.aparapi.test.runtime;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import com.amd.aparapi.internal.kernel.KernelRunner;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;

public class BufferTransfer{

   static OpenCLDevice openCLDevice = null;

   @BeforeClass public static void setUpBeforeClass() throws Exception {

      Device device = Device.best();
      if (device == null || !(device instanceof OpenCLDevice)) {
         fail("no opencl device!");
      }
      openCLDevice = (OpenCLDevice) device;
   }

   public static class InOutKernel extends Kernel{

      int[] in;

      int[] out;

      @Override public void run() {
         int gid = getGlobalId(0);
         in[gid] = out[gid];

      }

   }

   @Test public void inOutOnce() {

      final int SIZE = 1024;
      final InOutKernel kernel = new InOutKernel();
      final Range range = openCLDevice.createRange(SIZE);

      kernel.in = new int[SIZE];
      kernel.out = new int[SIZE];

      Util.fill(kernel.in, new Util.Filler(){
         public void fill(int[] array, int index) {
            array[index] = index;
         }
      });

      KernelRunner kernelRunner = new KernelRunner();
      kernelRunner.execute(kernel, range);

      assertTrue("in == out", Util.same(kernel.in, kernel.out));

      kernelRunner.dispose();
   }

   public static class AddKernel extends Kernel{

      int[] values;

      int[] result;

      @Override public void run() {
         int gid = getGlobalId(0);
         result[gid] = result[gid] + values[gid];

      }

   }

   @Test public void auto() {
      KernelRunner kernelRunner = new KernelRunner();

      final int SIZE = 1024;
      final AddKernel kernel = new AddKernel();
      final Range range = openCLDevice.createRange(SIZE);

      kernel.values = new int[SIZE];
      kernel.result = new int[SIZE];
      Util.zero(kernel.result);
      Util.fill(kernel.values, new Util.Filler(){
         public void fill(int[] array, int index) {
            array[index] = index;
         }
      });

      int[] expectedResult = Arrays.copyOf(kernel.result, kernel.result.length);

      Util.apply(expectedResult, kernel.values, new Util.Operator(){

         @Override public void apply(int[] lhs, int[] rhs, int index) {
            lhs[index] = lhs[index] + rhs[index];

         }
      });
      kernelRunner.execute(kernel, range);

      assertTrue("expectedResult == result", Util.same(expectedResult, kernel.result));

      kernelRunner.execute(kernel, range);

      Util.apply(expectedResult, kernel.values, new Util.Operator(){

         @Override public void apply(int[] lhs, int[] rhs, int index) {
            lhs[index] = lhs[index] + rhs[index];

         }
      });
      assertTrue("expectedResult == result", Util.same(expectedResult, kernel.result));

      Util.zero(kernel.values);
      kernelRunner.execute(kernel, range);
      assertTrue("expectedResult == result", Util.same(expectedResult, kernel.result));

      kernelRunner.dispose();
   }

   @Test public void explicit() {
      KernelRunner kernelRunner = new KernelRunner();
      kernelRunner.setExplicit(true);

      final int SIZE = 1024;
      final AddKernel kernel = new AddKernel();
      final Range range = openCLDevice.createRange(SIZE);

      kernel.values = new int[SIZE];
      kernel.result = new int[SIZE];
      Util.zero(kernel.result);
      Util.fill(kernel.values, new Util.Filler(){
         public void fill(int[] array, int index) {
            array[index] = index;
         }
      });

      int[] expectedResult = Arrays.copyOf(kernel.result, kernel.result.length);

      Util.apply(expectedResult, kernel.values, new Util.Operator(){

         @Override public void apply(int[] lhs, int[] rhs, int index) {
            lhs[index] = lhs[index] + rhs[index];

         }
      });

      kernelRunner.execute(kernel, range);
      kernelRunner.get(kernel.result);

      assertTrue("after first explicit add expectedResult == result", Util.same(expectedResult, kernel.result));

      kernelRunner.execute(kernel, range).get(kernel.result);

      Util.apply(expectedResult, kernel.values, new Util.Operator(){
         @Override public void apply(int[] lhs, int[] rhs, int index) {
            lhs[index] = lhs[index] + rhs[index];

         }
      });
      assertTrue("after second explicit add expectedResult == result", Util.same(expectedResult, kernel.result));

      Util.zero(kernel.values);

      kernelRunner.put(kernel.values).execute(kernel, range).get(kernel.result);

      assertTrue("after zeroing values and third explici add expectedResult == result", Util.same(expectedResult, kernel.result));

      Util.zero(kernel.result);

      kernelRunner.put(kernel.result).execute(kernel, range).get(kernel.result);

      Util.zero(expectedResult);

      assertTrue("after zeroing values and result and forth  explicit add expectedResult == result",
            Util.same(expectedResult, kernel.result));

      kernelRunner.dispose();
   }

   private class TestKernel extends Kernel{
      int[] simStep = new int[1];

      int[] neuronOutputs = new int[3];

      int[] expected = new int[] {
            3,
            0,
            0,
            0,
            3,
            0,
            0,
            0,
            3,
            0,
            0,
            0,
            3,
            0,
            0,
            0
      };

      public void step(KernelRunner kernelRunner) {
         int simSteps = 16;
         int[][] log = new int[neuronOutputs.length][simSteps];
         kernelRunner.put(neuronOutputs);
         for (simStep[0] = 0; simStep[0] < simSteps; simStep[0]++) {
            kernelRunner.put(simStep).execute(this, neuronOutputs.length).get(neuronOutputs);
            for (int n = 0; n < neuronOutputs.length; n++)
               log[n][simStep[0]] = neuronOutputs[n];
         }
         System.out.println(kernelRunner.getExecutionMode() + (kernelRunner.isExplicit() ? ", explicit" : ", auto"));

         for (int n = 0; n < neuronOutputs.length; n++)
            System.out.println(Arrays.toString(log[n]));

         assertTrue("log[2] == expected", Util.same(log[2], expected));
      }

      @Override public void run() {
         int neuronID = getGlobalId();
         neuronOutputs[neuronID] = (simStep[0] % (neuronID + 2) == 0) ? (neuronID + 1) : 0;
      }
   }

   @Test public void issue60Explicit() {

      KernelRunner kernelRunner = new KernelRunner();
      TestKernel kernel = new TestKernel();
      kernelRunner.setExplicit(true);
      kernel.step(kernelRunner);
      kernelRunner.dispose();

   }

   @Test public void issue60Auto() {
      KernelRunner kernelRunner = new KernelRunner();
      TestKernel kernel = new TestKernel();
      kernel.step(kernelRunner);
      kernelRunner.dispose();

   }

}
