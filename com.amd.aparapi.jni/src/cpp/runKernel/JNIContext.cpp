#include "JNIContext.h"
#include "OpenCLJNI.h"
#include "List.h"
#include "BufferManager.h"

JNIContext::JNIContext(JNIEnv *jenv, jobject _kernelObject, jobject _openCLDeviceObject, jint _flags): 
      kernelObject(jenv->NewGlobalRef(_kernelObject)),
      kernelClass((jclass)jenv->NewGlobalRef(jenv->GetObjectClass(_kernelObject))), 
      //openCLDeviceObject(jenv->NewGlobalRef(_openCLDeviceObject)),
      flags(_flags),
      profileBaseTime(0),
      passes(0),
      exec(NULL),
      //deviceType(((flags&com_amd_aparapi_internal_jni_KernelRunnerJNI_JNI_FLAG_USE_GPU)==com_amd_aparapi_internal_jni_KernelRunnerJNI_JNI_FLAG_USE_GPU)?CL_DEVICE_TYPE_GPU:CL_DEVICE_TYPE_CPU),
      profileFile(NULL), 
      valid(JNI_TRUE){

   BufferManager* bufferManager = BufferManager::getInstance();
   
   std::list<JNIContext*>::iterator it = bufferManager->jniContextList.begin();
   bufferManager->jniContextList.insert(it, this);
}

void JNIContext::dispose(JNIEnv *jenv, Config* config) {
   //fprintf(stdout, "dispose()\n");
   cl_int status = CL_SUCCESS;
   jenv->DeleteGlobalRef(kernelObject);
   jenv->DeleteGlobalRef(kernelClass);

   if (program != 0){
      status = clReleaseProgram((cl_program)program);
      //fprintf(stdout, "dispose program %0lx\n", program);
      CLException::checkCLError(status, "clReleaseProgram()");
      program = (cl_program)0;
   }
   if (kernel != 0){
      status = clReleaseKernel((cl_kernel)kernel);
      //fprintf(stdout, "dispose kernel %0lx\n", kernel);
      CLException::checkCLError(status, "clReleaseKernel()");
      kernel = (cl_kernel)0;
   }
   if (argc > 0){
      for (int i=0; i< argc; i++){
         KernelArg *arg = args[i];
         if (arg->javaArg != NULL ) {
            jenv->DeleteGlobalRef((jobject) arg->javaArg);
            arg->javaArg = NULL;
         }
         if (!arg->isPrimitive()){
            // those will be cleaned up by BufferManager!
            arg->arrayBuffer = NULL;
            arg->aparapiBuffer = NULL;
         }
         if (arg->name != NULL){
            free(arg->name); arg->name = NULL;
         }

         arg->arrayBuffer = NULL;
         arg->aparapiBuffer = NULL;
         arg->argObj = NULL;
         
         delete arg; arg=args[i]=NULL;
      }
      delete[] args; args=NULL;

      // do we need to call clReleaseEvent on any of these that are still retained....
      delete[] readEvents; readEvents = NULL;
      delete[] writeEvents; writeEvents = NULL;
      delete[] executeEvents; executeEvents = NULL;

      if (config->isProfilingEnabled()) {
         if (config->isProfilingCSVEnabled()) {
            if (profileFile != NULL && profileFile != stderr) {
               fclose(profileFile);
            }
         }
         delete[] readEventArgs; readEventArgs=0;
         delete[] writeEventArgs; writeEventArgs=0;
      } 
   }
}

void JNIContext::unpinAll(JNIEnv* jenv) {
   for (int i=0; i< argc; i++){
      KernelArg *arg = args[i];
      if (arg->isBackedByArray()) {
         arg->unpin(jenv);
      }
   }
}

cl_int JNIContext::setLocalBufferArg(JNIEnv *jenv, int argIdx, int argPos, bool verbose, KernelArg *kernelArg) {
   if (verbose){
       fprintf(stderr, "ISLOCAL, clSetKernelArg(jniContext->kernel, %d, %d, NULL);\n", argIdx, (int) kernelArg->arrayBuffer->lengthInBytes);
   }
   return(clSetKernelArg(this->kernel, argPos, (int)kernelArg->arrayBuffer->lengthInBytes, NULL));
}

cl_int JNIContext::setLocalAparapiBufferArg(JNIEnv *jenv, int argIdx, int argPos, bool verbose, KernelArg *kernelArg) {
   if (verbose){
       fprintf(stderr, "ISLOCAL, clSetKernelArg(jniContext->kernel, %d, %d, NULL);\n", argIdx, (int) kernelArg->aparapiBuffer->lengthInBytes);
   }
   return(clSetKernelArg(this->kernel, argPos, (int)kernelArg->aparapiBuffer->lengthInBytes, NULL));
}