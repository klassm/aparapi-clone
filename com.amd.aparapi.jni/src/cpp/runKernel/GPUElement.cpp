#include "GPUElement.h"

GPUElement::GPUElement(): 
	javaObject((jobject) 0),
   lengthInBytes(0),
   memMask((cl_uint)0),
   mem((cl_mem) 0)
   {
	}