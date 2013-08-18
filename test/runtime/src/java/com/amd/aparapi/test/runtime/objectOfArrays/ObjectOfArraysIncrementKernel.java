package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.Kernel;

public class ObjectOfArraysIncrementKernel extends Kernel {
   private final int size;
   private Holder holder;

   public ObjectOfArraysIncrementKernel(Holder holder, int size) {
      this.holder = holder;
      this.size = size;
   }

   @Override
   public void run() {
      int id = getGlobalId();
      if (id >= size) return;

      int value = holder.getAt(id);
      holder.setAt(id, value + 1);
   }
}
