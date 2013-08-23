package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.annotation.InlineClass;
import com.amd.aparapi.internal.kernel.KernelRunner;

@InlineClass
public class LinearGrid {
   private final int dimX;
   private final int dimY;
   private final int dimZ;
   private final int size;
   private double[] values;

   public LinearGrid(int dimX, int dimY, int dimZ) {
      this.size = dimX * dimY * dimZ;

      this.dimX = dimX;
      this.dimY = dimY;
      this.dimZ = dimZ;

      values = new double[size];
   }

   public void put(KernelRunner kernelRunner) {
      kernelRunner.put(values);
   }

   public void get(KernelRunner kernelRunner) {
      kernelRunner.get(values);
   }

   public void putAt(int x, int y, int z, double value) {
      int index = x * dimY * dimZ + y * dimZ + z;
      values[index] = value;
   }

   public double valueAt(int x, int y, int z) {
      int index = x * dimY * dimZ + y * dimZ + z;
      return values[index];
   }

   public int getSize() {
      return size;
   }

   public int getDimX() {
      return dimX;
   }

   public int getDimY() {
      return dimY;
   }

   public int getDimZ() {
      return dimZ;
   }
}
