#ifndef KERNEL_CONTEXT_H
#define KERNEL_CONTEXT_H

#include "Common.h"
#include "KernelArg.h"
#include "ProfileInfo.h"
#include "Config.h"
#include <list>

class KernelContext {
public:
   jobject kernelObject;
   jclass kernelClass;
   cl_program program;
   cl_kernel kernel;
   jint argc;
   KernelArg** args;
   cl_event* executeEvents;
   cl_event* readEvents;
   cl_ulong profileBaseTime;
   jint* readEventArgs;
   cl_event* writeEvents;
   jint* writeEventArgs;
   jboolean firstRun;
   jint passes;
   ProfileInfo *exec;
   FILE* profileFile;

   KernelContext(JNIEnv *jenv, jobject _kernelObject);
   
   static KernelContext* getKernelContext(jlong kernelContextHandle){
      return((KernelContext*)kernelContextHandle);
   }

   ~KernelContext(){
   }

   void dispose(JNIEnv *jenv, Config* config);
   void disposeMemory();

   /**
    * Release JNI critical pinned arrays before returning to java code
    */
   void unpinAll(JNIEnv* jenv);

   cl_int setLocalBufferArg(JNIEnv *jenv, int argIdx, int argPos, bool verbose, KernelArg *kernelArg);
};



#endif // JNI_CONTEXT_H
