package com.amd.aparapi;

import com.amd.aparapi.internal.kernel.KernelArg;
import com.amd.aparapi.internal.model.Entrypoint;

import java.util.ArrayList;
import java.util.List;

public class KernelMapping {
   public final Class<? extends Kernel> kernelClass;
   public final List<KernelArg> kernelArgs = new ArrayList<KernelArg>();
   public final Entrypoint entryPoint;

   public KernelMapping(Class<? extends Kernel> kernelClass, Entrypoint entryPoint) {
      this.kernelClass = kernelClass;
      this.entryPoint = entryPoint;
   }

   public KernelArg[] kernelArgsAsArray() {
      KernelArg[] args = new KernelArg[kernelArgs.size()];
      return kernelArgs.toArray(args);
   }
}
