#include "KernelRunnerContext.h"
#include "OpenCLJNI.h"
#include "List.h"


KernelRunnerContext::KernelRunnerContext(cl_device_id _deviceId, cl_device_type _deviceType, 
      cl_context _context, cl_command_queue _commandQueue, int _flags):
      deviceId(_deviceId),
      deviceType(_deviceType), 
      context(_context),
      commandQueue(_commandQueue),
      flags(_flags)
{
   bufferManager = new BufferManager();
   kernelContextList.reserve(20);
}


KernelRunnerContext::~KernelRunnerContext(void) {
   delete bufferManager;
}

void KernelRunnerContext::dispose(JNIEnv* jenv) {
   for (std::vector<KernelContext*>::iterator it = kernelContextList.begin(); it != kernelContextList.end(); it++) {
      (*it)->dispose(jenv, config);
      delete (*it);
   }
   kernelContextList.clear();

   cl_int status = CL_SUCCESS;

   if (context != 0){
      status = clReleaseContext(context);
      //fprintf(stdout, "dispose context %0lx\n", context);
      CLException::checkCLError(status, "clReleaseContext()");
      context = (cl_context)0;
   }

   if (commandQueue != NULL){
      if (config->isTrackingOpenCLResources()){
         commandQueueList.remove((cl_command_queue)commandQueue, __LINE__, __FILE__);
      }
      status = clReleaseCommandQueue((cl_command_queue)commandQueue);
      //fprintf(stdout, "dispose commandQueue %0lx\n", commandQueue);
      CLException::checkCLError(status, "clReleaseCommandQueue()");
      commandQueue = (cl_command_queue) NULL;
   }

   bufferManager->cleanUpNonReferencedBuffers(jenv, true);

   if (config->isTrackingOpenCLResources()){
      fprintf(stderr, "after dispose{ \n");
      commandQueueList.report(stderr);
      memList.report(stderr); 
      readEventList.report(stderr); 
      executeEventList.report(stderr); 
      writeEventList.report(stderr); 
      fprintf(stderr, "}\n");
   }
}

/**
 * Initialize a new KernelRunnerContext for a given Device object stemming from a Java JNI call.
 * @param jenv JNI environment
 * @param _openCLDeviceObject device object stemming from JNI
 * @param config configuration instance
 * @return returns a new instance of KernelRunnerContext
 */
KernelRunnerContext* KernelRunnerContext::contextFor(JNIEnv* jenv, jobject _openCLDeviceObject, int flags, Config* config) {
   cl_int status = CL_SUCCESS;

   jobject openCLDeviceObject = jenv->NewGlobalRef(_openCLDeviceObject);

   // init opencl
   cl_device_type deviceType;

   jobject platformInstance = OpenCLDevice::getPlatformInstance(jenv, openCLDeviceObject);
   cl_platform_id platformId = OpenCLPlatform::getPlatformId(jenv, platformInstance);
   cl_device_id deviceId = OpenCLDevice::getDeviceId(jenv, openCLDeviceObject);
   clGetDeviceInfo(deviceId, CL_DEVICE_TYPE,  sizeof(deviceType), &deviceType, NULL);

   cl_context_properties cps[3] = { CL_CONTEXT_PLATFORM, (cl_context_properties)platformId, 0 };
   cl_context_properties* cprops = (NULL == platformId) ? NULL : cps;
   cl_context context = clCreateContextFromType( cprops, deviceType, NULL, NULL, &status); 

   jenv->DeleteWeakGlobalRef((jweak) openCLDeviceObject);

   CLException::checkCLError(status, "clCreateContextFromType()");
   if (status != CL_SUCCESS){
      throw CLException(status, "clCreateContextFromType()");
   }

   // init opencl command queue

   cl_command_queue_properties queue_props = 0;
   if (config->isProfilingEnabled()) {
      queue_props |= CL_QUEUE_PROFILING_ENABLE;
   }

   cl_command_queue commandQueue = clCreateCommandQueue(context, deviceId,
      queue_props,
      &status);
   if(status != CL_SUCCESS) throw CLException(status,"clCreateCommandQueue()");

   return new KernelRunnerContext(deviceId, deviceType, context, commandQueue, flags);
}

void KernelRunnerContext::registerKernelContext(KernelContext* kernelContext) {
   kernelContextList.push_back(kernelContext);
}