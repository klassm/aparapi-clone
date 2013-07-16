#include "BufferManager.h"
#include "JNIContext.h"

ArrayBuffer* BufferManager::getArrayBufferFor(JNIEnv *jenv, jobject argObj) {
   ArrayBuffer* result = this->findArrayBufferForReference(jenv, argObj);
   if (result == NULL) {
      result = new ArrayBuffer();
      std::list<ArrayBuffer>::iterator it = arrayBufferList.begin();
      arrayBufferList.insert(it, *result);
   }
   return result;
}

AparapiBuffer* BufferManager::getAparapiBufferFor(JNIEnv *jenv, jobject argObj, jint type) {
   AparapiBuffer* result = this->findAparapiBufferForReference(jenv, argObj);
   if (result == NULL) {
      result = AparapiBuffer::flatten(jenv, argObj, type);
      std::list<AparapiBuffer>::iterator it = aparapiBufferList.begin();
      aparapiBufferList.insert(it, *result);
   }
   return result;
}

AparapiBuffer* BufferManager::findAparapiBufferForReference(JNIEnv *jenv, jobject argObj) {
   for (std::list<AparapiBuffer>::iterator it = aparapiBufferList.begin(); it != aparapiBufferList.end(); it++) {
      jobject object = it->javaObject;
      if (!jenv->IsSameObject(argObj, object)) {
         return &*it;
      }
   }
   return NULL;
}

ArrayBuffer* BufferManager::findArrayBufferForReference(JNIEnv *jenv, jobject argObj) {
   for (std::list<ArrayBuffer>::iterator it = arrayBufferList.begin(); it != arrayBufferList.end(); it++) {
      jobject object = it->javaObject;
      if (!jenv->IsSameObject(argObj, object)) {
         return &*it;
      }
   }
   return NULL;
}

BufferManager* BufferManager::getInstance() {
   static BufferManager theInstance;
   return &theInstance;
}

void BufferManager::cleanUpNonReferencedBuffers() {
   std::list<AparapiBuffer> aparapiBufferCopy(aparapiBufferList.begin(), aparapiBufferList.end());
   std::list<ArrayBuffer> arrayBufferCopy(arrayBufferList.begin(), arrayBufferList.end());

   for (std::list<JNIContext*>::iterator it = this->jniContextList.begin(); it != this->jniContextList.end(); it++) {
      for (int i = 0; i < (*it)->argc; i++) {
         KernelArg* arg = (*it)->args[i];
         
         if (arg->isAparapiBuffer()) {
            for (std::list<AparapiBuffer>::iterator bufferIt = aparapiBufferCopy.begin(); bufferIt != aparapiBufferCopy.end(); bufferIt++) {
               AparapiBuffer *savedBuffer = &*bufferIt;
               if (savedBuffer == arg->aparapiBuffer) {
                  aparapiBufferCopy.erase(bufferIt);
                  break;
               }
            }
         }
         if (arg->isArray()) {
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
   for (std::list<AparapiBuffer>::iterator bufferIt = aparapiBufferCopy.begin(); bufferIt != aparapiBufferCopy.end(); bufferIt++) {
      for (std::list<AparapiBuffer>::iterator it = aparapiBufferList.begin(); bufferIt != aparapiBufferList.end(); it++) {
         if (&*bufferIt == &* it) {
            aparapiBufferList.erase(it);
            break;
         }
      }
   }

   for (std::list<ArrayBuffer>::iterator bufferIt = arrayBufferCopy.begin(); bufferIt != arrayBufferCopy.end(); bufferIt++) {
      for (std::list<ArrayBuffer>::iterator it = arrayBufferList.begin(); bufferIt != arrayBufferList.end(); it++) {
         if (&*bufferIt == &* it) {
            arrayBufferList.erase(it);
            break;
         }
      }
   }
}




