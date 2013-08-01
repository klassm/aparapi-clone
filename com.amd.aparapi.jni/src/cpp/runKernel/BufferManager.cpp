#include "BufferManager.h"
#include "KernelContext.h"
#include "List.h"
#include "common.h"

BufferManager::BufferManager() {
   this->replacedAparapiBuffer = false;
   this->replacedArrayBuffer = false;
}

ArrayBuffer* BufferManager::getArrayBufferFor(JNIEnv *jenv, jobject reference) {
   ArrayBuffer* result = this->findArrayBufferForReference(jenv, reference);
   if (result == NULL) {
      result = new ArrayBuffer(jenv, reference);
      std::vector<ArrayBuffer*>::iterator it = arrayBufferList.begin();
      arrayBufferList.insert(it, result);
   }
   return result;
}

AparapiBuffer* BufferManager::getAparapiBufferFor(JNIEnv *jenv, jobject reference, jint type) {
   AparapiBuffer* result = this->findAparapiBufferForReference(jenv, reference);
   if (result == NULL) {
      result = AparapiBuffer::flatten(jenv, reference, type);
      std::vector<AparapiBuffer*>::iterator it = aparapiBufferList.begin();
      aparapiBufferList.insert(it, result);
   }
   return result;
}

AparapiBuffer* BufferManager::findAparapiBufferForReference(JNIEnv *jenv, jobject reference) {
   for (std::vector<AparapiBuffer*>::iterator it = aparapiBufferList.begin(); it != aparapiBufferList.end(); it++) {
      jobject object = (*it)->javaObject;
      if (jenv->IsSameObject(reference, object)) {
         return *it;
      }
   }
   return NULL;
}

ArrayBuffer* BufferManager::findArrayBufferForReference(JNIEnv *jenv, jobject reference) {
   for (std::vector<ArrayBuffer*>::iterator it = arrayBufferList.begin(); it != arrayBufferList.end(); it++) {
      jobject object = (*it)->javaObject;
      if (jenv->IsSameObject(reference, object)) {
         return *it;
      }
   }
   return NULL;
}

void BufferManager::cleanUpNonReferencedBuffers(JNIEnv *jenv) {
   this->cleanUpNonReferencedBuffers(jenv, false);
}

void BufferManager::cleanUpNonReferencedBuffers(JNIEnv *jenv, bool enforce) {
   if (! enforce && ! replacedAparapiBuffer && ! replacedArrayBuffer) return;
   
   if (enforce || replacedAparapiBuffer) {
      for (std::vector<AparapiBuffer*>::iterator bufferIt = aparapiBufferList.begin(); bufferIt != aparapiBufferList.end(); bufferIt++) {
         AparapiBuffer* buffer = *bufferIt;
         if (! (buffer->hasReferences())) {
            cleanUp(*bufferIt, jenv);
            bufferIt = aparapiBufferList.erase(bufferIt);

            // erase returns the a new iterator, pointing to the next element.
            // for the last element, we point to .end(), on which incrementing
            // is invalid!
            if (bufferIt == aparapiBufferList.end()) break;
         }
      }
   }

   if (enforce || replacedArrayBuffer) {
      for (std::vector<ArrayBuffer*>::iterator bufferIt = arrayBufferList.begin(); bufferIt != arrayBufferList.end(); bufferIt++) {
         ArrayBuffer* buffer = *bufferIt;
         if (! (buffer->hasReferences())) {
            cleanUp(buffer, jenv);
            bufferIt = arrayBufferList.erase(bufferIt);

            // erase returns the a new iterator, pointing to the next element.
            // for the last element, we point to .end(), on which incrementing
            // is invalid!
            if (bufferIt == arrayBufferList.end()) break;
         }
      }
   }
   
   replacedAparapiBuffer = false;
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




