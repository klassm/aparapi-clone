#ifndef BUFFER_MANAGER_H
#define BUFFER_MANAGER_H

#include "ArrayBuffer.h"
#include "AparapiBuffer.h"
#include <list>

/**
 * Class which is responsible for handling all the buffers. It keeps tack of each generated instance
 * and returns instances matching inner java objects instead of creating new ones. The class itself 
 * is a singleton. An instance can be obtained by calling BufferManager::getInstance().
 */
class BufferManager {
   public:
      BufferManager();

      bool replacedArrayBuffer;
      bool createdNewAparapiBuffer;

      /**
       * Get an ArrayBuffer for a matching argObj. If no buffer containing the argObj is found, 
       * a new one is created!
       * @param jenv JNI environment
       * @param argObj used for looking up any buffers containing the reference
       * @return buffer
       */
      ArrayBuffer* getArrayBufferFor(JNIEnv *jenv, jobject argObj);

      /**
       * Get an AparapiBuffer for a matching argObj. If no buffer containing the argObj is found, 
       * a new one is created!
       * @param jenv JNI environment
       * @param argObj used for looking up any buffers containing the reference
       * @return buffer
       */
      AparapiBuffer* getAparapiBufferFor(JNIEnv *jenv, jobject argObj, jint type);
      void cleanUpNonReferencedBuffers(JNIEnv *jenv, std::list<KernelContext*> kernelContextList, bool enforce);
      void cleanUpNonReferencedBuffers(JNIEnv *jenv, std::list<KernelContext*> kernelContextList);

   private:    
      std::list<AparapiBuffer*> aparapiBufferList;
      std::list<ArrayBuffer*> arrayBufferList;
	   void cleanUp(GPUElement* gpuElement, JNIEnv *jenv);

      AparapiBuffer* findAparapiBufferForReference(JNIEnv *jenv, jobject argObj);
      ArrayBuffer* findArrayBufferForReference(JNIEnv *jenv, jobject argObj);
};
#endif