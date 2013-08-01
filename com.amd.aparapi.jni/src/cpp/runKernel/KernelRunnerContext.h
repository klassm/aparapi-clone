#pragma once

#include "CL\cl.h"
#include "jni.h"
#include "Config.h"
#include "com_amd_aparapi_internal_jni_KernelRunnerJNI.h"
#include "BufferManager.h"
#include "KernelContext.h"

class KernelRunnerContext
{
public:
  
   cl_device_id deviceId;
   cl_device_type deviceType;
   cl_context context;
   cl_command_queue commandQueue;

   BufferManager* bufferManager;
   
   static KernelRunnerContext* contextFor(JNIEnv* jenv, jobject _openCLDeviceObject, int flags, Config* config);
   void dispose(JNIEnv* jenv);

   static KernelRunnerContext* getKernelRunnerContext(jlong kernelRunnerContextHandle){
      return((KernelRunnerContext*)kernelRunnerContextHandle);
   }
 
   /**
      * Holds a list of jni contexts, representing kernel entry points.
      */
   std::list<KernelContext*> kernelContextList;

   jboolean isUsingGPU(){
      //I'm pretty sure that this is equivalend to:
      //return flags & com_amd_aparapi_internal_jni_KernelRunnerJNI_JNI_FLAG_USE_GPU;
      return((flags&com_amd_aparapi_internal_jni_KernelRunnerJNI_JNI_FLAG_USE_GPU)==com_amd_aparapi_internal_jni_KernelRunnerJNI_JNI_FLAG_USE_GPU?JNI_TRUE:JNI_FALSE);
   }

   void registerKernelContext(KernelContext* kernelContext);

   ~KernelRunnerContext(void);


private:
   jint flags;

   KernelRunnerContext(cl_device_id _deviceId, cl_device_type _deviceType, 
      cl_context _context, cl_command_queue _commandQueue, int _flags);
};

