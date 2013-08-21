package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.Kernel;

public class HolderSumKernel extends Kernel {
   private final int size;
   private final Holder target;
   private Holder holder1;
   private Holder holder2;

   public HolderSumKernel(Holder holder1, Holder holder2, Holder target, int size) {
      this.holder1 = holder1;
      this.holder2 = holder2;
      this.target = target;
      this.size = size;
   }

   @Override
   public void run() {
      int globalId = getGlobalId();
      if (globalId >= size) return;

      target.setAt(globalId, holder1.itemAt(globalId) + holder2.itemAt(globalId));
   }
}
