#include "BufferManager.h"
#include "JNIContext.h"
#include "List.h"

ArrayBuffer* BufferManager::getArrayBufferFor(JNIEnv *jenv, jobject argObj) {
   ArrayBuffer* result = this->findArrayBufferForReference(jenv, argObj);
   if (result == NULL) {
      result = new ArrayBuffer(jenv, argObj);
      std::list<ArrayBuffer>::iterator it = arrayBufferList.begin();
      arrayBufferList.insert(it, *result);

      createdNewArrayBuffer = true;
   }
   return result;
}

AparapiBuffer* BufferManager::getAparapiBufferFor(JNIEnv *jenv, jobject argObj, jint type) {
   AparapiBuffer* result = this->findAparapiBufferForReference(jenv, argObj);
   if (result == NULL) {
      result = AparapiBuffer::flatten(jenv, argObj, type);
      std::list<AparapiBuffer>::iterator it = aparapiBufferList.begin();
      aparapiBufferList.insert(it, *result);

      createdNewArrayBuffer = true;
   }
   return result;
}

AparapiBuffer* BufferManager::findAparapiBufferForReference(JNIEnv *jenv, jobject argObj) {
   for (std::list<AparapiBuffer>::iterator it = aparapiBufferList.begin(); it != aparapiBufferList.end(); it++) {
      jobject object = it->javaObject;
      if (jenv->IsSameObject(argObj, object)) {
         return &*it;
      }
   }
   return NULL;
}

ArrayBuffer* BufferManager::findArrayBufferForReference(JNIEnv *jenv, jobject argObj) {
   for (std::list<ArrayBuffer>::iterator it = arrayBufferList.begin(); it != arrayBufferList.end(); it++) {
      jobject object = it->javaObject;
      if (jenv->IsSameObject(argObj, object)) {
         return &*it;
      }
   }
   return NULL;
}

BufferManager* BufferManager::getInstance() {
   static BufferManager theInstance;

   theInstance.createdNewAparapiBuffer = false;
   theInstance.createdNewArrayBuffer = false;

   return &theInstance;
}

void BufferManager::cleanUpNonReferencedBuffers(JNIEnv *jenv) {
   if (! createdNewAparapiBuffer && ! createdNewArrayBuffer) return;

   std::list<AparapiBuffer> aparapiBufferCopy(aparapiBufferList.begin(), aparapiBufferList.end());
   std::list<ArrayBuffer> arrayBufferCopy(arrayBufferList.begin(), arrayBufferList.end());

   for (std::list<JNIContext*>::iterator it = this->jniContextList.begin(); it != this->jniContextList.end(); it++) {
      for (int i = 0; i < (*it)->argc; i++) {
         KernelArg* arg = (*it)->args[i];
         
         if (createdNewAparapiBuffer && arg->isAparapiBuffer()) {
            for (std::list<AparapiBuffer>::iterator bufferIt = aparapiBufferCopy.begin(); bufferIt != aparapiBufferCopy.end(); bufferIt++) {
               AparapiBuffer *savedBuffer = &*bufferIt;
               if (savedBuffer == arg->aparapiBuffer) {
                  aparapiBufferCopy.erase(bufferIt);
                  break;
               }
            }
         }
         if (createdNewArrayBuffer && arg->isArray()) {
            for (std::list<ArrayBuffer>::iterator bufferIt = arrayBufferCopy.begin(); bufferIt != arrayBufferCopy.end(); bufferIt++) {
               ArrayBuffer *savedBuffer = &*bufferIt;
               if (savedBuffer == arg->arrayBuffer) {
                  arrayBufferCopy.erase(bufferIt);
                  break;
               }
            }
         }
      }
   }

   // by now both copy arrays will contain only unreferenced addresses

   if (createdNewAparapiBuffer) {
      for (std::list<AparapiBuffer>::iterator bufferIt = aparapiBufferCopy.begin(); bufferIt != aparapiBufferCopy.end(); bufferIt++) {
         for (std::list<AparapiBuffer>::iterator it = aparapiBufferList.begin(); it != aparapiBufferList.end(); it++) {
            if (&*bufferIt == &* it) {
               aparapiBufferList.erase(it);
			   GPUElement* element = (GPUElement*) &*it;
			   cleanUp(element, jenv);
               break;
            }
         }
      }
   }

   if (createdNewArrayBuffer) {
      for (std::list<ArrayBuffer>::iterator bufferIt = arrayBufferCopy.begin(); bufferIt != arrayBufferCopy.end(); bufferIt++) {
         for (std::list<ArrayBuffer>::iterator it = arrayBufferList.begin(); it != arrayBufferList.end(); it++) {
            if (&*bufferIt == &* it) {
               arrayBufferList.erase(it);
			   GPUElement* element = (GPUElement*) &*it;
			   cleanUp(element, jenv);
               break;
            }
         }
      }
   }

   createdNewAparapiBuffer = false;
   createdNewArrayBuffer = false;
}

void BufferManager::cleanUp(GPUElement* gpuElement, JNIEnv *jenv) {
	cl_int status = CL_SUCCESS;

	if (gpuElement->javaObject != NULL) {
		jenv->DeleteWeakGlobalRef((jweak) gpuElement->javaObject);
		if (config->isVerbose()){
			fprintf(stderr, "DeleteWeakGlobalRef for %p\n", gpuElement->javaObject);         
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




