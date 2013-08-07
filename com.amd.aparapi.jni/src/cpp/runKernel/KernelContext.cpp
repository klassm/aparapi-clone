#include "KernelContext.h"
#include "OpenCLJNI.h"
#include "List.h"
#include "BufferManager.h"

KernelContext::KernelContext(JNIEnv *jenv, jobject _kernelObject): 
      kernelObject(jenv->NewGlobalRef(_kernelObject)),
      kernelClass((jclass)jenv->NewGlobalRef(jenv->GetObjectClass(_kernelObject))), 
      profileBaseTime(0),
      passes(0),
      exec(NULL),
      profileFile(NULL) {
}

void KernelContext::dispose(JNIEnv *jenv, Config* config) {
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
         if (!arg->isPrimitive() && arg->buffer != NULL){
            // those will be cleaned up by BufferManager!
            arg->buffer->deleteReference();
            arg->buffer = NULL;
         }
         if (arg->name != NULL){
            free(arg->name); arg->name = NULL;
         }

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

void KernelContext::replaceKernelObject(JNIEnv *jenv, jobject _kernelObject) {
   jenv->DeleteGlobalRef(this->kernelObject);
   this->kernelObject = jenv->NewGlobalRef(_kernelObject);
}

void KernelContext::unpinAll(JNIEnv* jenv) {
   for (int i=0; i< argc; i++){
      KernelArg *arg = args[i];
      if (arg->isBackedByArray()) {
         arg->unpin(jenv);
      }
   }
}

cl_int KernelContext::setLocalBufferArg(JNIEnv *jenv, int argIdx, int argPos, bool verbose, KernelArg *kernelArg) {
   if (verbose){
       fprintf(stderr, "ISLOCAL, clSetKernelArg(kernelContext->kernel, %d, %d, NULL);\n", argIdx, (int) kernelArg->buffer->lengthInBytes);
   }
   return(clSetKernelArg(this->kernel, argPos, (int)kernelArg->buffer->lengthInBytes, NULL));
}

void KernelContext::disposeMemory() {
   for (int i = 0; i < argc; i++) {
      KernelArg* arg = args[i];
      if (arg->buffer != NULL) {
         arg->buffer->deleteReference();
         arg->buffer = NULL;
      }
   }
   firstRun = true;
}