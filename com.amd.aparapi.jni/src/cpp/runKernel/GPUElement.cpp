#include "GPUElement.h"

GPUElement::GPUElement(): 
	javaObject((jobject) 0),
   lengthInBytes(0),
   memMask((cl_uint)0),
   mem((cl_mem) 0),
   referenceCount(0)
   {
	}

void GPUElement::process(JNIEnv* jenv, cl_context context, KernelContext* kernelContext, KernelArg* arg, int& argPos, int argIdx) {}

void GPUElement::addReference() {
   referenceCount++;
}

void GPUElement::deleteReference() {
   referenceCount--;
}

bool GPUElement::hasReferences() {
   return referenceCount > 0;
}