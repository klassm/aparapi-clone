package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.Kernel;

public class LinearGridKernel extends Kernel {

   private LinearGrid grid;

   public LinearGridKernel(LinearGrid grid) {
      this.grid = grid;
   }

   @Override
   public void run() {
      int globalId = getGlobalId();

      if (globalId > grid.getSize()) return;

      int dimY = grid.getDimY();
      int dimZ = grid.getDimZ();
      int dimX = grid.getDimX();

      int pos_x = (globalId / (dimY * dimZ)) % dimX;
      int pos_y = (globalId / dimZ) % dimY;
      int pos_z = globalId % dimZ;

      double value = grid.valueAt(pos_x, pos_y, pos_z);
      grid.putAt(pos_x, pos_y, pos_z, value + 1);
   }
}
