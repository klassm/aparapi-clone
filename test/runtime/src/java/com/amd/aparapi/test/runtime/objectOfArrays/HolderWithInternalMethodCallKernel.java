package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.Kernel;

public class HolderWithInternalMethodCallKernel extends Kernel {
   private HolderWithInternalMethodCall holder;
   private int size;

   public HolderWithInternalMethodCallKernel(HolderWithInternalMethodCall holder, int size) {
      this.holder = holder;
      this.size = size;
   }

   @Override
   public void run() {
      int globalId = getGlobalId();
      if (globalId >= size) return;

      holder.doSomethingWith(globalId);
   }
}
