#include "KernelArg.h"
#include "KernelContext.h"
#include "BufferManager.h"
#include <string>
#include <iostream>

using std::string;
using std::cerr;
using std::endl;

jclass KernelArg::argClazz=(jclass)0;
jfieldID KernelArg::nameFieldID=0;
jfieldID KernelArg::typeFieldID=0; 
jfieldID KernelArg::javaArrayFieldID=0; 
jfieldID KernelArg::sizeInBytesFieldID=0;
jfieldID KernelArg::numElementsFieldID=0; 

KernelArg::KernelArg(JNIEnv *jenv, jobject argObj, KernelContext *_kernelContext):
   argObj(argObj),
   arrayBuffer(NULL),
   aparapiBuffer(NULL),
   kernelContext(_kernelContext)
   {
      javaArg = jenv->NewGlobalRef(argObj);   // save a global ref to the java Arg Object
      if (argClazz == 0){
         jclass c = jenv->GetObjectClass(argObj); 
         nameFieldID = JNIHelper::GetFieldID(jenv, c, "name", "Ljava/lang/String;");
         typeFieldID = JNIHelper::GetFieldID(jenv, c, "type", "I");
         javaArrayFieldID = JNIHelper::GetFieldID(jenv, c, "javaArray", "Ljava/lang/Object;");
         sizeInBytesFieldID = JNIHelper::GetFieldID(jenv, c, "sizeInBytes", "I");
         numElementsFieldID = JNIHelper::GetFieldID(jenv, c, "numElements", "I");
         argClazz  = c;
      }
      type = jenv->GetIntField(argObj, typeFieldID);
      jstring nameString  = (jstring)jenv->GetObjectField(argObj, nameFieldID);
      const char *nameChars = jenv->GetStringUTFChars(nameString, NULL);
      name = strdup(nameChars);
      jenv->ReleaseStringUTFChars(nameString, nameChars);
   }

const char* KernelArg::getTypeName() {
   string s = "";
   if(isStatic()) {
      s += "static ";
   }
   if (isFloat()) {
      s += "float";
   }
   else if(isInt()) {
      s += "int";
   }
   else if(isBoolean()) {
      s += "boolean";
   }
   else if(isByte()) {
      s += "byte";
   }
   else if(isLong()) {
      s += "long";
   }
   else if(isDouble()) {
      s += "double";
   }
   return s.c_str();
}

void KernelArg::getPrimitiveValue(JNIEnv *jenv, jfloat* value) {
   jfieldID fieldID = jenv->GetFieldID(kernelContext->kernelClass, name, "F");
   *value = jenv->GetFloatField(kernelContext->kernelObject, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jint* value) {
   jfieldID fieldID = jenv->GetFieldID(kernelContext->kernelClass, name, "I");
   *value = jenv->GetIntField(kernelContext->kernelObject, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jboolean* value) {
   jfieldID fieldID = jenv->GetFieldID(kernelContext->kernelClass, name, "B");
   *value = jenv->GetByteField(kernelContext->kernelObject, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jbyte* value) {
   jfieldID fieldID = jenv->GetFieldID(kernelContext->kernelClass, name, "B");
   *value = jenv->GetByteField(kernelContext->kernelObject, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jlong* value) {
   jfieldID fieldID = jenv->GetFieldID(kernelContext->kernelClass, name, "J");
   *value = jenv->GetLongField(kernelContext->kernelObject, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jdouble* value) {
   jfieldID fieldID = jenv->GetFieldID(kernelContext->kernelClass, name, "D");
   *value = jenv->GetDoubleField(kernelContext->kernelObject, fieldID);
}

void KernelArg::getStaticPrimitiveValue(JNIEnv *jenv, jfloat* value) {
   jfieldID fieldID = jenv->GetStaticFieldID(kernelContext->kernelClass, name, "F");
   *value = jenv->GetStaticFloatField(kernelContext->kernelClass, fieldID);
}
void KernelArg::getStaticPrimitiveValue(JNIEnv *jenv, jint* value) {
   jfieldID fieldID = jenv->GetStaticFieldID(kernelContext->kernelClass, name, "I");
   *value = jenv->GetStaticIntField(kernelContext->kernelClass, fieldID);
}
void KernelArg::getStaticPrimitiveValue(JNIEnv *jenv, jboolean* value) {
   jfieldID fieldID = jenv->GetStaticFieldID(kernelContext->kernelClass, name, "Z");
   *value = jenv->GetStaticBooleanField(kernelContext->kernelClass, fieldID);
}
void KernelArg::getStaticPrimitiveValue(JNIEnv *jenv, jbyte* value) {
   jfieldID fieldID = jenv->GetStaticFieldID(kernelContext->kernelClass, name, "B");
   *value = jenv->GetStaticByteField(kernelContext->kernelClass, fieldID);
}
void KernelArg::getStaticPrimitiveValue(JNIEnv *jenv, jlong* value) {
   jfieldID fieldID = jenv->GetStaticFieldID(kernelContext->kernelClass, name, "J");
   *value = jenv->GetStaticLongField(kernelContext->kernelClass, fieldID);
}
void KernelArg::getStaticPrimitiveValue(JNIEnv *jenv, jdouble* value) {
   jfieldID fieldID = jenv->GetStaticFieldID(kernelContext->kernelClass, name, "D");
   *value = jenv->GetStaticDoubleField(kernelContext->kernelClass, fieldID);
}

cl_int KernelArg::setPrimitiveArg(JNIEnv *jenv, int argIdx, int argPos, bool verbose){
   cl_int status = CL_SUCCESS;
   if (isFloat()) {
       jfloat f;
       getPrimitive(jenv, argIdx, argPos, verbose, &f);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(f), &f);
   }
   else if (isInt()) {
       jint i;
       getPrimitive(jenv, argIdx, argPos, verbose, &i);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(i), &i);
   }
   else if (isBoolean()) {
       jboolean z;
       getPrimitive(jenv, argIdx, argPos, verbose, &z);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(z), &z);
   }
   else if (isByte()) {
       jbyte b;
       getPrimitive(jenv, argIdx, argPos, verbose, &b);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(b), &b);
   }
   else if (isLong()) {
       jlong l;
       getPrimitive(jenv, argIdx, argPos, verbose, &l);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(l), &l);
   }
   else if (isDouble()) {
       jdouble d;
       getPrimitive(jenv, argIdx, argPos, verbose, &d);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(d), &d);
   }
   return status;
}

void KernelArg::updateReference(JNIEnv *jenv, BufferManager* bufferManager) {
   this->syncType(jenv);

   if (config->isVerbose()){
      fprintf(stderr, "got type for %s: %08x\n", this->name, this->type);
   }

   if (this->isPrimitive()) return;

   if (this->isArray()) {
      jarray newRef = (jarray)jenv->GetObjectField(this->javaArg, KernelArg::javaArrayFieldID);
      if (newRef == NULL) {
         this->arrayBuffer = NULL;
         return;
      }

      bool doUpdate = false;
      if (this->arrayBuffer == NULL) {
         doUpdate = true;
      } else if (!jenv->IsSameObject(newRef, this->arrayBuffer->javaObject)) {
         doUpdate = true;

      }

      if (doUpdate) {
         ArrayBuffer* oldBuffer = this->arrayBuffer;
         this->arrayBuffer = bufferManager->getArrayBufferFor(jenv, newRef);
         if (oldBuffer != arrayBuffer) {
            bufferManager->replacedArrayBuffer = true;
            if (oldBuffer != NULL) {
               oldBuffer->deleteReference();
            }
            this->arrayBuffer->addReference();
         }

         this->syncJavaArrayLength(jenv);
         this->syncSizeInBytes(jenv);
      }
   } else if (this->isAparapiBuffer()) {
      AparapiBuffer* oldBuffer = this->aparapiBuffer;
      this->aparapiBuffer = bufferManager->getAparapiBufferFor(jenv, javaArg, type);

      if (oldBuffer != this->aparapiBuffer) {
         bufferManager->replacedAparapiBuffer = true;
         if (oldBuffer != NULL) {
            oldBuffer->deleteReference();
         }
         this->aparapiBuffer->addReference();
      }
   }
}


