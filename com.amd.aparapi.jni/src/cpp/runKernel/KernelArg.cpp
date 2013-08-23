#include "KernelArg.h"
#include "KernelContext.h"
#include "BufferManager.h"
#include <string>
#include <iostream>
#include <vector>

using std::string;
using std::cerr;
using std::endl;

jclass KernelArg::argClazz=(jclass)0;
jfieldID KernelArg::nameFieldID=0;
jfieldID KernelArg::typeFieldID=0; 
jfieldID KernelArg::javaArrayFieldID=0; 
jfieldID KernelArg::sizeInBytesFieldID=0;
jfieldID KernelArg::numElementsFieldID=0; 
jfieldID KernelArg::inlinePathVariableNamesFieldID=0; 
jfieldID KernelArg::inlinePathVariableTypesFieldID=0; 

KernelArg::KernelArg(JNIEnv *jenv, jobject argObj, KernelContext *_kernelContext):
   argObj(argObj),
   buffer(NULL),
   //aparapiBuffer(NULL),
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
         inlinePathVariableNamesFieldID = JNIHelper::GetFieldID(jenv, c, "inlineReferencePathVariableName", "Ljava/lang/String;");
         inlinePathVariableTypesFieldID = JNIHelper::GetFieldID(jenv, c, "inlineReferencePathVariableType", "Ljava/lang/String;");
         argClazz  = c;
      }
      type = jenv->GetIntField(argObj, typeFieldID);

      name = JNIHelper::getStringFieldContentAsCharArray(jenv, argObj, nameFieldID);
      if (isPrimitive()) {
         char* inlinePathVariableNames = JNIHelper::getStringFieldContentAsCharArray(jenv, argObj, inlinePathVariableNamesFieldID);
         char* inlinePathVariableTypes = JNIHelper::getStringFieldContentAsCharArray(jenv, argObj, inlinePathVariableTypesFieldID);

         splitStringBy(inlinePathVariableNames, ".", &inlinePathVariableNamesParts);
         splitStringBy(inlinePathVariableTypes, ".", &inlinePathVariableTypesParts);

         free(inlinePathVariableNames);
         free(inlinePathVariableTypes);
      }
   }

void KernelArg::splitStringBy(char *toSplit, char *delimiter, std::vector<char*> *resultVector) {
   char * pnt;
   pnt = strtok(toSplit, delimiter);
   while(pnt!= NULL) {
      resultVector->push_back(strdup(pnt));
      pnt = strtok(NULL, ".");
   }
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

void KernelArg::getPrimitiveValue(JNIEnv *jenv, jfloat* value, jobject baseRef, jclass baseClass) {
   jfieldID fieldID = jenv->GetFieldID(baseClass, name, "F");
   *value = jenv->GetFloatField(baseRef, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jint* value, jobject baseRef, jclass baseClass) {
   jfieldID fieldID = jenv->GetFieldID(baseClass, name, "I");
   *value = jenv->GetIntField(baseRef, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jboolean* value, jobject baseRef, jclass baseClass) {
   jfieldID fieldID = jenv->GetFieldID(baseClass, name, "B");
   *value = jenv->GetByteField(baseRef, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jbyte* value, jobject baseRef, jclass baseClass) {
   jfieldID fieldID = jenv->GetFieldID(baseClass, name, "B");
   *value = jenv->GetByteField(baseRef, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jlong* value, jobject baseRef, jclass baseClass) {
   jfieldID fieldID = jenv->GetFieldID(baseClass, name, "J");
   *value = jenv->GetLongField(baseRef, fieldID);
}
void KernelArg::getPrimitiveValue(JNIEnv *jenv, jdouble* value, jobject baseRef, jclass baseClass) {
   jfieldID fieldID = jenv->GetFieldID(baseClass, name, "D");
   *value = jenv->GetDoubleField(baseRef, fieldID);
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
   jobject baseReference = kernelContext->kernelObject;

   for (int i = 0; i < inlinePathVariableNamesParts.size(); i++) {
      char* name = inlinePathVariableNamesParts.at(i);
      char* type = inlinePathVariableTypesParts.at(i);

      jclass c = jenv->GetObjectClass(baseReference);
      jfieldID fieldID = JNIHelper::GetFieldID(jenv, c, name, type);
      
      baseReference = (jobject) jenv->GetObjectField(baseReference, fieldID);
   }
   jclass baseClass = jenv->GetObjectClass(baseReference); 

   cl_int status = CL_SUCCESS;
   if (isFloat()) {
       jfloat f;
       getPrimitive(jenv, argIdx, argPos, verbose, &f, baseReference, baseClass);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(f), &f);
   }
   else if (isInt()) {
       jint i;
       getPrimitive(jenv, argIdx, argPos, verbose, &i, baseReference, baseClass);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(i), &i);
   }
   else if (isBoolean()) {
       jboolean z;
       getPrimitive(jenv, argIdx, argPos, verbose, &z, baseReference, baseClass);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(z), &z);
   }
   else if (isByte()) {
       jbyte b;
       getPrimitive(jenv, argIdx, argPos, verbose, &b, baseReference, baseClass);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(b), &b);
   }
   else if (isLong()) {
       jlong l;
       getPrimitive(jenv, argIdx, argPos, verbose, &l, baseReference, baseClass);
       status = clSetKernelArg(kernelContext->kernel, argPos, sizeof(l), &l);
   }
   else if (isDouble()) {
       jdouble d;
       getPrimitive(jenv, argIdx, argPos, verbose, &d, baseReference, baseClass);
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
         this->buffer = NULL;
         return;
      }

      bool doUpdate = false;
      if (this->buffer == NULL) {
         doUpdate = true;
      } else if (!jenv->IsSameObject(newRef, this->buffer->javaObject)) {
         doUpdate = true;
      }

      if (doUpdate) {
         GPUElement* oldBuffer = this->buffer;
         this->buffer = bufferManager->getArrayBufferFor(jenv, newRef);
         if (oldBuffer != buffer) {
            bufferManager->replacedArrayBuffer = true;
            if (oldBuffer != NULL) {
               oldBuffer->deleteReference();
            }
            this->buffer->addReference();
         }

         this->syncJavaArrayLength(jenv);
         this->syncSizeInBytes(jenv);
      }
   } else if (this->isAparapiBuffer()) {
      GPUElement* oldBuffer = this->buffer;
      this->buffer = bufferManager->getAparapiBufferFor(jenv, javaArg, type);

      if (oldBuffer != this->buffer) {
         bufferManager->replacedAparapiBuffer = true;
         if (oldBuffer != NULL) {
            oldBuffer->deleteReference();
         }
         this->buffer->addReference();
      }
   }
}


