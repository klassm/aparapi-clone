package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.internal.kernel.KernelRunner;

public class Holder {
   private int[] data;

   public Holder(int[] data) {
      this.data = data;
   }

   public int getAt(int index) {
      return data[index];
   }

   public void setAt(int index, int toSet) {
      data[index] = toSet;
   }

   public void put(KernelRunner kernelRunner) {
      kernelRunner.put(data);
   }

   public void get(KernelRunner kernelRunner) {
      kernelRunner.get(data);
   }
}
