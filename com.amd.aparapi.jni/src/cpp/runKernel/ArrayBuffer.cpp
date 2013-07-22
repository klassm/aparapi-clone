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
#define ARRAYBUFFER_SOURCE
#include "ArrayBuffer.h"
#include "JNIContext.h"
#include "List.h"

ArrayBuffer::ArrayBuffer(JNIEnv* jenv, jobject localReference):
   length(0),
   addr(NULL),
   isCopy(false),
   isPinned(false){
      jobject globalReference = jenv->NewGlobalRef(localReference);
      this->javaObject = globalReference;
   }

void ArrayBuffer::unpinAbort(JNIEnv *jenv){
   jenv->ReleasePrimitiveArrayCritical((jarray)this->javaObject, addr,JNI_ABORT);
   isPinned = JNI_FALSE;
}
void ArrayBuffer::unpinCommit(JNIEnv *jenv){
   jenv->ReleasePrimitiveArrayCritical((jarray)this->javaObject, addr, 0);
   isPinned = JNI_FALSE;
}
void ArrayBuffer::pin(JNIEnv *jenv){
   void *ptr = addr;
   addr = jenv->GetPrimitiveArrayCritical((jarray)this->javaObject,&isCopy);
   isPinned = JNI_TRUE;
}

void ArrayBuffer::process(JNIEnv* jenv, cl_context context, JNIContext* jniContext, KernelArg* arg, int& argPos, int argIdx) {
   
   cl_int status = CL_SUCCESS;

   if (config->isProfilingEnabled()){
      arg->arrayBuffer->read.valid = false;
      arg->arrayBuffer->write.valid = false;
   }


   // this uses different JNI calls for arrays vs. directBufs
   
   void * prevAddr =  arg->arrayBuffer->addr;

   // pin the arrays so that GC does not move them during the call
   // get the C memory address for the region being transferred

   arg->pin(jenv);

   if (config->isVerbose()) {
      fprintf(stderr, "runKernel: arrayOrBuf ref %p, oldAddr=%p, newAddr=%p, ref.mem=%p isCopy=%s\n",
            this->javaObject, 
            prevAddr,
            this->addr,
            this->mem,
            this->isCopy ? "true" : "false");
      fprintf(stderr, "at memory addr %p, contents: ", this->addr);
      unsigned char *pb = (unsigned char *) this->addr;
      for (int k=0; k<8; k++) {
         fprintf(stderr, "%02x ", pb[k]);
      }
      fprintf(stderr, "\n" );
   }

   // record whether object moved 
   // if we see that isCopy was returned by getPrimitiveArrayCritical, treat that as a move
   bool objectMoved = (this->addr != prevAddr) || this->isCopy;

   if (config->isVerbose()){
      if (arg->isExplicit() && arg->isExplicitWrite()){
         fprintf(stderr, "explicit write of %s\n",  arg->name);
      }
   }

   if (jniContext->firstRun || (this->mem == 0) || objectMoved ){
      if (this->mem == 0) {
         updateArray(jenv, context, jniContext, arg, argPos, argIdx);
      }
   } else {
      // Keep the arg position in sync if no updates were required
      if (arg->usesArrayLength()){
         argPos++;
      }
   }

   // We do not need to create a new memory each time the buffer is accessed, but we
   // want to set the buffer as kernel arg to each KernelArg it is referenced by!
   status = clSetKernelArg(jniContext->kernel, argPos, sizeof(cl_mem), (void *)&(this->mem));
   if(status != CL_SUCCESS) throw CLException(status,"clSetKernelArg (array)");
}

void ArrayBuffer::updateArray(JNIEnv* jenv, cl_context context, JNIContext* jniContext, KernelArg* arg, int& argPos, int argIdx) {

   cl_int status = CL_SUCCESS;
   // if either this is the first run or user changed input array
   // or gc moved something, then we create buffers/args
   cl_uint mask = CL_MEM_USE_HOST_PTR;
   if (arg->isReadByKernel() && arg->isMutableByKernel()) mask |= CL_MEM_READ_WRITE;
   else if (arg->isReadByKernel() && !arg->isMutableByKernel()) mask |= CL_MEM_READ_ONLY;
   else if (arg->isMutableByKernel()) mask |= CL_MEM_WRITE_ONLY;
   arg->arrayBuffer->memMask = mask;

   if (config->isVerbose()) {
      strcpy(arg->arrayBuffer->memSpec,"CL_MEM_USE_HOST_PTR");
      if (mask & CL_MEM_READ_WRITE) strcat(this->memSpec,"|CL_MEM_READ_WRITE");
      if (mask & CL_MEM_READ_ONLY) strcat(this->memSpec,"|CL_MEM_READ_ONLY");
      if (mask & CL_MEM_WRITE_ONLY) strcat(this->memSpec,"|CL_MEM_WRITE_ONLY");

      fprintf(stderr, "%s %d clCreateBuffer(context, %s, size=%08lx bytes, address=%p, &status)\n", arg->name, 
            argIdx, this->memSpec, (unsigned long) this->lengthInBytes, this->addr);
   }

   this->mem = clCreateBuffer(context, this->memMask, 
      this->lengthInBytes, this->addr, &status);


   if(status != CL_SUCCESS) throw CLException(status,"clCreateBuffer");

   if (config->isTrackingOpenCLResources()){
      memList.add(this->mem, __LINE__, __FILE__);
   }

   // Add the array length if needed
   if (arg->usesArrayLength()) {
      argPos++;
      arg->syncJavaArrayLength(jenv);

      status = clSetKernelArg(jniContext->kernel, argPos, sizeof(jint), &(this->length));
      if(status != CL_SUCCESS) throw CLException(status,"clSetKernelArg (array length)");

      if (config->isVerbose()){
         fprintf(stderr, "runKernel arg %d %s, length = %d\n", argIdx, arg->name, this->length);
      }
   }
}