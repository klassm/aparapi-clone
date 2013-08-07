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
package com.amd.aparapi.internal.kernel;

import com.amd.aparapi.*;
import com.amd.aparapi.Kernel.KernelState;
import com.amd.aparapi.annotation.Constant;
import com.amd.aparapi.annotation.Local;
import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.internal.exception.AparapiException;
import com.amd.aparapi.internal.exception.CodeGenException;
import com.amd.aparapi.internal.instruction.InstructionSet.TypeSpec;
import com.amd.aparapi.internal.jni.KernelRunnerJNI;
import com.amd.aparapi.internal.model.ClassModel;
import com.amd.aparapi.internal.model.Entrypoint;
import com.amd.aparapi.internal.util.UnsafeWrapper;
import com.amd.aparapi.internal.writer.KernelWriter;
import com.amd.aparapi.opencl.OpenCL;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class is responsible for executing <code>Kernel</code> implementations. <br/>
 *
 * The <code>KernelRunner</code> is the real workhorse for Aparapi.  Each <code>Kernel</code> instance creates a single
 * <code>KernelRunner</code> to encapsulate state and to help coordinate interactions between the <code>Kernel</code>
 * and it's execution logic.<br/>
 *
 * The <code>KernelRunner</code> is created <i>lazily</i> as a result of calling <code>Kernel.execute()</code>. A this
 * time the <code>ExecutionMode</code> is consulted to determine the default requested mode.  This will dictate how 
 * the <code>KernelRunner</code> will attempt to execute the <code>Kernel</code>
 *
 * @author gfrost
 *
 */
public class KernelRunner extends KernelRunnerJNI {

   private static Logger logger = Logger.getLogger(Config.getLoggerName());

   private OpenCLDevice lastGPUExecutionDevice = null;
   private Map<Class<? extends Kernel>, KernelMapping> kernelMappingMap = new HashMap<Class<? extends Kernel>, KernelMapping>();

   private final ExecutorService threadPool = Executors.newCachedThreadPool();

   private final LinkedHashSet<EXECUTION_MODE> executionModes = EXECUTION_MODE.getDefaultExecutionModes();
   private Iterator<EXECUTION_MODE> currentMode = executionModes.iterator();
   private EXECUTION_MODE executionMode = currentMode.next();

   private Set<String> capabilitiesSet;

   private long accumulatedExecutionTime = 0;
   private long conversionTime = 0;
   private long executionTime = 0;

   private final Set<Object> puts = new HashSet<Object>();

   private long kernelRunnerContextHandle = 0;


   /**
    * <code>Kernel.dispose()</code> delegates to <code>KernelRunner.dispose()</code> which delegates to
    * <code>disposeKernelRunnerJNI()</code> to actually close JNI data structures.<br/>
    *
    * @see #disposeKernelRunnerJNI
    */
   public void dispose() {
      if (getExecutionMode().isOpenCL()) {
         disposeKernelRunnerJNI(kernelRunnerContextHandle);
      }
      threadPool.shutdownNow();
   }

   boolean hasFP64Support() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return (capabilitiesSet.contains(OpenCL.CL_KHR_FP64));
   }

   boolean hasSelectFPRoundingModeSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_SELECT_FPROUNDING_MODE);
   }

   boolean hasGlobalInt32BaseAtomicsSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_GLOBAL_INT32_BASE_ATOMICS);
   }

   boolean hasGlobalInt32ExtendedAtomicsSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_GLOBAL_INT32_EXTENDED_ATOMICS);
   }

   boolean hasLocalInt32BaseAtomicsSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_LOCAL_INT32_BASE_ATOMICS);
   }

   boolean hasLocalInt32ExtendedAtomicsSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_LOCAL_INT32_EXTENDED_ATOMICS);
   }

   boolean hasInt64BaseAtomicsSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_INT64_BASE_ATOMICS);
   }

   boolean hasInt64ExtendedAtomicsSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_INT64_EXTENDED_ATOMICS);
   }

   boolean has3DImageWritesSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_3D_IMAGE_WRITES);
   }

   boolean hasByteAddressableStoreSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_BYTE_ADDRESSABLE_SUPPORT);
   }

   boolean hasFP16Support() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_FP16);
   }

   boolean hasGLSharingSupport() {
      if (capabilitiesSet == null) {
         throw new IllegalStateException("Capabilities queried before they were initialized");
      }
      return capabilitiesSet.contains(OpenCL.CL_KHR_GL_SHARING);
   }

   /**
    * Execute using a Java thread pool. Either because we were explicitly asked to do so, or because we 'fall back' after discovering an OpenCL issue.
    *
    * @param _range
    *          The globalSize requested by the user (via <code>Kernel.execute(globalSize)</code>)
    * @param _passes
    *          The # of passes requested by the user (via <code>Kernel.execute(globalSize, passes)</code>). Note this is usually defaulted to 1 via <code>Kernel.execute(globalSize)</code>.
    */
   private void executeJava(Kernel kernel, final Range _range, final int _passes) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("executeJava: range = " + _range);
      }

      if (getExecutionMode().equals(EXECUTION_MODE.SEQ)) {
         /**
          * SEQ mode is useful for testing trivial logic, but kernels which use SEQ mode cannot be used if the
          * product of localSize(0..3) is >1.  So we can use multi-dim ranges but only if the local size is 1 in all dimensions. 
          *
          * As a result of this barrier is only ever 1 work item wide and probably should be turned into a no-op. 
          *
          * So we need to check if the range is valid here. If not we have no choice but to punt.
          */
         if ((_range.getLocalSize(0) * _range.getLocalSize(1) * _range.getLocalSize(2)) > 1) {
            throw new IllegalStateException("Can't run range with group size >1 sequentially. Barriers would deadlock!");
         }

         final Kernel kernelClone = kernel.clone();
         final KernelState kernelState = kernelClone.getKernelState();

         kernelState.setRange(_range);
         kernelState.setGroupId(0, 0);
         kernelState.setGroupId(1, 0);
         kernelState.setGroupId(2, 0);
         kernelState.setLocalId(0, 0);
         kernelState.setLocalId(1, 0);
         kernelState.setLocalId(2, 0);
         kernelState.setLocalBarrier(new CyclicBarrier(1));

         for (int passId = 0; passId < _passes; passId++) {
            kernelState.setPassId(passId);

            if (_range.getDims() == 1) {
               for (int id = 0; id < _range.getGlobalSize(0); id++) {
                  kernelState.setGlobalId(0, id);
                  kernelClone.run();
               }
            } else if (_range.getDims() == 2) {
               for (int x = 0; x < _range.getGlobalSize(0); x++) {
                  kernelState.setGlobalId(0, x);

                  for (int y = 0; y < _range.getGlobalSize(1); y++) {
                     kernelState.setGlobalId(1, y);
                     kernelClone.run();
                  }
               }
            } else if (_range.getDims() == 3) {
               for (int x = 0; x < _range.getGlobalSize(0); x++) {
                  kernelState.setGlobalId(0, x);

                  for (int y = 0; y < _range.getGlobalSize(1); y++) {
                     kernelState.setGlobalId(1, y);

                     for (int z = 0; z < _range.getGlobalSize(2); z++) {
                        kernelState.setGlobalId(2, z);
                        kernelClone.run();
                     }

                     kernelClone.run();
                  }
               }
            }
         }
      } else {
         final int threads = _range.getLocalSize(0) * _range.getLocalSize(1) * _range.getLocalSize(2);
         final int globalGroups = _range.getNumGroups(0) * _range.getNumGroups(1) * _range.getNumGroups(2);
         /**
          * This joinBarrier is the barrier that we provide for the kernel threads to rendezvous with the current dispatch thread.
          * So this barrier is threadCount+1 wide (the +1 is for the dispatch thread)
          */
         final CyclicBarrier joinBarrier = new CyclicBarrier(threads + 1);

         /**
          * This localBarrier is only ever used by the kernels.  If the kernel does not use the barrier the threads 
          * can get out of sync, we promised nothing in JTP mode.
          *
          * As with OpenCL all threads within a group must wait at the barrier or none.  It is a user error (possible deadlock!)
          * if the barrier is in a conditional that is only executed by some of the threads within a group.
          *
          * Kernel developer must understand this.
          *
          * This barrier is threadCount wide.  We never hit the barrier from the dispatch thread.
          */
         final CyclicBarrier localBarrier = new CyclicBarrier(threads);

         for (int passId = 0; passId < _passes; passId++) {
            /**
              * Note that we emulate OpenCL by creating one thread per localId (across the group).
              *
              * So threadCount == range.getLocalSize(0)*range.getLocalSize(1)*range.getLocalSize(2);
              *
              * For a 1D range of 12 groups of 4 we create 4 threads. One per localId(0).
              *
              * We also clone the kernel 4 times. One per thread.
              *
              * We create local barrier which has a width of 4
              *
              *    Thread-0 handles localId(0) (global 0,4,8)
              *    Thread-1 handles localId(1) (global 1,5,7)
              *    Thread-2 handles localId(2) (global 2,6,10)
              *    Thread-3 handles localId(3) (global 3,7,11)
              *
              * This allows all threads to synchronize using the local barrier.
              *
              * Initially the use of local buffers seems broken as the buffers appears to be per Kernel.
              * Thankfully Kernel.clone() performs a shallow clone of all buffers (local and global)
              * So each of the cloned kernels actually still reference the same underlying local/global buffers.
              *
              * If the kernel uses local buffers but does not use barriers then it is possible for different groups
              * to see mutations from each other (unlike OpenCL), however if the kernel does not us barriers then it
              * cannot assume any coherence in OpenCL mode either (the failure mode will be different but still wrong)
              *
              * So even JTP mode use of local buffers will need to use barriers. Not for the same reason as OpenCL but to keep groups in lockstep.
              *
              **/
            for (int id = 0; id < threads; id++) {
               final int threadId = id;

               /**
                *  We clone one kernel for each thread.
                *
                *  They will all share references to the same range, localBarrier and global/local buffers because the clone is shallow.
                *  We need clones so that each thread can assign 'state' (localId/globalId/groupId) without worrying 
                *  about other threads.   
                */
               final Kernel kernelClone = kernel.clone();
               final KernelState kernelState = kernelClone.getKernelState();

               kernelState.setRange(_range);
               kernelState.setLocalBarrier(localBarrier);
               kernelState.setPassId(passId);

               threadPool.submit(new Runnable(){
                  @Override public void run() {
                     for (int globalGroupId = 0; globalGroupId < globalGroups; globalGroupId++) {

                        if (_range.getDims() == 1) {
                           kernelState.setLocalId(0, (threadId % _range.getLocalSize(0)));
                           kernelState.setGlobalId(0, (threadId + (globalGroupId * threads)));
                           kernelState.setGroupId(0, globalGroupId);
                        } else if (_range.getDims() == 2) {

                           /**
                            * Consider a 12x4 grid of 4*2 local groups
                            * <pre>
                            *                                             threads = 4*2 = 8
                            *                                             localWidth=4
                            *                                             localHeight=2
                            *                                             globalWidth=12
                            *                                             globalHeight=4
                            *
                            *    00 01 02 03 | 04 05 06 07 | 08 09 10 11  
                            *    12 13 14 15 | 16 17 18 19 | 20 21 22 23
                            *    ------------+-------------+------------
                            *    24 25 26 27 | 28 29 30 31 | 32 33 34 35
                            *    36 37 38 39 | 40 41 42 43 | 44 45 46 47  
                            *
                            *    00 01 02 03 | 00 01 02 03 | 00 01 02 03  threadIds : [0..7]*6
                            *    04 05 06 07 | 04 05 06 07 | 04 05 06 07
                            *    ------------+-------------+------------
                            *    00 01 02 03 | 00 01 02 03 | 00 01 02 03
                            *    04 05 06 07 | 04 05 06 07 | 04 05 06 07  
                            *
                            *    00 00 00 00 | 01 01 01 01 | 02 02 02 02  groupId[0] : 0..6 
                            *    00 00 00 00 | 01 01 01 01 | 02 02 02 02   
                            *    ------------+-------------+------------
                            *    00 00 00 00 | 01 01 01 01 | 02 02 02 02  
                            *    00 00 00 00 | 01 01 01 01 | 02 02 02 02
                            *
                            *    00 00 00 00 | 00 00 00 00 | 00 00 00 00  groupId[1] : 0..6 
                            *    00 00 00 00 | 00 00 00 00 | 00 00 00 00   
                            *    ------------+-------------+------------
                            *    01 01 01 01 | 01 01 01 01 | 01 01 01 01 
                            *    01 01 01 01 | 01 01 01 01 | 01 01 01 01
                            *
                            *    00 01 02 03 | 08 09 10 11 | 16 17 18 19  globalThreadIds == threadId + groupId * threads;
                            *    04 05 06 07 | 12 13 14 15 | 20 21 22 23
                            *    ------------+-------------+------------
                            *    24 25 26 27 | 32[33]34 35 | 40 41 42 43
                            *    28 29 30 31 | 36 37 38 39 | 44 45 46 47   
                            *
                            *    00 01 02 03 | 00 01 02 03 | 00 01 02 03  localX = threadId % localWidth; (for globalThreadId 33 = threadId = 01 : 01%4 =1)
                            *    00 01 02 03 | 00 01 02 03 | 00 01 02 03   
                            *    ------------+-------------+------------
                            *    00 01 02 03 | 00[01]02 03 | 00 01 02 03 
                            *    00 01 02 03 | 00 01 02 03 | 00 01 02 03
                            *
                            *    00 00 00 00 | 00 00 00 00 | 00 00 00 00  localY = threadId /localWidth  (for globalThreadId 33 = threadId = 01 : 01/4 =0)
                            *    01 01 01 01 | 01 01 01 01 | 01 01 01 01   
                            *    ------------+-------------+------------
                            *    00 00 00 00 | 00[00]00 00 | 00 00 00 00 
                            *    01 01 01 01 | 01 01 01 01 | 01 01 01 01
                            *
                            *    00 01 02 03 | 04 05 06 07 | 08 09 10 11  globalX=
                            *    00 01 02 03 | 04 05 06 07 | 08 09 10 11     groupsPerLineWidth=globalWidth/localWidth (=12/4 =3)
                            *    ------------+-------------+------------     groupInset =groupId%groupsPerLineWidth (=4%3 = 1)
                            *    00 01 02 03 | 04[05]06 07 | 08 09 10 11 
                            *    00 01 02 03 | 04 05 06 07 | 08 09 10 11     globalX = groupInset*localWidth+localX (= 1*4+1 = 5)
                            *
                            *    00 00 00 00 | 00 00 00 00 | 00 00 00 00  globalY
                            *    01 01 01 01 | 01 01 01 01 | 01 01 01 01      
                            *    ------------+-------------+------------
                            *    02 02 02 02 | 02[02]02 02 | 02 02 02 02 
                            *    03 03 03 03 | 03 03 03 03 | 03 03 03 03
                            *
                            * </pre>
                            * Assume we are trying to locate the id's for #33 
                            *
                            */

                           kernelState.setLocalId(0, (threadId % _range.getLocalSize(0))); // threadId % localWidth =  (for 33 = 1 % 4 = 1)
                           kernelState.setLocalId(1, (threadId / _range.getLocalSize(0))); // threadId / localWidth = (for 33 = 1 / 4 == 0)

                           final int groupInset = globalGroupId % _range.getNumGroups(0); // 4%3 = 1
                           kernelState.setGlobalId(0, ((groupInset * _range.getLocalSize(0)) + kernelState.getLocalIds()[0])); // 1*4+1=5

                           final int completeLines = (globalGroupId / _range.getNumGroups(0)) * _range.getLocalSize(1);// (4/3) * 2
                           kernelState.setGlobalId(1, (completeLines + kernelState.getLocalIds()[1])); // 2+0 = 2
                           kernelState.setGroupId(0, (globalGroupId % _range.getNumGroups(0)));
                           kernelState.setGroupId(1, (globalGroupId / _range.getNumGroups(0)));
                        } else if (_range.getDims() == 3) {

                           //Same as 2D actually turns out that localId[0] is identical for all three dims so could be hoisted out of conditional code

                           kernelState.setLocalId(0, (threadId % _range.getLocalSize(0)));

                           kernelState.setLocalId(1, ((threadId / _range.getLocalSize(0)) % _range.getLocalSize(1)));

                           // the thread id's span WxHxD so threadId/(WxH) should yield the local depth  
                           kernelState.setLocalId(2, (threadId / (_range.getLocalSize(0) * _range.getLocalSize(1))));

                           kernelState.setGlobalId(
                                 0,
                                 (((globalGroupId % _range.getNumGroups(0)) * _range.getLocalSize(0)) + kernelState.getLocalIds()[0]));

                           kernelState.setGlobalId(
                                 1,
                                 ((((globalGroupId / _range.getNumGroups(0)) * _range.getLocalSize(1)) % _range.getGlobalSize(1)) + kernelState
                                       .getLocalIds()[1]));

                           kernelState.setGlobalId(
                                 2,
                                 (((globalGroupId / (_range.getNumGroups(0) * _range.getNumGroups(1))) * _range.getLocalSize(2)) + kernelState
                                       .getLocalIds()[2]));

                           kernelState.setGroupId(0, (globalGroupId % _range.getNumGroups(0)));
                           kernelState.setGroupId(1, ((globalGroupId / _range.getNumGroups(0)) % _range.getNumGroups(1)));
                           kernelState.setGroupId(2, (globalGroupId / (_range.getNumGroups(0) * _range.getNumGroups(1))));
                        }

                        kernelClone.run();
                     }

                     await(joinBarrier); // This thread will rendezvous with dispatch thread here. This is effectively a join.                  
                  }
               });
            }

            await(joinBarrier); // This dispatch thread waits for all worker threads here. 
         }
      } // execution mode == JTP
   }

   private static void await(CyclicBarrier _barrier) {
      try {
         _barrier.await();
      } catch (final InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (final BrokenBarrierException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   private boolean usesOopConversion = false;

   /**
    *
    * @param entryPoint
    * @param kernel
    * @param arg
    * @return
    * @throws AparapiException
    */
   private boolean prepareOopConversionBuffer(Entrypoint entryPoint, Kernel kernel, KernelArg arg) throws AparapiException {
      usesOopConversion = true;
      final Class<?> arrayClass = arg.getField().getType();
      ClassModel c = null;
      boolean didReallocate = false;

      if (arg.getObjArrayElementModel() == null) {
         final String tmp = arrayClass.getName().substring(2).replace("/", ".");
         final String arrayClassInDotForm = tmp.substring(0, tmp.length() - 1);

         if (logger.isLoggable(Level.FINE)) {
            logger.fine("looking for type = " + arrayClassInDotForm);
         }

         // get ClassModel of obj array from entrypt.objectArrayFieldsClasses
         c = entryPoint.getObjectArrayFieldsClasses().get(arrayClassInDotForm);
         arg.setObjArrayElementModel(c);
      } else {
         c = arg.getObjArrayElementModel();
      }
      assert c != null : "should find class for elements " + arrayClass.getName();

      final int arrayBaseOffset = UnsafeWrapper.arrayBaseOffset(arrayClass);
      final int arrayScale = UnsafeWrapper.arrayIndexScale(arrayClass);

      if (logger.isLoggable(Level.FINEST)) {
         logger.finest("Syncing obj array type = " + arrayClass + " cvtd= " + c.getClassWeAreModelling().getName()
               + "arrayBaseOffset=" + arrayBaseOffset + " arrayScale=" + arrayScale);
      }

      int objArraySize = 0;
      Object newRef = null;
      try {
         newRef = arg.getField().get(kernel);
         objArraySize = Array.getLength(newRef);
      } catch (final IllegalAccessException e) {
         throw new AparapiException(e);
      }

      assert (newRef != null) && (objArraySize != 0) : "no data";

      final int totalStructSize = c.getTotalStructSize();
      final int totalBufferSize = objArraySize * totalStructSize;

      // allocate ByteBuffer if first time or array changed
      if ((arg.getObjArrayBuffer() == null) || (newRef != arg.getArray())) {
         final ByteBuffer structBuffer = ByteBuffer.allocate(totalBufferSize);
         arg.setObjArrayByteBuffer(structBuffer.order(ByteOrder.LITTLE_ENDIAN));
         arg.setObjArrayBuffer(arg.getObjArrayByteBuffer().array());
         didReallocate = true;
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest("objArraySize = " + objArraySize + " totalStructSize= " + totalStructSize + " totalBufferSize="
                  + totalBufferSize);
         }
      } else {
         arg.getObjArrayByteBuffer().clear();
      }

      // copy the fields that the JNI uses
      arg.setJavaArray(arg.getObjArrayBuffer());
      arg.setNumElements(objArraySize);
      arg.setSizeInBytes(totalBufferSize);

      for (int j = 0; j < objArraySize; j++) {
         int sizeWritten = 0;

         final Object object = UnsafeWrapper.getObject(newRef, arrayBaseOffset + (arrayScale * j));
         for (int i = 0; i < c.getStructMemberTypes().size(); i++) {
            final TypeSpec t = c.getStructMemberTypes().get(i);
            final long offset = c.getStructMemberOffsets().get(i);

            if (logger.isLoggable(Level.FINEST)) {
               logger.finest("name = " + c.getStructMembers().get(i).getNameAndTypeEntry().getNameUTF8Entry().getUTF8() + " t= "
                     + t);
            }

            switch (t) {
               case I: {
                  final int x = UnsafeWrapper.getInt(object, offset);
                  arg.getObjArrayByteBuffer().putInt(x);
                  sizeWritten += t.getSize();
                  break;
               }
               case F: {
                  final float x = UnsafeWrapper.getFloat(object, offset);
                  arg.getObjArrayByteBuffer().putFloat(x);
                  sizeWritten += t.getSize();
                  break;
               }
               case J: {
                  final long x = UnsafeWrapper.getLong(object, offset);
                  arg.getObjArrayByteBuffer().putLong(x);
                  sizeWritten += t.getSize();
                  break;
               }
               case Z: {
                  final boolean x = UnsafeWrapper.getBoolean(object, offset);
                  arg.getObjArrayByteBuffer().put(x == true ? (byte) 1 : (byte) 0);
                  // Booleans converted to 1 byte C chars for opencl
                  sizeWritten += TypeSpec.B.getSize();
                  break;
               }
               case B: {
                  final byte x = UnsafeWrapper.getByte(object, offset);
                  arg.getObjArrayByteBuffer().put(x);
                  sizeWritten += t.getSize();
                  break;
               }
               case D: {
                  throw new AparapiException("Double not implemented yet");
               }
               default:
                  assert true == false : "typespec did not match anything";
                  throw new AparapiException("Unhandled type in buffer conversion");
            }
         }

         // add padding here if needed
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest("sizeWritten = " + sizeWritten + " totalStructSize= " + totalStructSize);
         }

         assert sizeWritten <= totalStructSize : "wrote too much into buffer";

         while (sizeWritten < totalStructSize) {
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(arg.getName() + " struct pad byte = " + sizeWritten + " totalStructSize= " + totalStructSize);
            }
            arg.getObjArrayByteBuffer().put((byte) -1);
            sizeWritten++;
         }
      }

      assert arg.getObjArrayByteBuffer().arrayOffset() == 0 : "should be zero";

      return didReallocate;
   }

   private void extractOopConversionBuffer(Kernel kernel, KernelArg arg) throws AparapiException {
      final Class<?> arrayClass = arg.getField().getType();
      final ClassModel c = arg.getObjArrayElementModel();
      assert c != null : "should find class for elements: " + arrayClass.getName();
      assert arg.getArray() != null : "array is null";

      final int arrayBaseOffset = UnsafeWrapper.arrayBaseOffset(arrayClass);
      final int arrayScale = UnsafeWrapper.arrayIndexScale(arrayClass);
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest("Syncing field:" + arg.getName() + ", bb=" + arg.getObjArrayByteBuffer() + ", type = " + arrayClass);
      }

      int objArraySize = 0;
      try {
         objArraySize = Array.getLength(arg.getField().get(kernel));
      } catch (final IllegalAccessException e) {
         throw new AparapiException(e);
      }

      assert objArraySize > 0 : "should be > 0";

      final int totalStructSize = c.getTotalStructSize();
      // int totalBufferSize = objArraySize * totalStructSize;
      // assert arg.objArrayBuffer.length == totalBufferSize : "size should match";

      arg.getObjArrayByteBuffer().rewind();

      for (int j = 0; j < objArraySize; j++) {
         int sizeWritten = 0;
         final Object object = UnsafeWrapper.getObject(arg.getArray(), arrayBaseOffset + (arrayScale * j));
         for (int i = 0; i < c.getStructMemberTypes().size(); i++) {
            final TypeSpec t = c.getStructMemberTypes().get(i);
            final long offset = c.getStructMemberOffsets().get(i);
            switch (t) {
               case I: {
                  // read int value from buffer and store into obj in the array
                  final int x = arg.getObjArrayByteBuffer().getInt();
                  if (logger.isLoggable(Level.FINEST)) {
                     logger.finest("fType = " + t.getShortName() + " x= " + x);
                  }
                  UnsafeWrapper.putInt(object, offset, x);
                  sizeWritten += t.getSize();
                  break;
               }
               case F: {
                  final float x = arg.getObjArrayByteBuffer().getFloat();
                  if (logger.isLoggable(Level.FINEST)) {
                     logger.finest("fType = " + t.getShortName() + " x= " + x);
                  }
                  UnsafeWrapper.putFloat(object, offset, x);
                  sizeWritten += t.getSize();
                  break;
               }
               case J: {
                  final long x = arg.getObjArrayByteBuffer().getLong();
                  if (logger.isLoggable(Level.FINEST)) {
                     logger.finest("fType = " + t.getShortName() + " x= " + x);
                  }
                  UnsafeWrapper.putLong(object, offset, x);
                  sizeWritten += t.getSize();
                  break;
               }
               case Z: {
                  final byte x = arg.getObjArrayByteBuffer().get();
                  if (logger.isLoggable(Level.FINEST)) {
                     logger.finest("fType = " + t.getShortName() + " x= " + x);
                  }
                  UnsafeWrapper.putBoolean(object, offset, (x == 1 ? true : false));
                  // Booleans converted to 1 byte C chars for open cl
                  sizeWritten += TypeSpec.B.getSize();
                  break;
               }
               case B: {
                  final byte x = arg.getObjArrayByteBuffer().get();
                  if (logger.isLoggable(Level.FINEST)) {
                     logger.finest("fType = " + t.getShortName() + " x= " + x);
                  }
                  UnsafeWrapper.putByte(object, offset, x);
                  sizeWritten += t.getSize();
                  break;
               }
               case D: {
                  throw new AparapiException("Double not implemented yet");
               }
               default:
                  assert true == false : "typespec did not match anything";
                  throw new AparapiException("Unhandled type in buffer conversion");
            }
         }

         // add padding here if needed
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest("sizeWritten = " + sizeWritten + " totalStructSize= " + totalStructSize);
         }

         assert sizeWritten <= totalStructSize : "wrote too much into buffer";

         while (sizeWritten < totalStructSize) {
            // skip over pad bytes
            arg.getObjArrayByteBuffer().get();
            sizeWritten++;
         }
      }
   }

   private void restoreObjects(KernelMapping kernelMapping, Kernel kernel) throws AparapiException {
      for (KernelArg arg : kernelMapping.kernelArgs) {
         if ((arg.getType() & ARG_OBJ_ARRAY_STRUCT) != 0) {
            extractOopConversionBuffer(kernel, arg);
         }
      }
   }

   private boolean updateKernelArrayRefs(KernelMapping kernelMapping, Kernel kernel) throws AparapiException {
      boolean needsSync = false;

      for (KernelArg arg : kernelMapping.kernelArgs) {
         try {
            if ((arg.getType() & ARG_ARRAY) != 0) {
               Object newArrayRef;
               newArrayRef = arg.getField().get(kernel);

               if (newArrayRef == null) {
                  throw new IllegalStateException("Cannot send null refs to kernel, reverting to java");
               }

               if ((arg.getType() & ARG_OBJ_ARRAY_STRUCT) != 0) {
                  prepareOopConversionBuffer(kernelMapping.entryPoint, kernel, arg);
               } else {
                  // set up JNI fields for normal arrays
                  arg.setJavaArray(newArrayRef);
                  arg.setNumElements(Array.getLength(newArrayRef));
                  arg.setSizeInBytes(arg.getNumElements() * arg.getPrimitiveSize());

                  if (((arg.getType() & ARG_EXPLICIT) != 0) && puts.contains(newArrayRef)) {
                     arg.setType(arg.getType() | ARG_EXPLICIT_WRITE);
                     // System.out.println("detected an explicit write " + args[i].name);
                     puts.remove(newArrayRef);
                  }
               }

               if (newArrayRef != arg.getArray()) {
                  needsSync = true;

                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("saw newArrayRef for " + arg.getName() + " = " + newArrayRef + ", newArrayLen = "
                           + Array.getLength(newArrayRef));
                  }
               }

               arg.setArray(newArrayRef);
               assert arg.getArray() != null : "null array ref";
            }
         } catch (final IllegalArgumentException e) {
            logger.log(Level.SEVERE, "IllegalArgumentException during update kernel refs", e);
         } catch (final IllegalAccessException e) {
            logger.log(Level.SEVERE, "IllegalAccessException during update kernel refs", e);
         }
      }
      return needsSync;
   }

   private KernelRunner executeOpenCL(final Kernel kernel, KernelMapping kernelMapping,
                                      final Range _range, final int _passes) throws AparapiException {
      /*
      if (_range.getDims() > getMaxWorkItemDimensionsJNI(kernelContextHandle)) {
         throw new RangeException("Range dim size " + _range.getDims() + " > device "
               + getMaxWorkItemDimensionsJNI(kernelContextHandle));
      }
      if (_range.getWorkGroupSize() > getMaxWorkGroupSizeJNI(kernelContextHandle)) {
         throw new RangeException("Range workgroup size " + _range.getWorkGroupSize() + " > device "
               + getMaxWorkGroupSizeJNI(kernelContextHandle));
      }
      
            if (_range.getGlobalSize(0) > getMaxWorkItemSizeJNI(kernelContextHandle, 0)) {
               throw new RangeException("Range globalsize 0 " + _range.getGlobalSize(0) + " > device "
                     + getMaxWorkItemSizeJNI(kernelContextHandle, 0));
            }
            if (_range.getDims() > 1) {
               if (_range.getGlobalSize(1) > getMaxWorkItemSizeJNI(kernelContextHandle, 1)) {
                  throw new RangeException("Range globalsize 1 " + _range.getGlobalSize(1) + " > device "
                        + getMaxWorkItemSizeJNI(kernelContextHandle, 1));
               }
               if (_range.getDims() > 2) {
                  if (_range.getGlobalSize(2) > getMaxWorkItemSizeJNI(kernelContextHandle, 2)) {
                     throw new RangeException("Range globalsize 2 " + _range.getGlobalSize(2) + " > device "
                           + getMaxWorkItemSizeJNI(kernelContextHandle, 2));
                  }
               }
            }
      

      if (logger.isLoggable(Level.FINE)) {
         logger.fine("maxComputeUnits=" + this.getMaxComputeUnitsJNI(kernelContextHandle));
         logger.fine("maxWorkGroupSize=" + this.getMaxWorkGroupSizeJNI(kernelContextHandle));
         logger.fine("maxWorkItemDimensions=" + this.getMaxWorkItemDimensionsJNI(kernelContextHandle));
         logger.fine("maxWorkItemSize(0)=" + getMaxWorkItemSizeJNI(kernelContextHandle, 0));
         if (_range.getDims() > 1) {
            logger.fine("maxWorkItemSize(1)=" + getMaxWorkItemSizeJNI(kernelContextHandle, 1));
            if (_range.getDims() > 2) {
               logger.fine("maxWorkItemSize(2)=" + getMaxWorkItemSizeJNI(kernelContextHandle, 2));
            }
         }
      }
      */

      // explicit reference check, no equals!
      if (kernel != kernelMapping.getLastKernel()) {
         updateKernelJNI(kernelMapping.kernelContextHandle, kernel);
      }

      // Read the array refs after kernel may have changed them
      // We need to do this as input to computing the localSize
      final boolean needSync = updateKernelArrayRefs(kernelMapping, kernel);
      if (needSync && logger.isLoggable(Level.FINE)) {
         logger.fine("Need to resync arrays on " + kernel.getClass().getName());
      }

      // native side will reallocate array buffers if necessary
      if (runKernelJNI(kernelRunnerContextHandle, kernelMapping.kernelContextHandle, _range, needSync, _passes) != 0) {
         logger.warning("### CL exec seems to have failed. Trying to revert to Java ###");
         setFallbackExecutionMode();
         return execute(kernel, _range, _passes);
      }

      if (usesOopConversion) {
         restoreObjects(kernelMapping, kernel);
      }

      if (logger.isLoggable(Level.FINE)) {
         logger.fine("executeOpenCL completed. " + _range);
      }

      return this;
   }

   synchronized private KernelRunner fallBackAndExecute(Kernel kernel, final Range _range, final int _passes) {
      if (hasNextExecutionMode()) {
         tryNextExecutionMode();
      } else {
         setFallbackExecutionMode();
      }

      return execute(kernel, _range, _passes);
   }

   synchronized private KernelRunner warnFallBackAndExecute(Kernel kernel, final Range _range, final int _passes,
         Exception _exception) {
      if (logger.isLoggable(Level.WARNING)) {
         logger.warning("Reverting to Java Thread Pool (JTP) for " + kernel.getClass() + ": " + _exception.getMessage());
         _exception.printStackTrace();
      }
      return fallBackAndExecute(kernel, _range, _passes);
   }

   synchronized private KernelRunner warnFallBackAndExecute(Kernel kernel, final Range _range, final int _passes, String _excuse) {
      logger.warning("Reverting to Java Thread Pool (JTP) for " + kernel.getClass() + ": " + _excuse);
      return fallBackAndExecute(kernel, _range, _passes);
   }

   public synchronized KernelRunner execute(Kernel kernel, int globalSize) {
      return execute(kernel, globalSize, 1);
   }

   public synchronized KernelRunner execute(Kernel kernel, Range range) {
      return execute(kernel, range, 1);
   }

   public synchronized KernelRunner execute(Kernel kernel, int globalSize, int passes) {
      return execute(kernel, Range.create(Device.best(), globalSize), passes);
   }

   private void initKernelRunnerContextHandle(OpenCLDevice device, int flags) {
      if (kernelRunnerContextHandle != 0) return;
      kernelRunnerContextHandle = initKernelRunnerJNI(device, flags);
   }

   public synchronized KernelRunner execute(Kernel kernel, final Range _range, final int _passes) {


      long executeStartTime = System.currentTimeMillis();

      if (_range == null) {
         throw new IllegalStateException("range can't be null");
      }

      /* for backward compatibility reasons we still honor execution mode */
      if (getExecutionMode().isOpenCL()) {

         KernelMapping currentKernelMapping = kernelMappingMap.get(kernel.getClass());

         // See if user supplied a Device
         Device device = _range.getDevice();

         if ((device == null) || (device instanceof OpenCLDevice)) {
            if (currentKernelMapping == null) {
               try {
                  final ClassModel classModel = new ClassModel(kernel.getClass());
                  Entrypoint entryPoint = classModel.getEntrypoint(kernel);
                  currentKernelMapping = new KernelMapping(kernel.getClass(), entryPoint, kernel);
                  kernelMappingMap.put(kernel.getClass(), currentKernelMapping);
               } catch (final Exception exception) {
                  return warnFallBackAndExecute(kernel, _range, _passes, exception);
               }

               Entrypoint entryPoint = currentKernelMapping.entryPoint;
               if ((entryPoint != null) && !entryPoint.shouldFallback()) {
                  synchronized (Kernel.class) { // This seems to be needed because of a race condition uncovered with issue #68 http://code.google.com/p/aparapi/issues/detail?id=68

                     OpenCLDevice openCLDevice = (OpenCLDevice) device; // still might be null! 
                     if (openCLDevice == null && lastGPUExecutionDevice != null) {
                        openCLDevice = lastGPUExecutionDevice;
                     } else if (openCLDevice != null && lastGPUExecutionDevice != null && ! openCLDevice.equals(lastGPUExecutionDevice)) {
                        logger.severe("expected execution device: " + lastGPUExecutionDevice.toString());
                        logger.severe("current execution device: " + openCLDevice.toString());
                        throw new IllegalArgumentException("GPU device can only be set once! Please always " +
                            "use the same device!");
                     }

                     int jniFlags = 0;
                     if (openCLDevice == null) {
                        if (getExecutionMode().equals(EXECUTION_MODE.GPU)) {
                           // We used to treat as before by getting first GPU device
                           // now we get the best GPU
                           openCLDevice = (OpenCLDevice) OpenCLDevice.best();
                           jniFlags |= JNI_FLAG_USE_GPU; // this flag might be redundant now. 
                        } else {
                           // We fetch the first CPU device 
                           openCLDevice = (OpenCLDevice) OpenCLDevice.firstCPU();
                           if (openCLDevice == null) {
                              return warnFallBackAndExecute(kernel, _range, _passes,
                                    "CPU request can't be honored not CPU device");
                           }
                        }
                     } else {
                        if (openCLDevice.getType() == Device.TYPE.GPU) {
                           jniFlags |= JNI_FLAG_USE_GPU; // this flag might be redundant now. 
                        }
                     }

                     initKernelRunnerContextHandle(openCLDevice, jniFlags);
                     lastGPUExecutionDevice = openCLDevice;

                     //  jniFlags |= (Config.enableProfiling ? JNI_FLAG_ENABLE_PROFILING : 0);
                     //  jniFlags |= (Config.enableProfilingCSV ? JNI_FLAG_ENABLE_PROFILING_CSV | JNI_FLAG_ENABLE_PROFILING : 0);
                     //  jniFlags |= (Config.enableVerboseJNI ? JNI_FLAG_ENABLE_VERBOSE_JNI : 0);
                     // jniFlags |= (Config.enableVerboseJNIOpenCLResourceTracking ? JNI_FLAG_ENABLE_VERBOSE_JNI_OPENCL_RESOURCE_TRACKING :0);
                     // jniFlags |= (kernel.getExecutionMode().equals(EXECUTION_MODE.GPU) ? JNI_FLAG_USE_GPU : 0);
                     // Init the device to check capabilities before emitting the
                     // code that requires the capabilities.

                     // synchronized(Kernel.class){
                     currentKernelMapping.kernelContextHandle = initKernelJNI(kernelRunnerContextHandle, kernel);
                  } // end of synchronized! issue 68

                  if (currentKernelMapping.kernelContextHandle == 0) {
                     return warnFallBackAndExecute(kernel, _range, _passes, "initJNI failed to return a valid handle");
                  }

                  final String extensions = getExtensionsJNI(kernelRunnerContextHandle);
                  capabilitiesSet = new HashSet<String>();

                  final StringTokenizer strTok = new StringTokenizer(extensions);
                  while (strTok.hasMoreTokens()) {
                     capabilitiesSet.add(strTok.nextToken());
                  }

                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("Capabilities initialized to :" + capabilitiesSet.toString());
                  }

                  if (entryPoint.requiresDoublePragma() && !hasFP64Support()) {
                     return warnFallBackAndExecute(kernel, _range, _passes, "FP64 required but not supported");
                  }

                  if (entryPoint.requiresByteAddressableStorePragma() && !hasByteAddressableStoreSupport()) {
                     return warnFallBackAndExecute(kernel, _range, _passes,
                           "Byte addressable stores required but not supported");
                  }

                  final boolean all32AtomicsAvailable = hasGlobalInt32BaseAtomicsSupport()
                        && hasGlobalInt32ExtendedAtomicsSupport() && hasLocalInt32BaseAtomicsSupport()
                        && hasLocalInt32ExtendedAtomicsSupport();

                  if (entryPoint.requiresAtomic32Pragma() && !all32AtomicsAvailable) {

                     return warnFallBackAndExecute(kernel, _range, _passes, "32 bit Atomics required but not supported");
                  }

                  String openCL;
                  try {
                     openCL = KernelWriter.writeToString(entryPoint);
                  } catch (final CodeGenException codeGenException) {
                     return warnFallBackAndExecute(kernel, _range, _passes, codeGenException);
                  }

                  if (Config.enableShowGeneratedOpenCL) {
                     System.out.println(openCL);
                  }

                  if (logger.isLoggable(Level.INFO)) {
                     logger.info(openCL);
                  }

                  // Send the string to OpenCL to compile it
                  if (buildProgramJNI(kernelRunnerContextHandle, currentKernelMapping.kernelContextHandle, openCL) == 0) {
                     return warnFallBackAndExecute(kernel, _range, _passes, "OpenCL compile failed");
                  }

                  List<KernelArg> kernelArgs = findOutKernelArgsIn(entryPoint, kernel);
                  if (kernelArgs == null) {
                     return fallBackAndExecute(kernel, _range, _passes);
                  }

                  currentKernelMapping.kernelArgs.addAll(kernelArgs);

                  // at this point, i = the actual used number of arguments
                  // (private buffers do not get treated as arguments)

                  KernelArg[] kernelArgsArray = currentKernelMapping.kernelArgsAsArray();
                  setArgsJNI(kernelRunnerContextHandle, currentKernelMapping.kernelContextHandle,
                        kernelArgsArray, kernelArgsArray.length);

                  conversionTime = System.currentTimeMillis() - executeStartTime;

                  try {
                     executeOpenCL(kernel, currentKernelMapping, _range, _passes);
                  } catch (final AparapiException e) {
                     warnFallBackAndExecute(kernel, _range, _passes, e);
                  }
               } else {
                  warnFallBackAndExecute(kernel, _range, _passes, "failed to locate entrypoint");
               }
            } else {
               try {
                  executeOpenCL(kernel, currentKernelMapping, _range, _passes);
               } catch (final AparapiException e) {
                  warnFallBackAndExecute(kernel, _range, _passes, e);
               }
            }
         } else {
            warnFallBackAndExecute(kernel, _range, _passes,
                  "OpenCL was requested but Device supplied was not an OpenCLDevice");
         }
      } else {
         executeJava(kernel, _range, _passes);
      }

      if (Config.enableExecutionModeReporting) {
         System.out.println(kernel.getClass().getCanonicalName() + ":" + getExecutionMode());
      }

      executionTime = System.currentTimeMillis() - executeStartTime;
      accumulatedExecutionTime += executionTime;

      return this;
   }

   /**
    * Turns all referenced fields in a given entryPoint and kernel into a list of {@link KernelArg}s.
    * @param entryPoint entryPoint
    * @param kernel kernel
    * @return list of referenced fields in the form of a list of {@link KernelArg}
    */
   private List<KernelArg> findOutKernelArgsIn(Entrypoint entryPoint, Kernel kernel) {
      List<KernelArg> resultArgs = new ArrayList<KernelArg>();

      for (final Field field : entryPoint.getReferencedFields()) {
         try {
            field.setAccessible(true);
            KernelArg currentArgument = fieldToKernelArg(entryPoint, kernel, field);
            if (currentArgument == null) return null;

            resultArgs.add(currentArgument);
         } catch (final IllegalArgumentException e) {
            logger.log(Level.SEVERE, "IllegalArgumentException encountered during handling of field " + field.toString(), e);
         }
      }

      return resultArgs;
   }

   /**
    * Fills a new {@link KernelArg} object with data from a given {@link Entrypoint}, {@link Kernel} and {@link Field}.
    * @param entryPoint entryPoint
    * @param kernel kernel
    * @param field field
    * @return filled {@link KernelArg}
    */
   private KernelArg fieldToKernelArg(Entrypoint entryPoint, Kernel kernel, Field field) {
      KernelArg currentArgument = new KernelArg();
      currentArgument.setName(field.getName());
      currentArgument.setField(field);
      if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
         currentArgument.setType(currentArgument.getType() | ARG_STATIC);
      }

      final Class<?> type = field.getType();
      if (type.isArray()) {

         if (! handleArrayTypeKernelArg(entryPoint, kernel, field, currentArgument, type)) return null;
      } else if (type.isAssignableFrom(float.class)) {
         currentArgument.setType(currentArgument.getType() | ARG_PRIMITIVE);
         currentArgument.setType(currentArgument.getType() | ARG_FLOAT);
      } else if (type.isAssignableFrom(int.class)) {
         currentArgument.setType(currentArgument.getType() | ARG_PRIMITIVE);
         currentArgument.setType(currentArgument.getType() | ARG_INT);
      } else if (type.isAssignableFrom(double.class)) {
         currentArgument.setType(currentArgument.getType() | ARG_PRIMITIVE);
         currentArgument.setType(currentArgument.getType() | ARG_DOUBLE);
      } else if (type.isAssignableFrom(long.class)) {
         currentArgument.setType(currentArgument.getType() | ARG_PRIMITIVE);
         currentArgument.setType(currentArgument.getType() | ARG_LONG);
      } else if (type.isAssignableFrom(boolean.class)) {
         currentArgument.setType(currentArgument.getType() | ARG_PRIMITIVE);
         currentArgument.setType(currentArgument.getType() | ARG_BOOLEAN);
      } else if (type.isAssignableFrom(byte.class)) {
         currentArgument.setType(currentArgument.getType() | ARG_PRIMITIVE);
         currentArgument.setType(currentArgument.getType() | ARG_BYTE);
      } else if (type.isAssignableFrom(char.class)) {
         currentArgument.setType(currentArgument.getType() | ARG_PRIMITIVE);
         currentArgument.setType(currentArgument.getType() | ARG_CHAR);
      } else if (type.isAssignableFrom(short.class)) {
         currentArgument.setType(currentArgument.getType() | ARG_PRIMITIVE);
         currentArgument.setType(currentArgument.getType() | ARG_SHORT);
      }

      currentArgument.setPrimitiveSize(getPrimitiveSize(currentArgument.getType()));

      if (logger.isLoggable(Level.FINE)) {
         logger.fine("arg " + currentArgument.getName() + ", type=" + Integer.toHexString(currentArgument.getType())
             + ", primitiveSize=" + currentArgument.getPrimitiveSize());
      }
      return currentArgument;
   }

   /**
    * Fill the given {@link KernelArg} with array date in the given field.
    * @param entryPoint  entryPoint currently handled
    * @param kernel current kernel
    * @param field current kernel field
    * @param kernelArg current kernel arg (result type)
    * @param type field type
    * @return true if the array was successfully handled, false if an error has been encountered
    */
   private boolean handleArrayTypeKernelArg(Entrypoint entryPoint, Kernel kernel, Field field, KernelArg kernelArg, Class<?> type) {
      if (field.getAnnotation(Local.class) != null || kernelArg.getName().endsWith(Local.LOCAL_SUFFIX)) {
         kernelArg.setType(kernelArg.getType() | ARG_LOCAL);
      } else if ((field.getAnnotation(Constant.class) != null)
          || kernelArg.getName().endsWith(Constant.CONSTANT_SUFFIX)) {
         kernelArg.setType(kernelArg.getType() | ARG_CONSTANT);
      } else {
         kernelArg.setType(kernelArg.getType() | ARG_GLOBAL);
      }
      if (isExplicit()) {
         kernelArg.setType(kernelArg.getType() | ARG_EXPLICIT);
      }
      // for now, treat all write arrays as read-write, see bugzilla issue 4859
      // we might come up with a better solution later
      kernelArg.setType(kernelArg.getType()
          | (entryPoint.getArrayFieldAssignments().contains(field.getName()) ? (ARG_WRITE | ARG_READ) : 0));
      kernelArg.setType(kernelArg.getType()
          | (entryPoint.getArrayFieldAccesses().contains(field.getName()) ? ARG_READ : 0));
      // args[i].type |= ARG_GLOBAL;


      if (type.getName().startsWith("[L")) {
         kernelArg.setType(kernelArg.getType()
             | (ARG_OBJ_ARRAY_STRUCT |
             ARG_WRITE |
             ARG_READ |
             ARG_APARAPI_BUFFER));

         if (logger.isLoggable(Level.FINE)) {
            logger.fine("tagging " + kernelArg.getName() + " as (ARG_OBJ_ARRAY_STRUCT | ARG_WRITE | ARG_READ)");
         }
      } else if (type.getName().startsWith("[[")) {

         try {
            setMultiArrayType(kernel, kernelArg, type);
         } catch(AparapiException e) {
            logger.severe("failed to set kernel arguement " + kernelArg.getName() + ".  Aparapi only supports 2D and 3D arrays.");
            return false;
         }
      } else {

         kernelArg.setArray(null); // will get updated in updateKernelArrayRefs
         kernelArg.setType(kernelArg.getType() | ARG_ARRAY);

         kernelArg.setType(kernelArg.getType() | (type.isAssignableFrom(float[].class) ? ARG_FLOAT : 0));
         kernelArg.setType(kernelArg.getType() | (type.isAssignableFrom(int[].class) ? ARG_INT : 0));
         kernelArg.setType(kernelArg.getType() | (type.isAssignableFrom(boolean[].class) ? ARG_BOOLEAN : 0));
         kernelArg.setType(kernelArg.getType() | (type.isAssignableFrom(byte[].class) ? ARG_BYTE : 0));
         kernelArg.setType(kernelArg.getType() | (type.isAssignableFrom(char[].class) ? ARG_CHAR : 0));
         kernelArg.setType(kernelArg.getType() | (type.isAssignableFrom(double[].class) ? ARG_DOUBLE : 0));
         kernelArg.setType(kernelArg.getType() | (type.isAssignableFrom(long[].class) ? ARG_LONG : 0));
         kernelArg.setType(kernelArg.getType() | (type.isAssignableFrom(short[].class) ? ARG_SHORT : 0));

         // arrays whose length is used will have an int arg holding
         // the length as a kernel param
         if (entryPoint.getArrayFieldArrayLengthUsed().contains(kernelArg.getName())) {
            kernelArg.setType(kernelArg.getType() | ARG_ARRAYLENGTH);
         }

         if (type.getName().startsWith("[L")) {
            kernelArg.setType(kernelArg.getType() | (ARG_OBJ_ARRAY_STRUCT | ARG_WRITE | ARG_READ));
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("tagging " + kernelArg.getName() + " as (ARG_OBJ_ARRAY_STRUCT | ARG_WRITE | ARG_READ)");
            }
         }
      }
      return true;
   }


   private int getPrimitiveSize(int type) {
      if ((type & ARG_FLOAT) != 0) {
         return 4;
      } else if ((type & ARG_INT) != 0) {
         return 4;
      } else if ((type & ARG_BYTE) != 0) {
         return 1;
      } else if ((type & ARG_CHAR) != 0) {
         return 2;
      } else if ((type & ARG_BOOLEAN) != 0) {
         return 1;
      } else if ((type & ARG_SHORT) != 0) {
         return 2;
      } else if ((type & ARG_LONG) != 0) {
         return 8;
      } else if ((type & ARG_DOUBLE) != 0) {
         return 8;
      }
      return 0;
   }

   private void setMultiArrayType(Kernel kernel, KernelArg arg, Class<?> type) throws AparapiException {
      arg.setType(arg.getType() | (ARG_WRITE | ARG_READ | ARG_APARAPI_BUFFER));
      int numDims = 0;
      if(type.getName().startsWith("[[[[")) {
         throw new AparapiException("Aparapi only supports 2D and 3D arrays.");
      }
      arg.setType(arg.getType() | ARG_ARRAYLENGTH);
      while(type.getName().charAt(numDims) == '[') {
         numDims++;
      }
      Object buffer = new Object();
      try {
         buffer = arg.getField().get(kernel);
      } catch(IllegalAccessException e) {
         e.printStackTrace();
      }
      arg.setJavaBuffer(buffer);
      arg.setNumDims(numDims);
      Object subBuffer = buffer;
      int[] dims = new int[numDims];
      for(int i = 0; i < numDims-1; i++) {
         dims[i] = Array.getLength(subBuffer);
         subBuffer = Array.get(subBuffer, 0);
      }
      dims[numDims-1] = Array.getLength(subBuffer);
      arg.setDims(dims);

      if (subBuffer.getClass().isAssignableFrom(float[].class)) {
         arg.setType(arg.getType() | ARG_FLOAT);
      }
      if (subBuffer.getClass().isAssignableFrom(int[].class)) {
         arg.setType(arg.getType() | ARG_INT);
      }
      if (subBuffer.getClass().isAssignableFrom(boolean[].class)) {
         arg.setType(arg.getType() | ARG_BOOLEAN);
      }
      if (subBuffer.getClass().isAssignableFrom(byte[].class)) {
         arg.setType(arg.getType() | ARG_BYTE);
      }
      if (subBuffer.getClass().isAssignableFrom(char[].class)) {
         arg.setType(arg.getType() | ARG_CHAR);
      }
      if (subBuffer.getClass().isAssignableFrom(double[].class)) {
         arg.setType(arg.getType() | ARG_DOUBLE);
      }
      if (subBuffer.getClass().isAssignableFrom(long[].class)) {
         arg.setType(arg.getType() | ARG_LONG);
      }
      if (subBuffer.getClass().isAssignableFrom(short[].class)) {
         arg.setType(arg.getType() | ARG_SHORT);
      }
      int primitiveSize = getPrimitiveSize(arg.getType());
      int totalElements = 1;
      for(int i = 0; i < numDims; i++) {
         totalElements *= dims[i];
      }
      arg.setSizeInBytes(totalElements * primitiveSize);
   }

   public List<ProfileInfo> getProfileInfo(Kernel kernel) {
      return getProfileInfo(kernel.getClass());
   }

   public boolean hasProfileInfo(Class<? extends Kernel> kernelClass) {
      return kernelMappingMap.containsKey(kernelClass);
   }

   public List<ProfileInfo> getProfileInfo(Class<? extends Kernel> kernelClass) {
      KernelMapping kernelMapping = kernelMappingMap.get(kernelClass);
      if (kernelMapping == null) {
         throw new IllegalArgumentException("cannot find kernel for " + kernelClass.getName());
      }

      if (((getExecutionMode() == EXECUTION_MODE.GPU) || (getExecutionMode() == EXECUTION_MODE.CPU))) {
         // Only makes sense when we are using OpenCL
         return (getProfileInfoJNI(kernelMapping.kernelContextHandle));
      } else {
         return (null);
      }
   }

   private boolean explicit = false;

   public void setExplicit(boolean _explicit) {
      explicit = _explicit;
   }

   public boolean isExplicit() {
      return (explicit);
   }

   /**
    * Determine the time taken to convert bytecode to OpenCL for first Kernel.execute(range) call.
    *
    * @return The time spent preparing the kernel for execution using GPU
    *
    */
   public long getConversionTime() {
      return conversionTime;
   }

   /**
    * Determine the execution time of the previous Kernel.execute(range) call.
    *
    * @return The time spent executing the kernel (ms)
    *
    */
   public long getExecutionTime() {
      return executionTime;
   }

   /**
    * Determine the accumulated execution time of all previous Kernel.execute(range) calls.
    *
    * @return The accumulated time spent executing this kernel (ms)
    *
    */
   public long getAccumulatedExecutionTime() {
      return accumulatedExecutionTime;
   }

   public EXECUTION_MODE getExecutionMode() {
      return executionMode;
   }

   public void setExecutionMode(EXECUTION_MODE executionMode) {
      this.executionMode = executionMode;
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(long[] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(long[][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(long[][][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(double[] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(double[][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(double[][][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(float[] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(float[][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(float[][][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(int[] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(int[][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(int[][][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(byte[] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(byte[][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(byte[][][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(char[] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(char[][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(char[][][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(boolean[] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(boolean[][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed
    * @param array array to put
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner put(boolean[][][] array) {
      putRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(long[] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(long[][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(long[][][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array  array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(double[] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(double[][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(double[][][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(float[] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(float[][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(float[][][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(int[] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(int[][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(int[][][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(byte[] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(byte[][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(byte[][][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(char[] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(char[][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(char[][][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(boolean[] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(boolean[][] array) {
      getRaw(array);
      return (this);
   }

   /**
    * Enqueue a request to return this buffer from the GPU. This method blocks until the array is available.
    * @param array  array to get
    * @return This kernel so that we can use the 'fluent' style API
    */
   public KernelRunner get(boolean[][][] array) {
      getRaw(array);
      return (this);
   }


   /**
    * Enqueue a request to return this array from the GPU. This method blocks until the array is available.
    * <br/>
    * Note that <code>Kernel.put(type [])</code> calls will delegate to this call.
    * <br/>
    * Package public
    *
    * @param array
    *          It is assumed that this parameter is indeed an array (of int, float, short etc).
    *
    * @see #get(int[] arr)
    * @see #get(int[][] arr)
    * @see #get(int[][][] arr)
    * @see #get(float[] arr)
    * @see #get(float[][] arr)
    * @see #get(float[][][] arr)
    * @see #get(double[] arr)
    * @see #get(double[][] arr)
    * @see #get(double[][][] arr)
    * @see #get(long[] arr)
    * @see #get(long[][] arr)
    * @see #get(long[][][] arr)
    * @see #get(char[] arr)
    * @see #get(char[][] arr)
    * @see #get(char[][][] arr)
    * @see #get(boolean[] arr)
    * @see #get(boolean[][] arr)
    * @see #get(boolean[][][] arr)
    */
   private void getRaw(Object array) {
      if (explicit
            && ((getExecutionMode() == EXECUTION_MODE.GPU) || (getExecutionMode() == EXECUTION_MODE.CPU))) {
         // Only makes sense when we are using OpenCL
         getJNI(kernelRunnerContextHandle, array);
      }
   }

   /**
    * Tag this array so that it is explicitly enqueued before the kernel is executed. <br/>
    * Note that <code>Kernel.put(type [])</code> calls will delegate to this call. <br/>
    * Package public
    *
    * @param array
    *          It is assumed that this parameter is indeed an array (of int, float, short etc).
    * @see #put(int[] arr)
    * @see #put(int[][] arr)
    * @see #put(int[][][] arr)
    * @see #put(float[] arr)
    * @see #put(float[][] arr)
    * @see #put(float[][][] arr)
    * @see #put(double[] arr)
    * @see #put(double[][] arr)
    * @see #put(double[][][] arr)
    * @see #put(long[] arr)
    * @see #put(long[][] arr)
    * @see #put(long[][][] arr)
    * @see #put(char[] arr)
    * @see #put(char[][] arr)
    * @see #put(char[][][] arr)
    * @see #put(boolean[] arr)
    * @see #put(boolean[][] arr)
    * @see #put(boolean[][][] arr)
    */

   private void putRaw(Object array) {
      if (explicit
            && ((getExecutionMode() == EXECUTION_MODE.GPU) || (getExecutionMode() == EXECUTION_MODE.CPU))) {
         // Only makes sense when we are using OpenCL
         puts.add(array);
      }
   }

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

   public void setFallbackExecutionMode() {
      executionMode = EXECUTION_MODE.getFallbackExecutionMode();
   }

   public void freeGPUMemory() {
      if (executionMode == EXECUTION_MODE.GPU && kernelRunnerContextHandle != 0) {
         freeKernelRunnerMemoryJNI(kernelRunnerContextHandle);
      }
   }
}
