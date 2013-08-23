package com.amd.aparapi.test.runtime.objectOfArrays;

import com.amd.aparapi.Kernel;

public class LinearGridKernel extends Kernel {

   private LinearGrid grid1;
   private LinearGrid grid2;

   public LinearGridKernel(LinearGrid grid1, LinearGrid grid2) {
      this.grid1 = grid1;
      this.grid2 = grid2;
   }

   @Override
   public void run() {
      int globalId = getGlobalId();

      if (globalId > grid1.getSize()) return;

      int dimY = grid1.getDimY();
      int dimZ = grid1.getDimZ();
      int dimX = grid1.getDimX();

      int pos_x = (globalId / (dimY * dimZ)) % dimX;
      int pos_y = (globalId / dimZ) % dimY;
      int pos_z = globalId % dimZ;

      double value1 = grid1.valueAt(pos_x, pos_y, pos_z);
      double value2 = grid2.valueAt(pos_x, pos_y, pos_z);
      grid1.putAt(pos_x, pos_y, pos_z, value1 + value2);
   }
}
