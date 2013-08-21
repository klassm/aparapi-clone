package com.amd.aparapi.internal.model;

public class VirtualMethod {
   private MethodModelRaw methodModel;
   private VirtualMethodEntry virtualMethodEntry;
   private String accessFieldName;

   public VirtualMethod(MethodModelRaw methodModel, VirtualMethodEntry virtualMethodEntry, String accessFieldName) {
      this.methodModel = methodModel;
      this.virtualMethodEntry = virtualMethodEntry;
      this.accessFieldName = accessFieldName;
   }

   @Override
   public boolean equals(Object obj) {
      if (! (obj instanceof VirtualMethod)) return false;

      VirtualMethod method = (VirtualMethod) obj;

      return methodModel.getName().equals(method.methodModel.getName()) &&
            virtualMethodEntry.equals(method.virtualMethodEntry) &&
            accessFieldName.equals(method.accessFieldName);
   }

   public MethodModelRaw getMethodModel() {
      return methodModel;
   }

   public VirtualMethodEntry getVirtualMethodEntry() {
      return virtualMethodEntry;
   }

   public String getAccessFieldName() {
      return accessFieldName;
   }

   public String getAccessPath() {
      return virtualMethodEntry.getCallPath() + "_" + accessFieldName;
   }

   public MethodModel getNameWrappedMethodModel() {
      return MethodModelNameWrapper.wrap(methodModel, getAccessPath());
   }
}
