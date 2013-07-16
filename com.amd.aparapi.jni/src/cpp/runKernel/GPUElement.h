#ifndef GPUELEMENT_H
#define GPUELEMENT_H

#include "Common.h"
#include "ProfileInfo.h"

class KernelArg;
class JNIContext;

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

		virtual void process(JNIEnv* jenv, JNIContext* jniContext, KernelArg* arg, int& argPos, int argIdx);
   private:
};
#endif