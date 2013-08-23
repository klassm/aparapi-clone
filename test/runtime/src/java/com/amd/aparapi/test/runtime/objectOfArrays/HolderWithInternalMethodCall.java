package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.annotation.InlineClass;
import com.amd.aparapi.internal.kernel.KernelRunner;

@InlineClass
public class HolderWithInternalMethodCall {
   public int[] data;

   public HolderWithInternalMethodCall(int[] data) {
      this.data = data;
   }

   public void doSomethingWith(int id) {
      int value = data[id];
      value = handle(value);
      data[id] = value;
   }

   private int handle(int element) {
      return element * 5 + 3 - 5;
   }

   public void put(KernelRunner kernelRunner) {
      kernelRunner.put(data);
   }

   public void get(KernelRunner kernelRunner) {
      kernelRunner.get(data);
   }
}
