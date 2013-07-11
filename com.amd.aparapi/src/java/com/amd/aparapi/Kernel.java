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

import com.amd.aparapi.annotation.Experimental;
import com.amd.aparapi.annotation.OpenCLDelegate;
import com.amd.aparapi.annotation.OpenCLMapping;
import com.amd.aparapi.exception.DeprecatedException;
import com.amd.aparapi.internal.kernel.KernelRunner;
import com.amd.aparapi.internal.model.ClassModel.ConstantPool.MethodReferenceEntry;
import com.amd.aparapi.internal.opencl.OpenCLLoader;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Logger;

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

   private static Logger logger = Logger.getLogger(Config.getLoggerName());

    public abstract class Entry {
      public abstract void run();

      public Kernel execute(Range _range) {
         return (Kernel.this.execute("foo", _range, 1));
      }
   }

   /**
    * The <i>execution mode</i> ENUM enumerates the possible modes of executing a kernel. 
    * One can request a mode of execution using the values below, and query a kernel after it first executes to 
    * determine how it executed.  
    *    
    * <p>
    * Aparapi supports 4 execution modes. 
    * <ul>
    * <table>
    * <tr><th align="left">Enum value</th><th align="left">Execution</th></tr>
    * <tr><td><code><b>GPU</b></code></td><td>Execute using OpenCL on first available GPU device</td></tr>
    * <tr><td><code><b>CPU</b></code></td><td>Execute using OpenCL on first available CPU device</td></tr>
    * <tr><td><code><b>JTP</b></code></td><td>Execute using a Java Thread Pool (one thread spawned per available core)</td></tr>
    * <tr><td><code><b>SEQ</b></code></td><td>Execute using a single loop. This is useful for debugging but will be less 
    * performant than the other modes</td></tr>
    * </table>
    * </ul>
    * <p>
    * To request that a kernel is executed in a specific mode, call <code>Kernel.setExecutionMode(EXECUTION_MODE)</code> before the
    *  kernel first executes.
    * <p>
    * <blockquote><pre>
    *     int[] values = new int[1024];
    *     // fill values array
    *     SquareKernel kernel = new SquareKernel(values);
    *     kernel.setExecutionMode(Kernel.EXECUTION_MODE.JTP);
    *     kernel.execute(values.length);
    * </pre></blockquote>
    * <p>
    * Alternatively, the property <code>com.amd.aparapi.executionMode</code> can be set to one of <code>JTP,GPU,CPU,SEQ</code>
    * when an application is launched. 
    * <p><blockquote><pre>
    *    java -classpath ....;aparapi.jar -Dcom.amd.aparapi.executionMode=GPU MyApplication  
    * </pre></blockquote><p>
    * Generally setting the execution mode is not recommended (it is best to let Aparapi decide automatically) but the option
    * provides a way to compare a kernel's performance under multiple execution modes.
    * 
    * @author  gfrost AMD Javalabs
    * @version Alpha, 21/09/2010
    */

   public static enum EXECUTION_MODE {
      /**
       * A dummy value to indicate an unknown state.
       */
      NONE,
      /**
       * The value representing execution on a GPU device via OpenCL.
       */
      GPU,
      /**
       * The value representing execution on a CPU device via OpenCL.
       * <p>
       * <b>Note</b> not all OpenCL implementations support OpenCL compute on the CPU. 
       */
      CPU,
      /**
       * The value representing execution on a Java Thread Pool.
       * <p>
       * By default one Java thread is started for each available core and each core will execute <code>globalSize/cores</code> work items.
       * This creates a total of <code>globalSize%cores</code> threads to complete the work.  
       * Choose suitable values for <code>globalSize</code> to minimize the number of threads that are spawned. 
       */
      JTP,
      /**
       * The value representing execution sequentially in a single loop.
       * <p>
       * This is meant to be used for debugging a kernel.
       */
      SEQ;

      static EXECUTION_MODE getDefaultExecutionMode() {
         EXECUTION_MODE defaultExecutionMode = OpenCLLoader.isOpenCLAvailable() ? GPU : JTP;
         final String executionMode = Config.executionMode;
         if (executionMode != null) {
            try {
               EXECUTION_MODE requestedExecutionMode;
               requestedExecutionMode = getExecutionModeFromString(executionMode).iterator().next();
               logger.fine("requested execution mode =");
               if ((OpenCLLoader.isOpenCLAvailable() && requestedExecutionMode.isOpenCL()) || !requestedExecutionMode.isOpenCL()) {
                  defaultExecutionMode = requestedExecutionMode;
               }
            } catch (final Throwable t) {
               // we will take the default
            }
         }

         logger.fine("default execution modes = " + defaultExecutionMode);

         return (defaultExecutionMode);
      }

      static LinkedHashSet<EXECUTION_MODE> getDefaultExecutionModes() {
         LinkedHashSet<EXECUTION_MODE> defaultExecutionModes = new LinkedHashSet<EXECUTION_MODE>();

         if (OpenCLLoader.isOpenCLAvailable()) {
            defaultExecutionModes.add(GPU);
            defaultExecutionModes.add(JTP);
         } else {
            defaultExecutionModes.add(JTP);
         }

         final String executionMode = Config.executionMode;

         if (executionMode != null) {
            try {
               LinkedHashSet<EXECUTION_MODE> requestedExecutionModes;
               requestedExecutionModes = EXECUTION_MODE.getExecutionModeFromString(executionMode);
               logger.fine("requested execution mode =");
               for (final EXECUTION_MODE mode : requestedExecutionModes) {
                  logger.fine(" " + mode);
               }
               if ((OpenCLLoader.isOpenCLAvailable() && EXECUTION_MODE.anyOpenCL(requestedExecutionModes))
                     || !EXECUTION_MODE.anyOpenCL(requestedExecutionModes)) {
                  defaultExecutionModes = requestedExecutionModes;
               }
            } catch (final Throwable t) {
               // we will take the default
            }
         }

         logger.info("default execution modes = " + defaultExecutionModes);

         for (final EXECUTION_MODE e : defaultExecutionModes) {
            logger.info("SETTING DEFAULT MODE: " + e.toString());
         }

         return (defaultExecutionModes);
      }

      static LinkedHashSet<EXECUTION_MODE> getExecutionModeFromString(String executionMode) {
         final LinkedHashSet<EXECUTION_MODE> executionModes = new LinkedHashSet<EXECUTION_MODE>();
         for (final String mode : executionMode.split(",")) {
            executionModes.add(valueOf(mode.toUpperCase()));
         }
         return executionModes;
      }

      static EXECUTION_MODE getFallbackExecutionMode() {
         final EXECUTION_MODE defaultFallbackExecutionMode = JTP;
         logger.info("fallback execution mode = " + defaultFallbackExecutionMode);
         return (defaultFallbackExecutionMode);
      }

      static boolean anyOpenCL(LinkedHashSet<EXECUTION_MODE> _executionModes) {
         for (final EXECUTION_MODE mode : _executionModes) {
            if ((mode == GPU) || (mode == CPU)) {
               return true;
            }
         }
         return false;
      }

      public boolean isOpenCL() {
         return (this == GPU) || (this == CPU);
      }
   }

   private KernelRunner kernelRunner = null;

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

   /**
    * Determine the execution time of the previous Kernel.execute(range) call.
    * 
    * Note that for the first call this will include the conversion time. 
    * 
    * @return The time spent executing the kernel (ms) 
    * 
    * @see KernelRunner#getConversionTime();
    * @see KernelRunner#getAccumulatedExecutionTime() ();
    * 
    */
   public synchronized long getExecutionTime() {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      return (kernelRunner.getExecutionTime());
   }

   /**
    * Determine the total execution time of all previous Kernel.execute(range) calls.
    * 
    * Note that this will include the initial conversion time. 
    * 
    * @return The total time spent executing the kernel (ms) 
    * 
    * @see KernelRunner#getExecutionTime();
    * @see KernelRunner#getConversionTime();
    * 
    */
   public synchronized long getAccumulatedExecutionTime() {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      return (kernelRunner.getAccumulatedExecutionTime());
   }

   /**
    * Determine the time taken to convert bytecode to OpenCL for first Kernel.execute(range) call.
    * @return The time spent preparing the kernel for execution using GPU
    * 
    * @see KernelRunner#getExecutionTime();
    * @see #getAccumulatedExecutionTime;
    */
   public synchronized long getConversionTime() {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      return (kernelRunner.getConversionTime());
   }

   /**
    * Start execution of <code>_range</code> kernels.
    * <p>
    * When <code>kernel.execute(globalSize)</code> is invoked, Aparapi will schedule the execution of <code>globalSize</code> kernels. If the execution mode is GPU then 
    * the kernels will execute as OpenCL code on the GPU device. Otherwise, if the mode is JTP, the kernels will execute as a pool of Java threads on the CPU. 
    * <p>
    * @param _range The number of Kernels that we would like to initiate.
    * @returnThe Kernel instance (this) so we can chain calls to put(arr).execute(range).get(arr)
    * 
    */
   public synchronized Kernel execute(Range _range) {
      return (execute(_range, 1));
   }

   /**
    * Start execution of <code>_range</code> kernels.
    * <p>
    * When <code>kernel.execute(_range)</code> is invoked, Aparapi will schedule the execution of <code>_range</code> kernels. If the execution mode is GPU then 
    * the kernels will execute as OpenCL code on the GPU device. Otherwise, if the mode is JTP, the kernels will execute as a pool of Java threads on the CPU. 
    * <p>
    * Since adding the new <code>Range class</code> this method offers backward compatibility and merely defers to <code> return (execute(Range.create(_range), 1));</code>.
    * @param _range The number of Kernels that we would like to initiate.
    * @returnThe Kernel instance (this) so we can chain calls to put(arr).execute(range).get(arr)
    * 
    */
   public synchronized Kernel execute(int _range) {
      return (execute(Range.create(_range), 1));
   }

   /**
    * Start execution of <code>_passes</code> iterations of <code>_range</code> kernels.
    * <p>
    * When <code>kernel.execute(_range, _passes)</code> is invoked, Aparapi will schedule the execution of <code>_reange</code> kernels. If the execution mode is GPU then 
    * the kernels will execute as OpenCL code on the GPU device. Otherwise, if the mode is JTP, the kernels will execute as a pool of Java threads on the CPU. 
    * <p>
    * @param _range range specification to execute
    * @param _passes The number of passes to make
    * @return The Kernel instance (this) so we can chain calls to put(arr).execute(range).get(arr)
    * 
    */
   public synchronized Kernel execute(Range _range, int _passes) {
      return (execute("run", _range, _passes));
   }

   /**
    * Start execution of <code>_passes</code> iterations over the <code>_range</code> of kernels.
    * <p>
    * When <code>kernel.execute(_range)</code> is invoked, Aparapi will schedule the execution of <code>_range</code> kernels. If the execution mode is GPU then 
    * the kernels will execute as OpenCL code on the GPU device. Otherwise, if the mode is JTP, the kernels will execute as a pool of Java threads on the CPU. 
    * <p>
    * Since adding the new <code>Range class</code> this method offers backward compatibility and merely defers to <code> return (execute(Range.create(_range), 1));</code>.
    * @param _range The number of Kernels that we would like to initiate.
    * @returnThe Kernel instance (this) so we can chain calls to put(arr).execute(range).get(arr)
    * 
    */
   public synchronized Kernel execute(int _range, int _passes) {
      return (execute(Range.create(_range), _passes));
   }

   /**
    * Start execution of <code>globalSize</code> kernels for the given entrypoint.
    * <p>
    * When <code>kernel.execute("entrypoint", globalSize)</code> is invoked, Aparapi will schedule the execution of <code>globalSize</code> kernels. If the execution mode is GPU then 
    * the kernels will execute as OpenCL code on the GPU device. Otherwise, if the mode is JTP, the kernels will execute as a pool of Java threads on the CPU. 
    * <p>
    * @param _entry is the name of the method we wish to use as the entrypoint to the kernel
    * @param _range The range of Kernels that we would like to initiate.
    * @return The Kernel instance (this) so we can chain calls to put(arr).execute(range).get(arr)
    * 
    */
   public synchronized Kernel execute(Entry _entry, Range _range) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      return (kernelRunner.execute(_entry, _range, 1));
   }

   /**
    * Start execution of <code>globalSize</code> kernels for the given entrypoint.
    * <p>
    * When <code>kernel.execute("entrypoint", globalSize)</code> is invoked, Aparapi will schedule the execution of <code>globalSize</code> kernels. If the execution mode is GPU then 
    * the kernels will execute as OpenCL code on the GPU device. Otherwise, if the mode is JTP, the kernels will execute as a pool of Java threads on the CPU. 
    * <p>
    * @param _entrypoint is the name of the method we wish to use as the entrypoint to the kernel
    * @param _range The range of Kernels that we would like to initiate.
    * @return The Kernel instance (this) so we can chain calls to put(arr).execute(range).get(arr)
    * 
    */
   public synchronized Kernel execute(String _entrypoint, Range _range) {
      return (execute(_entrypoint, _range, 1));
   }

   /**
    * Start execution of <code>globalSize</code> kernels for the given entrypoint.
    * <p>
    * When <code>kernel.execute("entrypoint", globalSize)</code> is invoked, Aparapi will schedule the execution of <code>globalSize</code> kernels. If the execution mode is GPU then 
    * the kernels will execute as OpenCL code on the GPU device. Otherwise, if the mode is JTP, the kernels will execute as a pool of Java threads on the CPU. 
    * <p>
    * @param _entrypoint is the name of the method we wish to use as the entrypoint to the kernel
    * @param _range The number of Kernels that we would like to initiate.
    * @return The Kernel instance (this) so we can chain calls to put(arr).execute(range).get(arr)
    * 
    */
   public synchronized Kernel execute(String _entrypoint, Range _range, int _passes) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);

      }

      return (kernelRunner.execute(_entrypoint, _range, _passes));
   }

   /**
    * Release any resources associated with this Kernel.
    * <p>
    * When the execution mode is <code>CPU</code> or <code>GPU</code>, Aparapi stores some OpenCL resources in a data structure associated with the kernel instance.  The 
    * <code>dispose()</code> method must be called to release these resources. 
    * <p>
    * If <code>execute(int _globalSize)</code> is called after <code>dispose()</code> is called the results are undefined.  
    */
   public synchronized void dispose() {
      if (kernelRunner != null) {
         kernelRunner.dispose();
         kernelRunner = null;
      }
   }

   /**
    * Return the current execution mode.  
    * 
    * Before a Kernel executes, this return value will be the execution mode as determined by the setting of 
    * the EXECUTION_MODE enumeration. By default, this setting is either <b>GPU</b> 
    * if OpenCL is available on the target system, or <b>JTP</b> otherwise. This default setting can be
    * changed by calling setExecutionMode(). 
    * 
    * <p>
    * After a Kernel executes, the return value will be the mode in which the Kernel actually executed.
    * 
    * @return The current execution mode.
    * 
    * @see #setExecutionMode(EXECUTION_MODE)
    */
   public EXECUTION_MODE getExecutionMode() {
      return (executionMode);
   }

   /**
    * Set the execution mode. 
    * <p>
    * This should be regarded as a request. The real mode will be determined at runtime based on the availability of OpenCL and the characteristics of the workload.
    * 
    * @param _executionMode the requested execution mode.
    * 
    * @see #getExecutionMode()
    */
   public void setExecutionMode(EXECUTION_MODE _executionMode) {
      executionMode = _executionMode;
   }

   public void setFallbackExecutionMode() {
      executionMode = EXECUTION_MODE.getFallbackExecutionMode();
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

   // Explicit memory management API's follow

   /**
    * For dev purposes (we should remove this for production) allow us to define that this Kernel uses explicit memory management
    * @param _explicit (true if we want explicit memory management)
    */
   public void setExplicit(boolean _explicit) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.setExplicit(_explicit);
   }

   /**
    * For dev purposes (we should remove this for production) determine whether this Kernel uses explicit memory management
    * @return  (true if we kernel is using explicit memory management)
    */
   public boolean isExplicit() {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      return (kernelRunner.isExplicit());
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(long[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(long[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(long[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(double[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(double[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(double[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(float[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(float[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(float[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(int[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(int[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(int[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(byte[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(byte[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(byte[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
     * Tag this array so that it is explicitly enqueued before the kernel is executed
     * @param array
     * @return This kernel so that we can use the 'fluent' style API
     */
   public Kernel put(char[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(char[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(char[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(boolean[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(boolean[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel put(boolean[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.put(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(long[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(long[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(long[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(double[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(double[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(double[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(float[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(float[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(float[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(int[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(int[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(int[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(byte[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(byte[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(byte[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(char[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(char[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(char[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(boolean[] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(boolean[][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available. 
    * @param array
    * @return This kernel so that we can use the 'fluent' style API
    */
   public Kernel get(boolean[][][] array) {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      kernelRunner.get(array);
      return (this);
   }

   /**
    * Get the profiling information from the last successful call to Kernel.execute().
    * @return A list of ProfileInfo records
    */
   public List<ProfileInfo> getProfileInfo() {
      if (kernelRunner == null) {
         kernelRunner = new KernelRunner(this);
      }

      return (kernelRunner.getProfileInfo());
   }

   private final LinkedHashSet<EXECUTION_MODE> executionModes = EXECUTION_MODE.getDefaultExecutionModes();

   private Iterator<EXECUTION_MODE> currentMode = executionModes.iterator();

   private EXECUTION_MODE executionMode = currentMode.next();

   /**
    * set possible fallback path for execution modes.
    * for example setExecutionFallbackPath(GPU,CPU,JTP) will try to use the GPU
    * if it fails it will fall back to OpenCL CPU and finally it will try JTP.
    */
   public void addExecutionModes(EXECUTION_MODE... platforms) {
      executionModes.addAll(Arrays.asList(platforms));
      currentMode = executionModes.iterator();
      executionMode = currentMode.next();
   }

   /**
    * @return is there another execution path we can try
    */
   public boolean hasNextExecutionMode() {
      return currentMode.hasNext();
   }

   /**
    * try the next execution path in the list if there aren't any more than give up
    */
   public void tryNextExecutionMode() {
      if (currentMode.hasNext()) {
         executionMode = currentMode.next();
      }
   }
}
