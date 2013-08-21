package com.amd.aparapi.internal.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MethodModelNameWrapper implements InvocationHandler {
   private final String methodNamePrefix;
   private MethodModel model;

   public MethodModelNameWrapper(MethodModel model, String methodNamePrefix) {
      this.model = model;
      this.methodNamePrefix = methodNamePrefix;
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("getName")) {
         return methodNamePrefix + method.invoke(model, args);
      }
      return method.invoke(model, args);
   }

   public static MethodModel wrap(MethodModel model, String methodNamePrefix) {
      MethodModelNameWrapper wrapper = new MethodModelNameWrapper(model, methodNamePrefix);
      return (MethodModel)
            Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{MethodModel.class}, wrapper);
   }
}
