#ifndef BUFFER_MANAGER_H
#define BUFFER_MANAGER_H

#include "ArrayBuffer.h"
#include "AparapiBuffer.h"
#include <list>

class BufferManager {
   public:
      static BufferManager *getInstance();
      std::list<JNIContext*> jniContextList;

      ArrayBuffer* getArrayBufferFor(JNIEnv *jenv, jobject argObj);
      AparapiBuffer* getAparapiBufferFor(JNIEnv *jenv, jobject argObj, jint type);
      void cleanUpNonReferencedBuffers();

   private:
      BufferManager() {}
      ~BufferManager() {}
      std::list<AparapiBuffer> aparapiBufferList;
      std::list<ArrayBuffer> arrayBufferList;

      AparapiBuffer* findAparapiBufferForReference(JNIEnv *jenv, jobject argObj);
      ArrayBuffer* findArrayBufferForReference(JNIEnv *jenv, jobject argObj);
};
#endif