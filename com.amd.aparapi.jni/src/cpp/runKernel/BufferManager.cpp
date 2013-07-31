#include "BufferManager.h"
#include "JNIContext.h"
#include "List.h"
#include "common.h"

BufferManager::BufferManager() {
   this->createdNewAparapiBuffer = false;
   this->replacedArrayBuffer = false;
}

ArrayBuffer* BufferManager::getArrayBufferFor(JNIEnv *jenv, jobject reference) {
   ArrayBuffer* result = this->findArrayBufferForReference(jenv, reference);
   if (result == NULL) {
      result = new ArrayBuffer(jenv, reference);
      std::list<ArrayBuffer*>::iterator it = arrayBufferList.begin();
      arrayBufferList.insert(it, result);
   }
   return result;
}

AparapiBuffer* BufferManager::getAparapiBufferFor(JNIEnv *jenv, jobject reference, jint type) {
   AparapiBuffer* result = this->findAparapiBufferForReference(jenv, reference);
   if (result == NULL) {
      result = AparapiBuffer::flatten(jenv, reference, type);
      std::list<AparapiBuffer*>::iterator it = aparapiBufferList.begin();
      aparapiBufferList.insert(it, result);

      createdNewAparapiBuffer = true;
   }
   return result;
}

AparapiBuffer* BufferManager::findAparapiBufferForReference(JNIEnv *jenv, jobject reference) {
   for (std::list<AparapiBuffer*>::iterator it = aparapiBufferList.begin(); it != aparapiBufferList.end(); it++) {
      jobject object = (*it)->javaObject;
      if (jenv->IsSameObject(reference, object)) {
         return *it;
      }
   }
   return NULL;
}

ArrayBuffer* BufferManager::findArrayBufferForReference(JNIEnv *jenv, jobject reference) {
   for (std::list<ArrayBuffer*>::iterator it = arrayBufferList.begin(); it != arrayBufferList.end(); it++) {
      jobject object = (*it)->javaObject;
      if (jenv->IsSameObject(reference, object)) {
         return *it;
      }
   }
   return NULL;
}

BufferManager* BufferManager::getInstance() {
   static BufferManager theInstance;
   return &theInstance;
}

void BufferManager::cleanUpNonReferencedBuffers(JNIEnv *jenv) {
   this->cleanUpNonReferencedBuffers(jenv, false);
}

void BufferManager::cleanUpNonReferencedBuffers(JNIEnv *jenv, bool enforce) {
   if (! enforce && ! createdNewAparapiBuffer && ! replacedArrayBuffer) return;
   
   std::list<AparapiBuffer*> aparapiBufferCopy(aparapiBufferList.begin(), aparapiBufferList.end());
   std::list<ArrayBuffer*> arrayBufferCopy(arrayBufferList.begin(), arrayBufferList.end());
  
   for (std::list<JNIContext*>::iterator it = this->jniContextList.begin(); it != this->jniContextList.end(); it++) {
      JNIContext *context = *it;
      if (context == NULL || context->argc == 0 || context->args == NULL) continue;
      for (int i = 0; i < context->argc; i++) {
         
         KernelArg* arg = context->args[i];
         if (arg == NULL) continue;
         
         if ((enforce ||createdNewAparapiBuffer) && arg->isAparapiBuffer()) {
            for (std::list<AparapiBuffer*>::iterator bufferIt = aparapiBufferCopy.begin(); bufferIt != aparapiBufferCopy.end(); bufferIt++) {
               AparapiBuffer *savedBuffer = *bufferIt;
               if (savedBuffer == arg->aparapiBuffer) {
                  aparapiBufferCopy.erase(bufferIt);
                  break;
               }
            }
         }
         if ((enforce || replacedArrayBuffer) && arg->isArray()) {
            for (std::list<ArrayBuffer*>::iterator bufferIt = arrayBufferCopy.begin(); bufferIt != arrayBufferCopy.end(); bufferIt++) {
               ArrayBuffer *savedBuffer = *bufferIt;
               if (savedBuffer == arg->arrayBuffer) {
                  arrayBufferCopy.erase(bufferIt);
                  break;
               }
            }
         }
      }
   }

   // by now both copy arrays will contain only unreferenced addresses
   
   if (enforce || createdNewAparapiBuffer) {
      for (std::list<AparapiBuffer*>::iterator bufferIt = aparapiBufferCopy.begin(); bufferIt != aparapiBufferCopy.end(); bufferIt++) {
         for (std::list<AparapiBuffer*>::iterator it = aparapiBufferList.begin(); it != aparapiBufferList.end(); it++) {
            if (*bufferIt == *it) {
               AparapiBuffer *buffer = (*it);
               aparapiBufferList.erase(it);
			      cleanUp(buffer, jenv);
               break;
            }
         }
      }
   }

   if (enforce || replacedArrayBuffer) {
      for (std::list<ArrayBuffer*>::iterator bufferIt = arrayBufferCopy.begin(); bufferIt != arrayBufferCopy.end(); bufferIt++) {
         for (std::list<ArrayBuffer*>::iterator it = arrayBufferList.begin(); it != arrayBufferList.end(); it++) {
            if (*bufferIt == *it) {
               ArrayBuffer *buffer = (*it);
               arrayBufferList.erase(it);
			      cleanUp(buffer, jenv);
               break;
            }
         }
      }
   }
   
   createdNewAparapiBuffer = false;
   replacedArrayBuffer = false;
}

void BufferManager::cleanUp(GPUElement* gpuElement, JNIEnv *jenv) {
	cl_int status = CL_SUCCESS;

	if (gpuElement->javaObject != NULL) {
      
      jenv->DeleteGlobalRef(gpuElement->javaObject);
		if (config->isVerbose()){
			fprintf(stderr, "DeleteGlobalRef for %p\n", gpuElement->javaObject);         
		}
	}

	if (gpuElement->mem != 0) {
		if (config->isTrackingOpenCLResources()){
			memList.remove(gpuElement->mem,__LINE__, __FILE__);
		}
		status = clReleaseMemObject((cl_mem)gpuElement->mem);

		if(status != CL_SUCCESS) throw CLException(status, "clReleaseMemObject()");
		gpuElement->mem = (cl_mem) 0;
	}
	delete(gpuElement);
}




