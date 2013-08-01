#ifndef GPUELEMENT_H
#define GPUELEMENT_H

#include "Common.h"
#include "ProfileInfo.h"

class KernelArg;
class KernelContext;

class GPUElement {
	public:	
		// The java object that is contained within this GPUElement
		jobject javaObject;	
      
      // bytes in the array or directBuf
      jint lengthInBytes;      

		// OpenCL memory buffer
		cl_mem mem;               

		// Memory mask used for createBuffer
		cl_uint memMask;  

		ProfileInfo read;
		ProfileInfo write;

		GPUElement();

      int referenceCount;

      void addReference();
      void deleteReference();
      bool hasReferences();

		virtual void process(JNIEnv* jenv, cl_context context, KernelContext* kernelContext, KernelArg* arg, int& argPos, int argIdx);
   private:
};
#endif