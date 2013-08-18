package com.amd.aparapi.internal.model;

import java.util.Map;

public class ConstantPoolMergeHandler {
   private ClassModel.ConstantPool constantPool;
   private Map<Integer, Integer> updateMap;

   private int lastFreeConstantPoolEntry = 0;

   public ConstantPoolMergeHandler(ClassModel.ConstantPool constantPool, Map<Integer, Integer> updateMap) {
      this.constantPool = constantPool;
      this.updateMap = updateMap;
   }

   public int slotFor(int currentSlot) {
      if (updateMap.containsKey(currentSlot)) return updateMap.get(currentSlot);

      int newSlot = getNextFreeConstantPoolSlot();
      updateMap.put(currentSlot, newSlot);

      return newSlot;
   }

   private int getNextFreeConstantPoolSlot() {
      int i = lastFreeConstantPoolEntry + 1;
      while(constantPoolContainsIndex(i)) {
         i++;
      }
      lastFreeConstantPoolEntry = i;
      return i;
   }

   private boolean constantPoolContainsIndex(int index) {
      for (ClassModel.ConstantPool.Entry entry : constantPool.getEntries()) {
         if (entry.getSlot() == index) return true;
      }
      return false;
   }
}
