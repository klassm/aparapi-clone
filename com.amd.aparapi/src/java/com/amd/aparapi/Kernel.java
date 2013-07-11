/*
Copyright (c) 2010-2011, Advanced Micro Devices, Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following
disclaimer. 

Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution. 

Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
derived from this software without specific prior written permission. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

If you use the software (in whole or in part), you shall adhere to all applicable U.S., European, and other export
laws, including but not limited to the U.S. Export Administration Regulations ("EAR"), (15 C.F.R. Sections 730 through
774), and E.U. Council Regulation (EC) No 1334/2000 of 22 June 2000.  Further, pursuant to Section 740.6 of the EAR,
you hereby certify that, except pursuant to a license granted by the United States Department of Commerce Bureau of 
Industry and Security or as otherwise permitted pursuant to a License Exception under the U.S. Export Administration 
Regulations ("EAR"), you will not (1) export, re-export or release to a national of a country in Country Groups D:1,
E:1 or E:2 any restricted technology, software, or source code you receive hereunder, or (2) export to Country Groups
D:1, E:1 or E:2 the direct product of such technology or software, if such foreign produced direct product is subject
to national security controls as identified on the Commerce Control List (currently found in Supplement 1 to Part 774
of EAR).  For the most current Country Group listings, or for additional information about the EAR or your obligations
under those regulations, please refer to the U.S. Bureau of Industry and Security's website at http://www.bis.doc.gov/. 

*/
package com.amd.aparapi;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.amd.aparapi.annotation.Experimental;
import com.amd.aparapi.annotation.OpenCLDelegate;
import com.amd.aparapi.annotation.OpenCLMapping;
import com.amd.aparapi.exception.DeprecatedException;
import com.amd.aparapi.internal.model.ClassModel.ConstantPool.MethodReferenceEntry;

/**
 * A <i>kernel</i> encapsulates a data parallel algorithm that will execute either on a GPU
 * (through conversion to OpenCL) or on a CPU via a Java Thread Pool. 
 * <p>
 * To write a new kernel, a developer extends the <code>Kernel</code> class and overrides the <code>Kernel.run()</code> method.
 * To execute this kernel, the developer creates a new instance of it and calls <code>Kernel.execute(int globalSize)</code> with a suitable 'global size'. At runtime
 * Aparapi will attempt to convert the <code>Kernel.run()</code> method (and any method called directly or indirectly
 * by <code>Kernel.run()</code>) into OpenCL for execution on GPU devices made available via the OpenCL platform. 
 * <p>
 * Note that <code>Kernel.run()</code> is not called directly. Instead, 
 * the <code>Kernel.execute(int globalSize)</code> method will cause the overridden <code>Kernel.run()</code> 
 * method to be invoked once for each value in the range <code>0...globalSize</code>.
 * <p>
 * On the first call to <code>Kernel.execute(int _globalSize)</code>, Aparapi will determine the EXECUTION_MODE of the kernel. 
 * This decision is made dynamically based on two factors:
 * <ol>
 * <li>Whether OpenCL is available (appropriate drivers are installed and the OpenCL and Aparapi dynamic libraries are included on the system path).</li>
 * <li>Whether the bytecode of the <code>run()</code> method (and every method that can be called directly or indirectly from the <code>run()</code> method)
 *  can be converted into OpenCL.</li>
 * </ol>
 * <p>
 * Below is an example Kernel that calculates the square of a set of input values.
 * <p>
 * <blockquote><pre>
 *     class SquareKernel extends Kernel{
 *         private int values[];
 *         private int squares[];
 *         public SquareKernel(int values[]){
 *            this.values = values;
 *            squares = new int[values.length];
 *         }
 *         public void run() {
 *             int gid = getGlobalID();
 *             squares[gid] = values[gid]*values[gid];
 *         }
 *         public int[] getSquares(){
 *             return(squares);
 *         }
 *     }
 * </pre></blockquote>
 * <p>
 * To execute this kernel, first create a new instance of it and then call <code>execute(Range _range)</code>. 
 * <p>
 * <blockquote><pre>
 *     int[] values = new int[1024];
 *     // fill values array
 *     Range range = Range.create(values.length); // create a range 0..1024
 *     SquareKernel kernel = new SquareKernel(values);
 *     kernel.execute(range);
 * </pre></blockquote>
 * <p>
 * When <code>execute(Range)</code> returns, all the executions of <code>Kernel.run()</code> have completed and the results are available in the <code>squares</code> array.
 * <p>
 * <blockquote><pre>
 *     int[] squares = kernel.getSquares();
 *     for (int i=0; i< values.length; i++){
 *        System.out.printf("%4d %4d %8d\n", i, values[i], squares[i]);
 *     }
 * </pre></blockquote>
 * <p>
 * A different approach to creating kernels that avoids extending Kernel is to write an anonymous inner class:
 * <p>
 * <blockquote><pre>
 *   
 *     final int[] values = new int[1024];
 *     // fill the values array 
 *     final int[] squares = new int[values.length];
 *     final Range range = Range.create(values.length);
 *   
 *     Kernel kernel = new Kernel(){
 *         public void run() {
 *             int gid = getGlobalID();
 *             squares[gid] = values[gid]*values[gid];
 *         }
 *     };
 *     kernel.execute(range);
 *     for (int i=0; i< values.length; i++){
 *        System.out.printf("%4d %4d %8d\n", i, values[i], squares[i]);
 *     }
 *     
 * </pre></blockquote>
 * <p>
 *
 * @author  gfrost AMD Javalabs
 * @version Alpha, 21/09/2010
 */
public abstract class Kernel implements Cloneable {

   private KernelState kernelState = new KernelState();

   /**
    * This class is for internal Kernel state management<p>
    * NOT INTENDED FOR USE BY USERS
    */
   public final class KernelState {

      private int[] globalIds = new int[] {
            0,
            0,
            0
      };

      private int[] localIds = new int[] {
            0,
            0,
            0
      };

      private int[] groupIds = new int[] {
            0,
            0,
            0
      };

      private Range range;

      private int passId;

      private volatile CyclicBarrier localBarrier;

      /**
       * Default constructor
       */
      protected KernelState() {

      }

      /**
       * Copy constructor
       * 
       * @param kernelState
       */
      protected KernelState(KernelState kernelState) {
         globalIds = kernelState.getGlobalIds();
         localIds = kernelState.getLocalIds();
         groupIds = kernelState.getGroupIds();
         range = kernelState.getRange();
         passId = kernelState.getPassId();
         localBarrier = kernelState.getLocalBarrier();
      }

      /**
       * @return the globalIds
       */
      public int[] getGlobalIds() {
         return globalIds;
      }

      /**
       * @param globalIds the globalIds to set
       */
      public void setGlobalIds(int[] globalIds) {
         this.globalIds = globalIds;
      }

      /**
       * Set a specific index value
       * 
       * @param _index
       * @param value
       */
      public void setGlobalId(int _index, int value) {
         globalIds[_index] = value;
      }

      /**
       * @return the localIds
       */
      public int[] getLocalIds() {
         return localIds;
      }

      /**
       * @param localIds the localIds to set
       */
      public void setLocalIds(int[] localIds) {
         this.localIds = localIds;
      }

      /**
       * Set a specific index value
       * 
       * @param _index
       * @param value
       */
      public void setLocalId(int _index, int value) {
         localIds[_index] = value;
      }

      /**
       * @return the groupIds
       */
      public int[] getGroupIds() {
         return groupIds;
      }

      /**
       * @param groupIds the groupIds to set
       */
      public void setGroupIds(int[] groupIds) {
         this.groupIds = groupIds;
      }

      /**
       * Set a specific index value
       * 
       * @param _index
       * @param value
       */
      public void setGroupId(int _index, int value) {
         groupIds[_index] = value;
      }

      /**
       * @return the range
       */
      public Range getRange() {
         return range;
      }

      /**
       * @param range the range to set
       */
      public void setRange(Range range) {
         this.range = range;
      }

      /**
       * @return the passId
       */
      public int getPassId() {
         return passId;
      }

      /**
       * @param passId the passId to set
       */
      public void setPassId(int passId) {
         this.passId = passId;
      }

      /**
       * @return the localBarrier
       */
      public CyclicBarrier getLocalBarrier() {
         return localBarrier;
      }

      /**
       * @param localBarrier the localBarrier to set
       */
      public void setLocalBarrier(CyclicBarrier localBarrier) {
         this.localBarrier = localBarrier;
      }
   }

   /**
    * Determine the globalId of an executing kernel.
    * <p>
    * The kernel implementation uses the globalId to determine which of the executing kernels (in the global domain space) this invocation is expected to deal with. 
    * <p>
    * For example in a <code>SquareKernel</code> implementation:
    * <p>
    * <blockquote><pre>
    *     class SquareKernel extends Kernel{
    *         private int values[];
    *         private int squares[];
    *         public SquareKernel(int values[]){
    *            this.values = values;
    *            squares = new int[values.length];
    *         }
    *         public void run() {
    *             int gid = getGlobalID();
    *             squares[gid] = values[gid]*values[gid];
    *         }
    *         public int[] getSquares(){
    *             return(squares);
    *         }
    *     }
    * </pre></blockquote>
    * <p>
    * Each invocation of <code>SquareKernel.run()</code> retrieves it's globalId by calling <code>getGlobalId()</code>, and then computes the value of <code>square[gid]</code> for a given value of <code>value[gid]</code>.
    * <p> 
    * @return The globalId for the Kernel being executed
    * 
    * @see #getLocalId()
    * @see #getGroupId()
    * @see #getGlobalSize()
    * @see #getNumGroups()
    * @see #getLocalSize()
    */

   @OpenCLDelegate
   protected final int getGlobalId() {
      return getGlobalId(0);
   }

   @OpenCLDelegate
   protected final int getGlobalId(int _dim) {
      return kernelState.getGlobalIds()[_dim];
   }

   /*
      @OpenCLDelegate protected final int getGlobalX() {
         return (getGlobalId(0));
      }

      @OpenCLDelegate protected final int getGlobalY() {
         return (getGlobalId(1));
      }

      @OpenCLDelegate protected final int getGlobalZ() {
         return (getGlobalId(2));
      }
   */
   /**
    * Determine the groupId of an executing kernel.
    * <p>
    * When a <code>Kernel.execute(int globalSize)</code> is invoked for a particular kernel, the runtime will break the work into various 'groups'.
    * <p>
    * A kernel can use <code>getGroupId()</code> to determine which group a kernel is currently 
    * dispatched to
    * <p>
    * The following code would capture the groupId for each kernel and map it against globalId.
    * <blockquote><pre>
    *     final int[] groupIds = new int[1024];
    *     Kernel kernel = new Kernel(){
    *         public void run() {
    *             int gid = getGlobalId();
    *             groupIds[gid] = getGroupId();
    *         }
    *     };
    *     kernel.execute(groupIds.length);
    *     for (int i=0; i< values.length; i++){
    *        System.out.printf("%4d %4d\n", i, groupIds[i]);
    *     } 
    * </pre></blockquote>
    * 
    * @see #getLocalId()
    * @see #getGlobalId()
    * @see #getGlobalSize()
    * @see #getNumGroups()
    * @see #getLocalSize()
    * 
    * @return The groupId for this Kernel being executed
    */
   @OpenCLDelegate
   protected final int getGroupId() {
      return getGroupId(0);
   }

   @OpenCLDelegate
   protected final int getGroupId(int _dim) {
      return kernelState.getGroupIds()[_dim];
   }

   /*
      @OpenCLDelegate protected final int getGroupX() {
         return (getGroupId(0));
      }

      @OpenCLDelegate protected final int getGroupY() {
         return (getGroupId(1));
      }

      @OpenCLDelegate protected final int getGroupZ() {
         return (getGroupId(2));
      }
   */
   /**
    * Determine the passId of an executing kernel.
    * <p>
    * When a <code>Kernel.execute(int globalSize, int passes)</code> is invoked for a particular kernel, the runtime will break the work into various 'groups'.
    * <p>
    * A kernel can use <code>getPassId()</code> to determine which pass we are in.  This is ideal for 'reduce' type phases
    * 
    * @see #getLocalId()
    * @see #getGlobalId()
    * @see #getGlobalSize()
    * @see #getNumGroups()
    * @see #getLocalSize()
    * 
    * @return The groupId for this Kernel being executed
    */
   @OpenCLDelegate
   protected final int getPassId() {
      return kernelState.getPassId();
   }

   /**
    * Determine the local id of an executing kernel.
    * <p>
    * When a <code>Kernel.execute(int globalSize)</code> is invoked for a particular kernel, the runtime will break the work into
    * various 'groups'.
    * <code>getLocalId()</code> can be used to determine the relative id of the current kernel within a specific group.
    * <p>
    * The following code would capture the groupId for each kernel and map it against globalId.
    * <blockquote><pre>
    *     final int[] localIds = new int[1024];
    *     Kernel kernel = new Kernel(){
    *         public void run() {
    *             int gid = getGlobalId();
    *             localIds[gid] = getLocalId();
    *         }
    *     };
    *     kernel.execute(localIds.length);
    *     for (int i=0; i< values.length; i++){
    *        System.out.printf("%4d %4d\n", i, localIds[i]);
    *     } 
    * </pre></blockquote>
    * 
    * @see #getGroupId()
    * @see #getGlobalId()
    * @see #getGlobalSize()
    * @see #getNumGroups()
    * @see #getLocalSize()
    * 
    * @return The local id for this Kernel being executed
    */
   @OpenCLDelegate
   protected final int getLocalId() {
      return getLocalId(0);
   }

   @OpenCLDelegate
   protected final int getLocalId(int _dim) {
      return kernelState.getLocalIds()[_dim];
   }

   /*
      @OpenCLDelegate protected final int getLocalX() {
         return (getLocalId(0));
      }

      @OpenCLDelegate protected final int getLocalY() {
         return (getLocalId(1));
      }

      @OpenCLDelegate protected final int getLocalZ() {
         return (getLocalId(2));
      }
   */
   /**
    * Determine the size of the group that an executing kernel is a member of.
    * <p>
    * When a <code>Kernel.execute(int globalSize)</code> is invoked for a particular kernel, the runtime will break the work into
    * various 'groups'. <code>getLocalSize()</code> allows a kernel to determine the size of the current group.
    * <p>
    * Note groups may not all be the same size. In particular, if <code>(global size)%(# of compute devices)!=0</code>, the runtime can choose to dispatch kernels to 
    * groups with differing sizes. 
    * 
    * @see #getGroupId()
    * @see #getGlobalId()
    * @see #getGlobalSize()
    * @see #getNumGroups()
    * @see #getLocalSize()
    * 
    * @return The size of the currently executing group.
    */
   @OpenCLDelegate
   protected final int getLocalSize() {
      return kernelState.getRange().getLocalSize(0);
   }

   @OpenCLDelegate
   protected final int getLocalSize(int _dim) {
      return kernelState.getRange().getLocalSize(_dim);
   }

   /*
      @OpenCLDelegate protected final int getLocalWidth() {
         return (range.getLocalSize(0));
      }

      @OpenCLDelegate protected final int getLocalHeight() {
         return (range.getLocalSize(1));
      }

      @OpenCLDelegate protected final int getLocalDepth() {
         return (range.getLocalSize(2));
      }
   */
   /**
    * Determine the value that was passed to <code>Kernel.execute(int globalSize)</code> method.
    * 
    * @see #getGroupId()
    * @see #getGlobalId()
    * @see #getNumGroups()
    * @see #getLocalSize()
    * 
    * @return The value passed to <code>Kernel.execute(int globalSize)</code> causing the current execution.
    */
   @OpenCLDelegate
   protected final int getGlobalSize() {
      return kernelState.getRange().getGlobalSize(0);
   }

   @OpenCLDelegate
   protected final int getGlobalSize(int _dim) {
      return kernelState.getRange().getGlobalSize(_dim);
   }

   /*
      @OpenCLDelegate protected final int getGlobalWidth() {
         return (range.getGlobalSize(0));
      }

      @OpenCLDelegate protected final int getGlobalHeight() {
         return (range.getGlobalSize(1));
      }

      @OpenCLDelegate protected final int getGlobalDepth() {
         return (range.getGlobalSize(2));
      }
   */
   /**
    * Determine the number of groups that will be used to execute a kernel
    * <p>
    * When <code>Kernel.execute(int globalSize)</code> is invoked, the runtime will split the work into
    * multiple 'groups'. <code>getNumGroups()</code> returns the total number of groups that will be used.
    * 
    * @see #getGroupId()
    * @see #getGlobalId()
    * @see #getGlobalSize()
    * @see #getNumGroups()
    * @see #getLocalSize()
    * 
    * @return The number of groups that kernels will be dispatched into.
    */
   @OpenCLDelegate
   protected final int getNumGroups() {
      return kernelState.getRange().getNumGroups(0);
   }

   @OpenCLDelegate
   protected final int getNumGroups(int _dim) {
      return kernelState.getRange().getNumGroups(_dim);
   }

   /*
      @OpenCLDelegate protected final int getNumGroupsWidth() {
         return (range.getGroups(0));
      }

      @OpenCLDelegate protected final int getNumGroupsHeight() {
         return (range.getGroups(1));
      }

      @OpenCLDelegate protected final int getNumGroupsDepth() {
         return (range.getGroups(2));
      }
   */
   /**
    * The entry point of a kernel. 
    *  
    * <p>
    * Every kernel must override this method.
    */
   public abstract void run();

   /**
    * When using a Java Thread Pool Aparapi uses clone to copy the initial instance to each thread. 
    *  
    * <p>
    * If you choose to override <code>clone()</code> you are responsible for delegating to <code>super.clone();</code>
    */
   @Override
   public Kernel clone() {
      try {
         final Kernel worker = (Kernel) super.clone();

         // We need to be careful to also clone the KernelState
         worker.kernelState = worker.new KernelState(kernelState); // Qualified copy constructor

         worker.kernelState.setGroupIds(new int[] {
               0,
               0,
               0
         });

         worker.kernelState.setLocalIds(new int[] {
               0,
               0,
               0
         });

         worker.kernelState.setGlobalIds(new int[] {
               0,
               0,
               0
         });

         return worker;
      } catch (final CloneNotSupportedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         return (null);
      }
   }


   /**
    * Wait for all kernels in the current group to rendezvous at this call before continuing execution.
    * 
    * @annotion Experimental
    */
   @OpenCLDelegate
   @Experimental
   protected final void localBarrier() {
      try {
         kernelState.getLocalBarrier().await();
      } catch (final InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (final BrokenBarrierException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   /**
    * Wait for all kernels in the current group to rendezvous at this call before continuing execution.
    * 
    * 
    * Java version is identical to localBarrier()
    * 
    * @annotion Experimental
    * @deprecated
    */
   @OpenCLDelegate
   @Experimental
   @Deprecated
   protected final void globalBarrier() throws DeprecatedException {
      throw new DeprecatedException(
            "Kernel.globalBarrier() has been deprecated. It was based an incorrect understanding of OpenCL functionality.");
   }

   public KernelState getKernelState() {
      return kernelState;
   }

   final static Map<String, String> typeToLetterMap = new HashMap<String, String>();

   static {
      // only primitive types for now
      typeToLetterMap.put("double", "D");
      typeToLetterMap.put("float", "F");
      typeToLetterMap.put("int", "I");
      typeToLetterMap.put("long", "J");
      typeToLetterMap.put("boolean", "Z");
      typeToLetterMap.put("byte", "B");
      typeToLetterMap.put("char", "C");
      typeToLetterMap.put("short", "S");
      typeToLetterMap.put("void", "V");
   }

   private static String descriptorToReturnTypeLetter(String desc) {
      // find the letter after the closed parenthesis
      return desc.substring(desc.lastIndexOf(')') + 1);
   }

   private static String getReturnTypeLetter(Method meth) {
      final Class<?> retClass = meth.getReturnType();
      final String strRetClass = retClass.toString();
      final String mapping = typeToLetterMap.get(strRetClass);
      // System.out.println("strRetClass = <" + strRetClass + ">, mapping = " + mapping);
      return mapping;
   }

   public static String getMappedMethodName(MethodReferenceEntry _methodReferenceEntry) {
      String mappedName = null;
      final String name = _methodReferenceEntry.getNameAndTypeEntry().getNameUTF8Entry().getUTF8();
      for (final Method kernelMethod : Kernel.class.getDeclaredMethods()) {
         if (kernelMethod.isAnnotationPresent(OpenCLMapping.class)) {
            // ultimately, need a way to constrain this based upon signature (to disambiguate abs(float) from abs(int);
            // for Alpha, we will just disambiguate based on the return type
            if (false) {
               System.out.println("kernelMethod is ... " + kernelMethod.toGenericString());
               System.out.println("returnType = " + kernelMethod.getReturnType());
               System.out.println("returnTypeLetter = " + getReturnTypeLetter(kernelMethod));
               System.out.println("kernelMethod getName = " + kernelMethod.getName());
               System.out.println("methRefName = " + name + " descriptor = "
                     + _methodReferenceEntry.getNameAndTypeEntry().getDescriptorUTF8Entry().getUTF8());
               System.out
                     .println("descToReturnTypeLetter = "
                           + descriptorToReturnTypeLetter(_methodReferenceEntry.getNameAndTypeEntry().getDescriptorUTF8Entry()
                                 .getUTF8()));
            }
            if (_methodReferenceEntry.getNameAndTypeEntry().getNameUTF8Entry().getUTF8().equals(kernelMethod.getName())
                  && descriptorToReturnTypeLetter(_methodReferenceEntry.getNameAndTypeEntry().getDescriptorUTF8Entry().getUTF8())
                        .equals(getReturnTypeLetter(kernelMethod))) {
               final OpenCLMapping annotation = kernelMethod.getAnnotation(OpenCLMapping.class);
               final String mapTo = annotation.mapTo();
               if (!mapTo.equals("")) {
                  mappedName = mapTo;
                  // System.out.println("mapTo = " + mapTo);
               }
            }
         }
      }
      // System.out.println("... in getMappedMethodName, returning = " + mappedName);
      return (mappedName);
   }

   public static boolean isMappedMethod(MethodReferenceEntry methodReferenceEntry) {
      boolean isMapped = false;
      for (final Method kernelMethod : Kernel.class.getDeclaredMethods()) {
         if (kernelMethod.isAnnotationPresent(OpenCLMapping.class)) {
            if (methodReferenceEntry.getNameAndTypeEntry().getNameUTF8Entry().getUTF8().equals(kernelMethod.getName())) {

               // well they have the same name ;) 
               isMapped = true;
            }
         }
      }
      return (isMapped);
   }

   public static boolean isOpenCLDelegateMethod(MethodReferenceEntry methodReferenceEntry) {
      boolean isMapped = false;
      for (final Method kernelMethod : Kernel.class.getDeclaredMethods()) {
         if (kernelMethod.isAnnotationPresent(OpenCLDelegate.class)) {
            if (methodReferenceEntry.getNameAndTypeEntry().getNameUTF8Entry().getUTF8().equals(kernelMethod.getName())) {

               // well they have the same name ;) 
               isMapped = true;
            }
         }
      }
      return (isMapped);
   }

   public static boolean usesAtomic32(MethodReferenceEntry methodReferenceEntry) {
      for (final Method kernelMethod : Kernel.class.getDeclaredMethods()) {
         if (kernelMethod.isAnnotationPresent(OpenCLMapping.class)) {
            if (methodReferenceEntry.getNameAndTypeEntry().getNameUTF8Entry().getUTF8().equals(kernelMethod.getName())) {
               final OpenCLMapping annotation = kernelMethod.getAnnotation(OpenCLMapping.class);
               return annotation.atomic32();
            }
         }
      }
      return (false);
   }

   // For alpha release atomic64 is not supported
   public static boolean usesAtomic64(MethodReferenceEntry methodReferenceEntry) {
      //for (java.lang.reflect.Method kernelMethod : Kernel.class.getDeclaredMethods()) {
      //   if (kernelMethod.isAnnotationPresent(Kernel.OpenCLMapping.class)) {
      //      if (methodReferenceEntry.getNameAndTypeEntry().getNameUTF8Entry().getUTF8().equals(kernelMethod.getName())) {
      //         OpenCLMapping annotation = kernelMethod.getAnnotation(Kernel.OpenCLMapping.class);
      //           return annotation.atomic64();
      //      }
      //   }
      //}
      return (false);
   }

   // the flag useNullForLocalSize is useful for testing that what we compute for localSize is what OpenCL
   // would also compute if we passed in null.  In non-testing mode, we just call execute with the
   // same localSize that we computed in getLocalSizeJNI.  We don't want do publicize these of course.
   // GRF we can't access this from test classes without exposing in in javadoc so I left the flag but made the test/set of the flag reflectively
   boolean useNullForLocalSize = false;


}
