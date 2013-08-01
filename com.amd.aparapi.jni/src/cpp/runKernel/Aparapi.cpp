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
   laws, including but not limited to the U.S. Export Administration Regulations ("EAR"), (15 C.F.R. Sections 730 
   through 774), and E.U. Council Regulation (EC) No 1334/2000 of 22 June 2000.  Further, pursuant to Section 740.6 of
   the EAR, you hereby certify that, except pursuant to a license granted by the United States Department of Commerce
   Bureau of Industry and Security or as otherwise permitted pursuant to a License Exception under the U.S. Export 
   Administration Regulations ("EAR"), you will not (1) export, re-export or release to a national of a country in 
   Country Groups D:1, E:1 or E:2 any restricted technology, software, or source code you receive hereunder, or (2) 
   export to Country Groups D:1, E:1 or E:2 the direct product of such technology or software, if such foreign produced
   direct product is subject to national security controls as identified on the Commerce Control List (currently 
   found in Supplement 1 to Part 774 of EAR).  For the most current Country Group listings, or for additional 
   information about the EAR or your obligations under those regulations, please refer to the U.S. Bureau of Industry
   and Security?s website at http://www.bis.doc.gov/. 
   */

#define APARAPI_SOURCE

//this is a workaround for windows machines since <windows.h> defines min/max that break code.
#define NOMINMAX

#include "Aparapi.h"
#include "Config.h"
#include "ProfileInfo.h"
#include "ArrayBuffer.h"
#include "AparapiBuffer.h"
#include "CLHelper.h"
#include "List.h"
#include "BufferManager.h"
#include "OpenCLJNI.h"
#include <algorithm>
#include <list>

std::list<KernelRunnerContext*> kernelRunnerContextList;
bool isInitialized = false;


/**
 * Global initialization. This currently boils down to creating a config object if required.
 */
void initialize(JNIEnv* jenv) {
   if (isInitialized) return;

   isInitialized = true;
   if (config == NULL){
      config = new Config(jenv);
   }
}

/**
 * Open up an initial profile file for the executing kernel.
 *
 * @param jenv JNI environment
 * @param kernelContext context for the currently executing kernel
 */
void writeProfile(JNIEnv* jenv, KernelContext* kernelContext) {
   // compute profile filename
   // indicate cpu or gpu
   // timestamp
   // kernel name

   jclass classMethodAccess = jenv->FindClass("java/lang/Class"); 
   jmethodID getNameID = jenv->GetMethodID(classMethodAccess,"getName","()Ljava/lang/String;");
   jstring className = (jstring)jenv->CallObjectMethod(kernelContext->kernelClass, getNameID);
   const char *classNameChars = jenv->GetStringUTFChars(className, NULL);

   const size_t TIME_STR_LEN = 200;

   char timeStr[TIME_STR_LEN];
   struct tm *tmp;
   time_t t = time(NULL);
   tmp = localtime(&t);
   if (tmp == NULL) {
      perror("localtime");
   }
   //strftime(timeStr, TIME_STR_LEN, "%F.%H%M%S", tmp);  %F seemed to cause a core dump
   strftime(timeStr, TIME_STR_LEN, "%H%M%S", tmp);

   char* fnameStr = new char[strlen(classNameChars) + strlen(timeStr) + 128];
   jint pid = getProcess();

   //sprintf(fnameStr, "%s.%s.%d.%llx\n", classNameChars, timeStr, pid, kernelContext);
   sprintf(fnameStr, "aparapiprof.%s.%d.%p", timeStr, pid, kernelContext);
   jenv->ReleaseStringUTFChars(className, classNameChars);

   FILE* profileFile = fopen(fnameStr, "w");
   if (profileFile != NULL) {
      kernelContext->profileFile = profileFile;
   } else {
      kernelContext->profileFile = stderr;
      fprintf(stderr, "Could not open profile data file %s, reverting to stderr\n", fnameStr);
   }
   delete []fnameStr;
}

/**
 * calls either GetCurrentProcessId or getpid depending on if we're on WIN32 or any other system
 * conveiniece function so we don't have to have #ifdefs all over the code
 */
jint getProcess() {
   #if defined (_WIN32)
      return GetCurrentProcessId();
   #else
      return (jint)getpid();
   #endif
}

/**
 * If we are profiling events then test a first event, and report profiling info.
 *
 * @param kernelRunnerContext context holding the information about the OpenCL context
 * @param kernelContext the context holding the information we got from Java
 *
 * @throws CLException
 */
void profileFirstRun(KernelRunnerContext* kernelRunnerContext, KernelContext* kernelContext) {
   cl_event firstEvent;
   int status = CL_SUCCESS;

   status = enqueueMarker(kernelRunnerContext->commandQueue, &firstEvent);
   if (status != CL_SUCCESS) throw CLException(status, "clEnqueueMarker endOfTxfers");

   status = clWaitForEvents(1, &firstEvent);
   if (status != CL_SUCCESS) throw CLException(status,"clWaitForEvents");

   status = clGetEventProfilingInfo(firstEvent, CL_PROFILING_COMMAND_QUEUED, sizeof(kernelContext->profileBaseTime), &(kernelContext->profileBaseTime), NULL);
   if (status != CL_SUCCESS) throw CLException(status, "clGetEventProfilingInfo#1");

   clReleaseEvent(firstEvent);
   if (status != CL_SUCCESS) throw CLException(status, "clReleaseEvent() read event");

   if (config->isVerbose()) {
      fprintf(stderr, "profileBaseTime %lu \n", (unsigned long)kernelContext->profileBaseTime);
   }
}

/**
 * Calls either clEnqueueMarker or clEnqueueMarkerWithWaitList 
 * depending on the version of OpenCL installed.
 * convenience function so we don't have to have #ifdefs all over the code
 *
 * Actually I backed this out (Gary) when issue #123 was reported.  This involved
 * a build on a 1.2 compatible platform which failed on a platform with a 1.1 runtime. 
 * Failed to link. 
 * The answer is to set   -DCL_USE_DEPRECATED_OPENCL_1_1_APIS at compile time and *not* use 
 * the CL_VERSION_1_2 ifdef.
 */
int enqueueMarker(cl_command_queue commandQueue, cl_event* firstEvent) {
   return clEnqueueMarker(commandQueue, firstEvent);
}


/**
 * Step through all non-primitive (arrays) args and determine if the fields have changed.
 * A field may have been re-assigned by Java code to NULL or another instance. 
 * If we detect a change then we discard the previous cl_mem buffer. The caller will detect 
 * that the buffers are null and will create new cl_mem buffers. 
 * 
 * @param jenv the java environment
 * @param jobj the object we might be updating
 * @param kernelContext the context we're working in
 *
 * @throws CLException
 */
jint updateNonPrimitiveReferences(JNIEnv *jenv, jobject jobj, KernelRunnerContext* kernelRunnerContext, KernelContext* kernelContext) {
   cl_int status = CL_SUCCESS;
   if (kernelContext != NULL){
      for (jint i = 0; i < kernelContext->argc; i++){ 
         
         KernelArg *arg = kernelContext->args[i];
         arg->updateReference(jenv, kernelRunnerContext->bufferManager);
      } // for each arg
   } // if kernelContext != NULL
   return(status);
}

/**
 * Processes all of the arguments for the OpenCL Kernel that we got from the KernelContext
 *
 * @param jenv the java environment
 * @param kernelRunnerContext context holding the information about the OpenCL context
 * @param kernelContext the context with the arguements
 * @param argPos out: the absolute position of the last argument
 * @param writeEventCount out: the number of arguements that could be written to
 *
 * @throws CLException
 */
int processArgs(JNIEnv* jenv, KernelRunnerContext* kernelRunnerContext, KernelContext* kernelContext, int& argPos, int& writeEventCount) {

   cl_int status = CL_SUCCESS;

   // argPos is used to keep track of the kernel arg position, it can 
   // differ from "argIdx" due to insertion of javaArrayLength args which are not
   // fields read from the kernel object.
   for (int argIdx = 0; argIdx < kernelContext->argc; argIdx++, argPos++) {

      KernelArg *arg = kernelContext->args[argIdx];

      // make sure that the JNI arg reflects the latest type info from the instance.
      // For example if the buffer is tagged as explicit and needs to be pushed
      arg->syncType(jenv);

      if (config->isVerbose()){
         fprintf(stderr, "got type for arg %d, %s, type=%08x\n", argIdx, arg->name, arg->type);
      }

      if (!arg->isPrimitive() && !arg->isLocal()) {
          processObject(jenv, kernelRunnerContext, kernelContext, arg, argPos, argIdx);

          if (arg->needToEnqueueWrite() && (!arg->isConstant() || arg->isExplicitWrite())) {
              if (config->isVerbose()) {
                  fprintf(stderr, "%swriting %s%sbuffer argIndex=%d argPos=%d %s\n",  
                        (arg->isExplicit() ? "explicitly " : ""), 
                        (arg->isConstant() ? "constant " : ""), 
                        (arg->isLocal() ? "local " : ""), 
                        argIdx,
                        argPos,
                        arg->name);
              }
              updateWriteEvents(jenv, kernelRunnerContext, kernelContext, arg, argIdx, writeEventCount);
          }
      } else if (arg->isLocal()) {
          processLocal(jenv, kernelContext, arg, argPos, argIdx);
      } else {  // primitive arguments
         status = arg->setPrimitiveArg(jenv, argIdx, argPos, config->isVerbose());
         if(status != CL_SUCCESS) throw CLException(status,"clSetKernelArg()");
      }

   }  // for each arg
   return status;
}

/**
 * Manages the memory of KernelArgs that are object.  i.e. handels pinning, and moved objects.
 * Currently the only objects supported are arrays.
 *
 * @param jenv the java environment
 * @param kernelRunnerContext context holding the information about the OpenCL context
 * @param kernelContext the context we got from java
 * @param arg the argument we are processing
 * @param argPos out: the position of arg in the opencl argument list
 * @param argIdx the position of arg in the argument array
 *
 * @throws CLException
 */
void processObject(JNIEnv* jenv, KernelRunnerContext* kernelRunnerContext, KernelContext* kernelContext, KernelArg* arg, int& argPos, int argIdx) {
    if(arg->isArray()) {
       arg->arrayBuffer->process(jenv, kernelRunnerContext->context, kernelContext, arg, argPos, argIdx);
    } else if(arg->isAparapiBuffer()) {
       arg->aparapiBuffer->process(jenv, kernelRunnerContext->context, kernelContext, arg, argPos, argIdx);
    }
}

/**
 * Keeps track of write events for KernelArgs.
 *
 * @param jenv the java envrionment
 * @param kernelRunnerContext context holding the information about the OpenCL context
 * @param kernelContext the context we got from java
 * @param arg the KernelArg to create a write event for
 * @param argIdx the position of arg in the argument array
 * @param writeEventCount out: the number of write events we've created so far
 *
 * @throws CLException
 */
void updateWriteEvents(JNIEnv* jenv, KernelRunnerContext* kernelRunnerContext, KernelContext* kernelContext, KernelArg* arg, int argIdx, int& writeEventCount) {

   cl_int status = CL_SUCCESS;

   // we only enqueue a write if we know the kernel actually reads the buffer 
   // or if there is an explicit write pending
   // the default behavior for Constant buffers is also that there is no write enqueued unless explicit

   if (config->isProfilingEnabled()) {
      kernelContext->writeEventArgs[writeEventCount] = argIdx;
   }

   if(arg->isArray()) {
      status = clEnqueueWriteBuffer(kernelRunnerContext->commandQueue, arg->arrayBuffer->mem, CL_FALSE, 0, 
         arg->arrayBuffer->lengthInBytes, arg->arrayBuffer->addr, 0, NULL, &(kernelContext->writeEvents[writeEventCount]));
   } else if(arg->isAparapiBuffer()) {
      status = clEnqueueWriteBuffer(kernelRunnerContext->commandQueue, arg->aparapiBuffer->mem, CL_FALSE, 0, 
         arg->aparapiBuffer->lengthInBytes, arg->aparapiBuffer->data, 0, NULL, &(kernelContext->writeEvents[writeEventCount]));
   }
   if(status != CL_SUCCESS) throw CLException(status,"clEnqueueWriteBuffer");

   if (config->isTrackingOpenCLResources()){
      writeEventList.add(kernelContext->writeEvents[writeEventCount],__LINE__, __FILE__);
   }
   writeEventCount++;
   if (arg->isExplicit() && arg->isExplicitWrite()){
      if (config->isVerbose()){
         fprintf(stderr, "clearing explicit buffer bit %d %s\n", argIdx, arg->name);
      }
      arg->clearExplicitBufferBit(jenv);
   }
}

void processLocal(JNIEnv* jenv, KernelContext* kernelContext, KernelArg* arg, int& argPos, int argIdx) {
   if(arg->isArray()) processLocalArray(jenv,kernelContext,arg,argPos,argIdx);
   if(arg->isAparapiBuffer()) processLocalBuffer(jenv,kernelContext,arg,argPos,argIdx);
}

/**
 * Sets the opencl kernel argument for local args.
 *
 * @param jenv the java envrionment
 * @param kernelContext the context we got from java
 * @param arg the KernelArg to create a write event for
 * @param argPos out: the position of arg in the opencl argument list
 * @param argIdx the position of arg in the argument array
 *
 * @throws CLException
 */
void processLocalArray(JNIEnv* jenv, KernelContext* kernelContext, KernelArg* arg, int& argPos, int argIdx) {

   cl_int status = CL_SUCCESS;
   // what if local buffer size has changed?  We need a check for resize here.
   if (kernelContext->firstRun) {
      status = kernelContext->setLocalBufferArg(jenv, argIdx, argPos, config->isVerbose(), arg);
      if(status != CL_SUCCESS) throw CLException(status,"clSetKernelArg() (local)");

      // Add the array length if needed
      if (arg->usesArrayLength()) {
         arg->syncJavaArrayLength(jenv);

         status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(jint), &(arg->arrayBuffer->length));

         if (config->isVerbose()){
            fprintf(stderr, "runKernel arg %d %s, javaArrayLength = %d\n", argIdx, arg->name, arg->arrayBuffer->length);
         }

         if(status != CL_SUCCESS) throw CLException(status,"clSetKernelArg (array length)");

      }
   } else {
      // Keep the arg position in sync if no updates were required
      if (arg->usesArrayLength()) {
         argPos++;
      }
   }
}

/**
 * Sets the opencl kernel arguement for local args.
 *
 * @param jenv the java envrionment
 * @param kernelContext the context we got from java
 * @param arg the KernelArg to create a write event for
 * @param argPos out: the position of arg in the opencl argument list
 * @param argIdx the position of arg in the argument array
 *
 * @throws CLException
 */
void processLocalBuffer(JNIEnv* jenv, KernelContext* kernelContext, KernelArg* arg, int& argPos, int argIdx) {

   cl_int status = CL_SUCCESS;
   // what if local buffer size has changed?  We need a check for resize here.
   if (kernelContext->firstRun) {
      status = kernelContext->setLocalBufferArg(jenv, argIdx, argPos, config->isVerbose(), arg);
      if(status != CL_SUCCESS) throw CLException(status,"clSetKernelArg() (local)");

      // Add the array length if needed
      if (arg->usesArrayLength()) {
         arg->syncJavaArrayLength(jenv);

         for(int i = 0; i < arg->aparapiBuffer->numDims; i++)
         {
             int length = arg->aparapiBuffer->lens[i];
             status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(jint), &length);
             if (config->isVerbose()){
                fprintf(stderr, "runKernel arg %d %s, javaArrayLength = %d\n", argIdx, arg->name, length);
             }
             if(status != CL_SUCCESS) throw CLException(status,"clSetKernelArg (array length)");
         }
      }
   } else {
      // Keep the arg position in sync if no updates were required
      if (arg->usesArrayLength()) {
         argPos += arg->aparapiBuffer->numDims;
      }
   }
}



/**
 * Enqueus the current kernel to run on opencl
 *
 * @param kernelRunnerContext context holding the information about the OpenCL context
 * @param kernelContext the context with the arguements
 * @param range the range that the kernel is running over
 * @param passes the number of passes for the kernel
 * @param argPos the number of arguments we passed to the kernel
 * @param writeEventCount the number of arguement that will be updated
 *
 * @throws CLException
 */
void enqueueKernel(KernelRunnerContext* kernelRunnerContext, KernelContext* kernelContext, Range& range, int passes, int argPos, int writeEventCount){
   // We will need to revisit the execution of multiple devices.  
   // POssibly cloning the range per device and mutating each to handle a unique subrange (of global) and
   // maybe even pushing the offset into the range class.

   //   size_t globalSize_0AsSizeT = (range.globalDims[0] /kernelContext->deviceIdc);
   //   size_t localSize_0AsSizeT = range.localDims[0];

   // To support multiple passes we add a 'secret' final arg called 'passid' and just schedule multiple enqueuendrange kernels.  Each of which having a separate value of passid


   // delete the last set
   if (kernelContext->exec) {
      delete kernelContext->exec;
      kernelContext->exec = NULL;
   } 
   kernelContext->passes = passes;
   kernelContext->exec = new ProfileInfo[passes];

   cl_int status = CL_SUCCESS;
   for (int passid=0; passid < passes; passid++) {

      //size_t offset = 1; // (size_t)((range.globalDims[0]/kernelContext->deviceIdc)*dev);
      status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(passid), &(passid));
      if (status != CL_SUCCESS) throw CLException(status, "clSetKernelArg() (passid)");

      // wait for this event count
      int writeCount = 0;
      // list of events to wait for
      cl_event* writeEvents = NULL;



      // -----------
      // fix for Mac OSX CPU driver (and possibly others) 
      // which fail to give correct maximum work group info
      // while using clGetDeviceInfo
      // see: http://www.openwall.com/lists/john-dev/2012/04/10/4
      cl_uint max_group_size[3];
      status = clGetKernelWorkGroupInfo(kernelContext->kernel,
                                        kernelRunnerContext->deviceId,
                                        CL_KERNEL_WORK_GROUP_SIZE,
                                        sizeof(max_group_size),
                                        &max_group_size, NULL);
      
      if (status != CL_SUCCESS) {
         CLException(status, "clGetKernelWorkGroupInfo()").printError();
      } else {
         range.localDims[0] = std::min((cl_uint)range.localDims[0], max_group_size[0]);
         if (range.globalDims[0] % range.localDims[0] != 0) {
            int groupCount = (range.globalDims[0] / range.localDims[0]) + 1;
            range.globalDims[0] = range.localDims[0] * groupCount;
         }
      }
      // ------ end fix

        
      // two options here due to passid
      // there may be 1 or more passes
      // enqueue depends on write enqueues 
      // we don't block but and we populate the executeEvents
      if (passid == 0) {

         writeCount = writeEventCount;
         if(writeEventCount > 0) {
            writeEvents = kernelContext->writeEvents;
         }

      // we are in some passid > 0 pass 
      // maybe middle or last!
      // we don't depend on write enqueues
      // we block and do supply executeEvents 
      } else {
         //fprintf(stderr, "setting passid to %d of %d not first not last\n", passid, passes);
         
         status = clWaitForEvents(1, &kernelContext->executeEvents[0]);
         if (status != CL_SUCCESS) throw CLException(status, "clWaitForEvents() execute event");

         if (config->isTrackingOpenCLResources()) {
            executeEventList.remove(kernelContext->executeEvents[0],__LINE__, __FILE__);
         }

         status = clReleaseEvent(kernelContext->executeEvents[0]);
         if (status != CL_SUCCESS) throw CLException(status, "clReleaseEvent() read event");

         // We must capture any profile info for passid-1  so we must wait for the last execution to complete
         if (passid == 1 && config->isProfilingEnabled()) {

            // Now we can profile info for passid-1 
            status = profile(&kernelContext->exec[passid-1], &kernelContext->executeEvents[0], 1, NULL, kernelContext->profileBaseTime);
            if (status != CL_SUCCESS) throw CLException(status,"");
         }

      }

      status = clEnqueueNDRangeKernel(
            kernelRunnerContext->commandQueue,
            kernelContext->kernel,
            range.dims,
            range.offsets,
            range.globalDims,
            range.localDims,
            writeCount,
            writeEvents,
            &kernelContext->executeEvents[0]);
      clFinish(kernelRunnerContext->commandQueue);

      if (status != CL_SUCCESS) {

         for(int i = 0; i<range.dims;i++) {
            fprintf(stderr, "after clEnqueueNDRangeKernel, globalSize[%d] = %d, localSize[%d] = %d\n",
                  i, (int)range.globalDims[i], i, (int)range.localDims[i]);
         }
         throw CLException(status, "clEnqueueNDRangeKernel()");
      }

      if(config->isTrackingOpenCLResources()){
         executeEventList.add(kernelContext->executeEvents[0],__LINE__, __FILE__);
      }
    
   }
}

// Should failed profiling abort the run and return early?
cl_int profile(ProfileInfo *profileInfo, cl_event *event, jint type, char* name, cl_ulong profileBaseTime ) {

   cl_int status = CL_SUCCESS;

   try {
      status = clGetEventProfilingInfo(*event, CL_PROFILING_COMMAND_QUEUED, sizeof(profileInfo->queued), &(profileInfo->queued), NULL);
      if(status != CL_SUCCESS) throw CLException(status, "clGetEventProfiliningInfo() QUEUED");

      status = clGetEventProfilingInfo(*event, CL_PROFILING_COMMAND_SUBMIT, sizeof(profileInfo->submit), &(profileInfo->submit), NULL);
      if(status != CL_SUCCESS) throw CLException(status, "clGetEventProfiliningInfo() SUBMIT");

      status = clGetEventProfilingInfo(*event, CL_PROFILING_COMMAND_START, sizeof(profileInfo->start), &(profileInfo->start), NULL);
      if(status != CL_SUCCESS) throw CLException(status, "clGetEventProfiliningInfo() START");

      status = clGetEventProfilingInfo(*event, CL_PROFILING_COMMAND_END, sizeof(profileInfo->end), &(profileInfo->end), NULL);
      if(status != CL_SUCCESS) throw CLException(status, "clGetEventProfiliningInfo() END");

   } catch(CLException& cle) {
     cle.printError();
     return cle.status();
   }

   profileInfo->queued -= profileBaseTime;
   profileInfo->submit -= profileBaseTime;
   profileInfo->start -= profileBaseTime;
   profileInfo->end -= profileBaseTime;
   profileInfo->type = type;
   profileInfo->name = name;
   profileInfo->valid = true;

   return status;
}

/**
 * Set readEvents and readArgEvents
 * readEvents[] will be populated with the event's that we will wait on below.  
 * readArgEvents[] will map the readEvent to the arg that originated it
 * So if we had
 *     arg[0]  read_write array
 *     arg[1]  read array
 *     arg[2]  write array
 *     arg[3]  primitive
 *     arg[4]  read array
 * At the end of the call
 *     readCount=3
 *     readEvent[0] = new read event for arg0
 *     readArgEvent[0] = 0
 *     readEvent[1] = new read event for arg1
 *     readArgEvent[1] = 1
 *     readEvent[2] = new read event for arg4
 *     readArgEvent[2] = 4
 *
 * @param jenv the java envrionment
 * @param kernelRunnerContext context holding the information about the OpenCL context
 * @param kernelContext the context we got from Java
 *
 * @return number of reads. 
 * It will never be > kernelContext->argc which is the size of readEvents[] and readEventArgs[]
 *
 * @throws CLException
 */
int getReadEvents(JNIEnv* jenv, KernelRunnerContext* kernelRunnerContext, KernelContext* kernelContext) {

   int readEventCount = 0; 

   cl_int status = CL_SUCCESS;
   for (int i=0; i< kernelContext->argc; i++) {
      KernelArg *arg = kernelContext->args[i];

      if (arg->needToEnqueueRead()){
         if (arg->isConstant()){
            fprintf(stderr, "reading %s\n", arg->name);
         }
         if (config->isProfilingEnabled()) {
            kernelContext->readEventArgs[readEventCount] = i;
         }
         if (config->isVerbose()){
            fprintf(stderr, "reading buffer %d %s\n", i, arg->name);
         }

         if(arg->isArray()) {
            status = clEnqueueReadBuffer(kernelRunnerContext->commandQueue, arg->arrayBuffer->mem, 
                CL_FALSE, 0, arg->arrayBuffer->lengthInBytes, arg->arrayBuffer->addr, 1, 
                kernelContext->executeEvents, &(kernelContext->readEvents[readEventCount]));
         } else if(arg->isAparapiBuffer()) {
            status = clEnqueueReadBuffer(kernelRunnerContext->commandQueue, arg->aparapiBuffer->mem, 
                CL_TRUE, 0, arg->aparapiBuffer->lengthInBytes, arg->aparapiBuffer->data, 1, 
                kernelContext->executeEvents, &(kernelContext->readEvents[readEventCount]));
            arg->aparapiBuffer->inflate(jenv, arg);
         }

         if (status != CL_SUCCESS) throw CLException(status, "clEnqueueReadBuffer()");

         if (config->isTrackingOpenCLResources()){
            readEventList.add(kernelContext->readEvents[readEventCount],__LINE__, __FILE__);
         }
         readEventCount++;
      }
   }
   return readEventCount;
}


/**
 * Wait for read events and release them afterwards.
 *
 * @param kernelContext the context we got from Java
 * @param readEventCount the number of read events to wait for
 * @param passes the number of passes for the kernel
 *
 * @throws CLException
 */
void waitForReadEvents(KernelContext* kernelContext, int readEventCount, int passes) {

   // don't change the order here
   // We wait for the reads which each depend on the execution, which depends on the writes ;)
   // So after the reads have completed, we can release the execute and writes.
   
   cl_int status = CL_SUCCESS;

   if (readEventCount > 0){

      status = clWaitForEvents(readEventCount, kernelContext->readEvents);
      if (status != CL_SUCCESS) throw CLException(status, "clWaitForEvents() read events");

      for (int i=0; i < readEventCount; i++){

         if (config->isProfilingEnabled()) {

            status = profile(&kernelContext->args[kernelContext->readEventArgs[i]]->arrayBuffer->read, 
               &kernelContext->readEvents[i], 0,kernelContext->args[kernelContext->readEventArgs[i]]->name, kernelContext->profileBaseTime);
            if (status != CL_SUCCESS) throw CLException(status, "");
         }
         status = clReleaseEvent(kernelContext->readEvents[i]);
         if (status != CL_SUCCESS) throw CLException(status, "clReleaseEvent() read event");

         if (config->isTrackingOpenCLResources()){
            readEventList.remove(kernelContext->readEvents[i],__LINE__, __FILE__);
         }
      }
   } else {
      // if readEventCount == 0 then we don't need any reads so we just wait for the executions to complete
      status = clWaitForEvents(1, kernelContext->executeEvents);
      if (status != CL_SUCCESS) throw CLException(status, "clWaitForEvents() execute event");
   }

   if (config->isTrackingOpenCLResources()){
      executeEventList.remove(kernelContext->executeEvents[0],__LINE__, __FILE__);
   }
   if (config->isProfilingEnabled()) {
      status = profile(&kernelContext->exec[passes-1], &kernelContext->executeEvents[0], 1, NULL, kernelContext->profileBaseTime); // multi gpu ?
      if (status != CL_SUCCESS) throw CLException(status, "");
   }
}

/**
 * Check to make sure OpenCL exited correctly and update java memory.
 *
 * @param jenv the java environment
 * @param kernelContext the context we got from Java
 * @param writeEventCount the number of write events to wait for
 *
 * @throws CLException
 */
void checkEvents(JNIEnv* jenv, KernelContext* kernelContext, int writeEventCount) {
   // extract the execution status from the executeEvent
   cl_int status;
   cl_int executeStatus;

   status = clGetEventInfo(kernelContext->executeEvents[0], CL_EVENT_COMMAND_EXECUTION_STATUS, sizeof(cl_int), &executeStatus, NULL);
   if (status != CL_SUCCESS) throw CLException(status, "clGetEventInfo() execute event");
   if (executeStatus != CL_COMPLETE) throw CLException(executeStatus, "Execution status of execute event");

   status = clReleaseEvent(kernelContext->executeEvents[0]);
   if (status != CL_SUCCESS) throw CLException(status, "clReleaseEvent() read event");

   for (int i = 0; i < writeEventCount; i++) {

      if (config->isProfilingEnabled()) {
         profile(&kernelContext->args[kernelContext->writeEventArgs[i]]->arrayBuffer->write, &kernelContext->writeEvents[i], 2, kernelContext->args[kernelContext->writeEventArgs[i]]->name, kernelContext->profileBaseTime);
      }

      status = clReleaseEvent(kernelContext->writeEvents[i]);
      if (status != CL_SUCCESS) throw CLException(status, "clReleaseEvent() write event");

      if (config->isTrackingOpenCLResources()){
         writeEventList.remove(kernelContext->writeEvents[i],__LINE__, __FILE__);
      }
   }

   kernelContext->unpinAll(jenv);

   if (config->isProfilingCSVEnabled()) {
      writeProfileInfo(kernelContext);
   }
   if (config->isTrackingOpenCLResources()){
      fprintf(stderr, "following execution of kernel{\n");
      commandQueueList.report(stderr);
      memList.report(stderr); 
      readEventList.report(stderr); 
      executeEventList.report(stderr); 
      writeEventList.report(stderr); 
      fprintf(stderr, "}\n");
   }

   kernelContext->firstRun = false;
}

/**
 * Write out the final profile info.
 *
 * @param kernelContext the context we got from Java
 */
jint writeProfileInfo(KernelContext* kernelContext){
   cl_ulong currSampleBaseTime = -1;
   int pos = 1;

   if (kernelContext->firstRun) {
      fprintf(kernelContext->profileFile, "# PROFILE Name, queued, submit, start, end (microseconds)\n");
   }       

   // A read by a user kernel means the OpenCL layer wrote to the kernel and vice versa
   for (int i=0; i< kernelContext->argc; i++){
      KernelArg *arg=kernelContext->args[i];
      if (arg->isBackedByArray() && arg->isReadByKernel()){

         // Initialize the base time for this sample
         if (currSampleBaseTime == -1) {
            currSampleBaseTime = arg->arrayBuffer->write.queued;
         } 
         fprintf(kernelContext->profileFile, "%d write %s,", pos++, arg->name);

         fprintf(kernelContext->profileFile, "%lu,%lu,%lu,%lu,",  
        	(unsigned long)(arg->arrayBuffer->write.queued - currSampleBaseTime)/1000,
        	(unsigned long)(arg->arrayBuffer->write.submit - currSampleBaseTime)/1000,
        	(unsigned long)(arg->arrayBuffer->write.start - currSampleBaseTime)/1000,
        	(unsigned long)(arg->arrayBuffer->write.end - currSampleBaseTime)/1000);
      }
   }

   for (jint pass=0; pass<kernelContext->passes; pass++){

      // Initialize the base time for this sample if necessary
      if (currSampleBaseTime == -1) {
         currSampleBaseTime = kernelContext->exec[pass].queued;
      } 

      // exec 
      fprintf(kernelContext->profileFile, "%d exec[%d],", pos++, pass);

      fprintf(kernelContext->profileFile, "%lu,%lu,%lu,%lu,",  
            (unsigned long)(kernelContext->exec[pass].queued - currSampleBaseTime)/1000,
            (unsigned long)(kernelContext->exec[pass].submit - currSampleBaseTime)/1000,
            (unsigned long)(kernelContext->exec[pass].start - currSampleBaseTime)/1000,
            (unsigned long)(kernelContext->exec[pass].end - currSampleBaseTime)/1000);
   }

   // 
   if ( kernelContext->argc == 0 ) {
      fprintf(kernelContext->profileFile, "\n");
   } else { 
      for (int i=0; i< kernelContext->argc; i++){
         KernelArg *arg=kernelContext->args[i];
         if (arg->isBackedByArray() && arg->isMutableByKernel()){

            // Initialize the base time for this sample
            if (currSampleBaseTime == -1) {
               currSampleBaseTime = arg->arrayBuffer->read.queued;
            }

            fprintf(kernelContext->profileFile, "%d read %s,", pos++, arg->name);

            fprintf(kernelContext->profileFile, "%lu,%lu,%lu,%lu,",  
            	(unsigned long)(arg->arrayBuffer->read.queued - currSampleBaseTime)/1000,
            	(unsigned long)(arg->arrayBuffer->read.submit - currSampleBaseTime)/1000,
            	(unsigned long)(arg->arrayBuffer->read.start - currSampleBaseTime)/1000,
            	(unsigned long)(arg->arrayBuffer->read.end - currSampleBaseTime)/1000);
         }
      }
   }
   fprintf(kernelContext->profileFile, "\n");
   return(0);
}

/**
 * Find the arguement in our list of KernelArgs that matches the array the user asked for.
 *
 * @param jenv the java environment
 * @param kernelContext the context we're working in
 * @param buffer the array we're looking for
 *
 * @return the KernelArg representing the array
 */
KernelArg* getArgForBuffer(JNIEnv* jenv, KernelContext* kernelContext, jobject buffer) {
   KernelArg *returnArg = NULL;

   if (kernelContext != NULL){
      for (jint i = 0; returnArg == NULL && i < kernelContext->argc; i++){ 
         KernelArg *arg = kernelContext->args[i];
         if (arg->isArray()) {
            jboolean isSame = jenv->IsSameObject(buffer, arg->arrayBuffer->javaObject);
            if (isSame){
               if (config->isVerbose()){
                  fprintf(stderr, "matched arg '%s'\n", arg->name);
               }
               returnArg = arg;
            }else{
               if (config->isVerbose()){
                  fprintf(stderr, "unmatched arg '%s'\n", arg->name);
               }
            }
         } else if(arg->isAparapiBuffer()) {
            jboolean isSame = jenv->IsSameObject(buffer, arg->aparapiBuffer->getJavaObject(jenv,arg));
            if (isSame) {
               if (config->isVerbose()) {
                  fprintf(stderr, "matched arg '%s'\n", arg->name);
               }
               returnArg = arg;
            } else {
               if (config->isVerbose()) {
                  fprintf(stderr, "unmatched arg '%s'\n", arg->name);
               }
            }
         }
      }
      if (returnArg == NULL){
         if (config->isVerbose()){
            fprintf(stderr, "attempt to get arg for buffer that does not appear to be referenced from kernel\n");
         }
      }
   }
   return returnArg;
}



// -------------- JNI methods ----------------------- //

JNI_JAVA(jlong, KernelRunnerJNI, initKernelRunnerJNI)
   (JNIEnv *jenv, jobject jobj, jobject openCLDeviceObject, jint flags) {

      initialize(jenv);

      if (openCLDeviceObject == NULL){
         fprintf(stderr, "no device object!\n");
      }

      KernelRunnerContext* context = KernelRunnerContext::contextFor(jenv, openCLDeviceObject, flags, config);

      std::list<KernelRunnerContext*>::iterator it = kernelRunnerContextList.begin();
      kernelRunnerContextList.insert(it, context);

      commandQueueList.add(context->commandQueue, __LINE__, __FILE__);

      return (jlong) context;
}

JNI_JAVA(jlong, KernelRunnerJNI, initKernelJNI)
   (JNIEnv *jenv, jobject jobj, jlong kernelRunnerHandle, jobject kernelObject) {
      
      initialize(jenv);

      KernelRunnerContext* kernelRunnerContext = KernelRunnerContext::getKernelRunnerContext(kernelRunnerHandle);

      cl_int status = CL_SUCCESS;
      KernelContext* kernelContext = new KernelContext(jenv, kernelObject);

      kernelRunnerContext->registerKernelContext(kernelContext);

      return((jlong)kernelContext);
}

JNI_JAVA(jlong, KernelRunnerJNI, buildProgramJNI)
   (JNIEnv *jenv, jobject jobj, jlong kernelRunnerContextHandle, jlong kernelContextHandle, jstring source) {

      initialize(jenv);

      KernelRunnerContext* kernelRunnerContext = KernelRunnerContext::getKernelRunnerContext(kernelRunnerContextHandle);
      KernelContext* kernelContext = KernelContext::getKernelContext(kernelContextHandle);

      if (kernelContext == NULL || kernelRunnerContext == 0){
         return 0;
      }

      try {
         cl_int status = CL_SUCCESS;

         kernelContext->program = CLHelper::compile(jenv, kernelRunnerContext->context,  1, &kernelRunnerContext->deviceId, source, NULL, &status);

         if(status == CL_BUILD_PROGRAM_FAILURE) throw CLException(status, "");

         kernelContext->kernel = clCreateKernel(kernelContext->program, "run", &status);
         if(status != CL_SUCCESS) throw CLException(status,"clCreateKernel()");


         if (config->isProfilingCSVEnabled()) {
            writeProfile(jenv, kernelContext);
         }
      } catch(CLException& cle) {
         cle.printError();
         return 0;
      }
      
      return((jlong)kernelContext);
}

JNI_JAVA(jint, KernelRunnerJNI, setArgsJNI)
   (JNIEnv *jenv, jobject jobj, jlong kernelRunnerContextHandle, jlong kernelContextHandle, jobjectArray argArray, jint argc) {
      initialize(jenv);

      KernelRunnerContext* kernelRunnerContext = KernelRunnerContext::getKernelRunnerContext(kernelRunnerContextHandle);
      KernelContext* kernelContext = KernelContext::getKernelContext(kernelContextHandle);

      if (kernelRunnerContext == NULL || kernelContext == NULL) {
         return 0;
      }

      cl_int status = CL_SUCCESS;

      kernelContext->argc = argc;
      kernelContext->args = new KernelArg*[kernelContext->argc];
      kernelContext->firstRun = true;

      // Step through the array of KernelArg's to capture the type data for the Kernel's data members.
      for (jint i = 0; i < kernelContext->argc; i++){ 
         jobject argObj = jenv->GetObjectArrayElement(argArray, i);
         KernelArg* arg = kernelContext->args[i] = new KernelArg(jenv, argObj, kernelContext);
         if (config->isVerbose()){
            if (arg->isExplicit()){
               fprintf(stderr, "%s is explicit!\n", arg->name);
            }
         }

         if (config->isVerbose()){
            fprintf(stderr, "in setArgs arg %d %s type %08x\n", i, arg->name, arg->type);
            if (arg->isLocal()){
               fprintf(stderr, "in setArgs arg %d %s is local\n", i, arg->name);
            }else if (arg->isConstant()){
               fprintf(stderr, "in setArgs arg %d %s is constant\n", i, arg->name);
            }else{
               fprintf(stderr, "in setArgs arg %d %s is *not* local\n", i, arg->name);
            }
         }

         //If an error occurred, return early so we report the first problem, not the last
         if (jenv->ExceptionCheck() == JNI_TRUE) {
            kernelContext->argc = -1;
            delete[] kernelContext->args;
            kernelContext->args = NULL;
            kernelContext->firstRun = true;
            return (status);
         }

      }
      // we will need an executeEvent buffer for all devices
      kernelContext->executeEvents = new cl_event[1];

      // We will need *at most* kernelContext->argc read/write events
      kernelContext->readEvents = new cl_event[kernelContext->argc];
      if (config->isProfilingEnabled()) {
         kernelContext->readEventArgs = new jint[kernelContext->argc];
      }
      kernelContext->writeEvents = new cl_event[kernelContext->argc];
      if (config->isProfilingEnabled()) {
         kernelContext->writeEventArgs = new jint[kernelContext->argc];
      }
      
      return(status);
}

JNI_JAVA(jint, KernelRunnerJNI, runKernelJNI)
   (JNIEnv *jenv, jobject jobj, jlong kernelRunnerContextHandle, jlong kernelContextHandle, 
   jobject _range, jboolean needSync, jint passes) {
      initialize(jenv);

      KernelRunnerContext* kernelRunnerContext = KernelRunnerContext::getKernelRunnerContext(kernelRunnerContextHandle);
      KernelContext* kernelContext = KernelContext::getKernelContext(kernelContextHandle);

      if (kernelRunnerContext == NULL || kernelContext == NULL) {
         return 0;
      }

      Range range(jenv, _range);

      cl_int status = CL_SUCCESS;

      if (kernelContext->firstRun && config->isProfilingEnabled()){
         try {
            profileFirstRun(kernelRunnerContext, kernelContext);
         } catch(CLException& cle) {
            cle.printError();
            return 0L;
         }
      }

      int argPos = 0;
      // Need to capture array refs
      if (kernelContext->firstRun || needSync) {
         try {
            updateNonPrimitiveReferences(jenv, jobj, kernelRunnerContext, kernelContext);
         } catch (CLException& cle) {
             cle.printError();
         }
         if (config->isVerbose()){
            fprintf(stderr, "back from updateNonPrimitiveReferences\n");
         }
      }


      try {
         int writeEventCount = 0;
         processArgs(jenv, kernelRunnerContext, kernelContext, argPos, writeEventCount);

         kernelRunnerContext->bufferManager->cleanUpNonReferencedBuffers(jenv);

         enqueueKernel(kernelRunnerContext, kernelContext, range, passes, argPos, writeEventCount);
         int readEventCount = getReadEvents(jenv, kernelRunnerContext, kernelContext);
         waitForReadEvents(kernelContext, readEventCount, passes);
         checkEvents(jenv, kernelContext, writeEventCount);
      }
      catch(CLException& cle) {
         cle.printError();
         kernelContext->unpinAll(jenv);
         return cle.status();
      }

      return(status);
}

JNI_JAVA(jint, KernelRunnerJNI, getJNI)
   (JNIEnv *jenv, jobject jobj, jlong kernelRunnerContextHandle, jobject buffer) {
      initialize(jenv);

      KernelRunnerContext* kernelRunnerContext = KernelRunnerContext::getKernelRunnerContext(kernelRunnerContextHandle);

      if (kernelRunnerContext == NULL) {
         return 0;
      }

      cl_int status = CL_SUCCESS;

      std::vector<KernelContext*> contextList = kernelRunnerContext->kernelContextList;
      for (std::vector<KernelContext*>::iterator it = contextList.begin(); it != contextList.end(); it++) {
         KernelContext *context = *it;

         KernelArg *arg = getArgForBuffer(jenv, context, buffer);
         if (arg != NULL){
            if (config->isVerbose()){
               fprintf(stderr, "explicitly reading buffer %s\n", arg->name);
            }
            if(arg->isArray()) {
               arg->pin(jenv);

               try {
                  status = clEnqueueReadBuffer(kernelRunnerContext->commandQueue, arg->arrayBuffer->mem, 
                                                CL_FALSE, 0, 
                                                arg->arrayBuffer->lengthInBytes,
                                                arg->arrayBuffer->addr , 0, NULL, 
                                                &context->readEvents[0]);
                  if (config->isVerbose()){
                     fprintf(stderr, "explicitly read %s ptr=%p len=%d\n", 
                              arg->name, arg->arrayBuffer->addr, 
                              arg->arrayBuffer->lengthInBytes );
                  }
                  if (status != CL_SUCCESS) throw CLException(status, "clEnqueueReadBuffer()");

                  status = clWaitForEvents(1, context->readEvents);
                  if (status != CL_SUCCESS) throw CLException(status, "clWaitForEvents");

                  if (config->isProfilingEnabled()) {
                     status = profile(&arg->arrayBuffer->read, &context->readEvents[0], 0,
                                       arg->name, context->profileBaseTime);
                     if (status != CL_SUCCESS) throw CLException(status, "profile ");
                  }

                  status = clReleaseEvent(context->readEvents[0]);
                  if (status != CL_SUCCESS) throw CLException(status, "clReleaseEvent() read event");

                  // since this is an explicit buffer get, 
                  // we expect the buffer to have changed so we commit
                  arg->unpin(jenv); // was unpinCommit

               //something went wrong print the error and exit
               } catch(CLException& cle) {
                  cle.printError();
                  return status;
               }
            } else if(arg->isAparapiBuffer()) {

               try {
                  status = clEnqueueReadBuffer(kernelRunnerContext->commandQueue, arg->aparapiBuffer->mem, 
                                                CL_FALSE, 0, 
                                                arg->aparapiBuffer->lengthInBytes,
                                                arg->aparapiBuffer->data, 0, NULL, 
                                                &context->readEvents[0]);
                  if (config->isVerbose()){
                     fprintf(stderr, "explicitly read %s ptr=%p len=%d\n", 
                              arg->name, arg->aparapiBuffer->data, 
                              arg->aparapiBuffer->lengthInBytes );
                  }
                  if (status != CL_SUCCESS) throw CLException(status, "clEnqueueReadBuffer()");

                  status = clWaitForEvents(1, context->readEvents);
                  if (status != CL_SUCCESS) throw CLException(status, "clWaitForEvents");

                  if (config->isProfilingEnabled()) {
                     status = profile(&arg->aparapiBuffer->read, &context->readEvents[0], 0,
                                       arg->name, context->profileBaseTime);
                     if (status != CL_SUCCESS) throw CLException(status, "profile "); 
                  }

                  status = clReleaseEvent(context->readEvents[0]);
                  if (status != CL_SUCCESS) throw CLException(status, "clReleaseEvent() read event");

                  arg->aparapiBuffer->inflate(jenv,arg);

               //something went wrong print the error and exit
               } catch(CLException& cle) {
                  cle.printError();
                  return status;
               }
            }
         } else {
            if (config->isVerbose()){
               fprintf(stderr, "attempt to request to get a buffer that does not appear to be referenced from kernel\n");
            }
         }
         
      }
      return 0;
}

JNI_JAVA(jint, KernelRunnerJNI, disposeKernelRunnerJNI)
   (JNIEnv *jenv, jobject jobj, jlong kernelRunnerContextHandle) {
      initialize(jenv);

      KernelRunnerContext* kernelRunnerContext = KernelRunnerContext::getKernelRunnerContext(kernelRunnerContextHandle);

      if (kernelRunnerContext == NULL) {
         return 0;
      }

      kernelRunnerContext->dispose(jenv);

      cl_int status = CL_SUCCESS;
      CLException::checkCLError(status, "dispose()");
      
      return(status);
}

JNI_JAVA(jint, KernelRunnerJNI, freeKernelRunnerMemoryJNI)
      (JNIEnv *jenv, jobject jobj, jlong kernelRunnerContextHandle) {
      KernelRunnerContext* kernelRunnerContext = KernelRunnerContext::getKernelRunnerContext(kernelRunnerContextHandle);

      if (kernelRunnerContext == NULL) {
         return 0;
      }

      kernelRunnerContext->disposeMemory(jenv);

      cl_int status = CL_SUCCESS;
      CLException::checkCLError(status, "dispose()");
      
      return(status);

}

JNI_JAVA(jstring, KernelRunnerJNI, getExtensionsJNI)
   (JNIEnv *jenv, jobject jobj, jlong kernelRunnerContextHandle) {
      initialize(jenv);

      KernelRunnerContext* kernelRunnerContext = KernelRunnerContext::getKernelRunnerContext(kernelRunnerContextHandle);

      if (kernelRunnerContext == NULL) {
         return 0;
      }

      cl_int status = CL_SUCCESS;
      jstring jextensions = CLHelper::getExtensions(jenv, kernelRunnerContext->deviceId, &status);

      return jextensions;
}

JNI_JAVA(jobject, KernelRunnerJNI, getProfileInfoJNI)
   (JNIEnv *jenv, jobject jobj, jlong kernelContextHandle) {
      initialize(jenv);

      cl_int status = CL_SUCCESS;
      KernelContext* kernelContext = KernelContext::getKernelContext(kernelContextHandle);
      jobject returnList = NULL;
      if (kernelContext != NULL){
         returnList = JNIHelper::createInstance(jenv, ArrayListClass, VoidReturn );
         if (config->isProfilingEnabled()){

            for (jint i = 0; i < kernelContext->argc; i++){ 
               KernelArg *arg = kernelContext->args[i];
               if (arg->isArray()){
                  if (arg->isMutableByKernel() && arg->arrayBuffer->write.valid){
                     jobject writeProfileInfo = arg->arrayBuffer->write.createProfileInfoInstance(jenv);
                     JNIHelper::callVoid(jenv, returnList, "add", ArgsBooleanReturn(ObjectClassArg), writeProfileInfo);
                  }
               }
            }

            for (jint pass = 0; pass < kernelContext->passes; pass++){
               jobject executeProfileInfo = kernelContext->exec[pass].createProfileInfoInstance(jenv);
               JNIHelper::callVoid(jenv, returnList, "add", ArgsBooleanReturn(ObjectClassArg), executeProfileInfo);
            }

            for (jint i = 0; i < kernelContext->argc; i++){ 
               KernelArg *arg = kernelContext->args[i];
               if (arg->isArray()){
                  if (arg->isReadByKernel() && arg->arrayBuffer->read.valid){
                     jobject readProfileInfo = arg->arrayBuffer->read.createProfileInfoInstance(jenv);
                     JNIHelper::callVoid(jenv, returnList, "add", ArgsBooleanReturn(ObjectClassArg), readProfileInfo);
                  }
               }
            }
         }
      }
      return returnList;
}












////////////////////////////////////////////////








