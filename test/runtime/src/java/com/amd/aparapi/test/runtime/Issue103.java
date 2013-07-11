package com.amd.aparapi.test.runtime;

import com.amd.aparapi.*;
import static org.junit.Assert.assertTrue;

import com.amd.aparapi.internal.kernel.KernelRunner;
import org.junit.Test;



public class Issue103 extends Kernel {
   static final int size = 32;
   
   static int[] source = new int[size];
   static int[] target = new int[size];

   @Override
   public void run() {
      int id = getGlobalId();
      target[id] = source[id];
   }

   void validate() {
      for (int i = 0; i < size; i++) {
         System.out.println(target[i] + " ... " + source[i]);
         assertTrue("target == source", target[i] == source[i]);
      }
   }
   
   @Test public void test() {
      KernelRunner kernelRunner = new KernelRunner();
      kernelRunner.execute(this, size);
      validate();
      kernelRunner.dispose();
   }
   
   public static void main(String[] args) {
      Issue103 b = new Issue103();
      b.test();
   }

   public Issue103() {
      for(int i = 0; i < size; ++i) {
         source[i] = 7;
         target[i] = 99;
      }
   }
}
