#include "aparapi.h";

class GPUElement {
	public:
		// The last address where we saw this java object
		void *lastJavaObjectAddress;
		
		// The java object that is contained within this GPUElement
		jobject javaObject;		

		// OpenCL memory buffer
		cl_mem mem;               

		// Memory mask used for createBuffer
		cl_uint memMask;  

		ProfileInfo read;
		ProfileInfo write;

		virtual void process(JNIEnv* jenv, JNIContext* jniContext, KernelArg* arg, int& argPos, int argIdx);
};