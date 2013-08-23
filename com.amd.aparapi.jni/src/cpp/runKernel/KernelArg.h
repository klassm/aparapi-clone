
#ifndef KERNEL_ARG_H
#define KERNEL_ARG_H

#include "Common.h"
#include "JNIHelper.h"
#include "ArrayBuffer.h"
#include "AparapiBuffer.h"
#include "GPUElement.h"
#include "com_amd_aparapi_internal_jni_KernelRunnerJNI.h"
#include "Config.h"
#include "BufferManager.h"
#include <iostream>

#ifdef _WIN32
#define strdup _strdup
#endif

class KernelContext;

class KernelArg{
   private:
      static jclass argClazz;
      static jfieldID nameFieldID;
      static jfieldID typeFieldID; 
      static jfieldID sizeInBytesFieldID;
      static jfieldID numElementsFieldID;
      static jfieldID inlinePathVariableNamesFieldID;
      static jfieldID inlinePathVariableTypesFieldID;

      std::vector<char*> inlinePathVariableNamesParts;
      std::vector<char*> inlinePathVariableTypesParts;

      const char* getTypeName();

      //all of these use KernelContext so they can't be inlined

      //get the value of a primitive arguement
      void getPrimitiveValue(JNIEnv *jenv, jfloat *value, jobject baseRef, jclass baseClass);
      void getPrimitiveValue(JNIEnv *jenv, jint *value, jobject baseRef, jclass baseClass);
      void getPrimitiveValue(JNIEnv *jenv, jboolean *value, jobject baseRef, jclass baseClass);
      void getPrimitiveValue(JNIEnv *jenv, jbyte *value, jobject baseRef, jclass baseClass);
      void getPrimitiveValue(JNIEnv *jenv, jlong *value, jobject baseRef, jclass baseClass);
      void getPrimitiveValue(JNIEnv *jenv, jdouble *value, jobject baseRef, jclass baseClass);

      //get the value of a static primitive arguement
      void getStaticPrimitiveValue(JNIEnv *jenv, jfloat *value);
      void getStaticPrimitiveValue(JNIEnv *jenv, jint *value);
      void getStaticPrimitiveValue(JNIEnv *jenv, jboolean *value);
      void getStaticPrimitiveValue(JNIEnv *jenv, jbyte *value);
      void getStaticPrimitiveValue(JNIEnv *jenv, jlong *value);
      void getStaticPrimitiveValue(JNIEnv *jenv, jdouble *value);

      template<typename T> 
      void getPrimitive(JNIEnv *jenv, int argIdx, int argPos, bool verbose, T* value, jobject baseRef, jclass baseClass) {
         if(isStatic()) {
            getStaticPrimitiveValue(jenv, value);
         }
         else {
            getPrimitiveValue(jenv, value, baseRef, baseClass);
         }
         if (verbose) {
             std::cerr << "clSetKernelArg " << getTypeName() << " '" << name
                       << " ' index=" << argIdx << " pos=" << argPos 
                       << " value=" << *value << std::endl;
         }
      }

      void splitStringBy(char *toSplit, char *delimiter, std::vector<char*> *resultVector);


   public:
      static jfieldID javaArrayFieldID; 
   public:
      KernelContext *kernelContext;  
      jobject argObj;    // the Java KernelRunner.KernelArg object that we are mirroring.
      jobject javaArg;   // global reference to the corresponding java KernelArg object we grabbed our own global reference so that the object won't be collected until we dispose!
      char *name;        // used for debugging printfs
      jint type;         // a bit mask determining the type of this arg

      GPUElement* buffer;
      //ArrayBuffer *arrayBuffer;
      //AparapiBuffer *aparapiBuffer;

      // Uses KernelContext so cant inline here see below
      KernelArg(JNIEnv *jenv, jobject argObj, KernelContext *kernelContext);

      ~KernelArg(){
      }

      /**
       * Update any non primitive references. For arrays, this means either allocating a new buffer with the array content
       * if this has not been done before or allocating a new one as the array reference has changed.
       * @param jenv JNI reference
       */
      void updateReference(JNIEnv *jenv, BufferManager* bufferManager);

      void unpinAbort(JNIEnv *jenv) {
         if (this->isArray()) {
            ((ArrayBuffer*)this->buffer)->unpinAbort(jenv);
         }
      }

      void unpinCommit(JNIEnv *jenv){
         if (this->isArray()) {
            ((ArrayBuffer*)this->buffer)->unpinCommit(jenv);
         }
      }

      void unpin(JNIEnv *jenv){
         //if  (value.ref.isPinned == JNI_FALSE){		 
         //     fprintf(stdout, "why are we unpinning buffer %s! isPinned = JNI_TRUE\n", name);
         //}
         if (isMutableByKernel()){
            // we only need to commit if the buffer has been written to
            // we use mode=0 in that case (rather than JNI_COMMIT) because that frees any copy buffer if it exists
            // in most cases this array will have been pinned so this will not be an issue
            unpinCommit(jenv);
         }else {
            // fast path for a read_only buffer
            unpinAbort(jenv);
         }
      }
      void pin(JNIEnv *jenv){
         if (this->isArray()) {
            ((ArrayBuffer*)this->buffer)->pin(jenv);
         }
      }

      int isArray(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_ARRAY);
      }
      int isReadByKernel(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_READ);
      }
      int isMutableByKernel(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_WRITE);
      }
      int isExplicit(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_EXPLICIT);
      }
      int usesArrayLength(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_ARRAYLENGTH);
      }
      int isExplicitWrite(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_EXPLICIT_WRITE);
      }
      int isImplicit(){
         return(!isExplicit());
      }
      int isPrimitive(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_PRIMITIVE);
      }
      int isGlobal(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_GLOBAL);
      }
      int isFloat(){
         return(type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_FLOAT);
      }
      int isLong(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_LONG);
      }
      int isInt(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_INT);
      }
      int isDouble(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_DOUBLE);
      }
      int isBoolean(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_BOOLEAN);
      }
      int isByte(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_BYTE);
      }
      int isShort(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_SHORT);
      }
      int isLocal(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_LOCAL);
      }
      int isStatic(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_STATIC);
      }
      int isConstant(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_CONSTANT);
      }
      int isAparapiBuffer(){
         return (type&com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_APARAPI_BUFFER);
      }
      int isBackedByArray(){
         return ( (isArray() && (isGlobal() || isConstant())));
      }
      int needToEnqueueRead(){
         return(((isArray() && isGlobal()) || ((isAparapiBuffer()&&isGlobal()))) && (isImplicit()&&isMutableByKernel()));
      }
      int needToEnqueueWrite(){
         return ((isImplicit()&&isReadByKernel())||(isExplicit()&&isExplicitWrite()));
      }
      void syncType(JNIEnv* jenv){
         type = jenv->GetIntField(javaArg, typeFieldID);
      }
      void syncSizeInBytes(JNIEnv* jenv){
         if (this->isArray()) {
            ((ArrayBuffer*)this->buffer)->lengthInBytes = jenv->GetIntField(javaArg, sizeInBytesFieldID);
         }
      }
      void syncJavaArrayLength(JNIEnv* jenv){
         if (this->isArray()) {
            ((ArrayBuffer*)this->buffer)->length = jenv->GetIntField(javaArg, numElementsFieldID);
         }
      }
      void clearExplicitBufferBit(JNIEnv* jenv){
         type &= ~com_amd_aparapi_internal_jni_KernelRunnerJNI_ARG_EXPLICIT_WRITE;
         jenv->SetIntField(javaArg, typeFieldID,type );
      }

      // Uses KernelContext so can't inline here we below.  
      void syncValue(JNIEnv *jenv);

      // Uses KernelContext so can't inline here we below.  
      cl_int setPrimitiveArg(JNIEnv *jenv, int argIdx, int argPos, bool verbose);
};


#endif // KERNEL_ARG_H
