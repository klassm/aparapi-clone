package com.amd.aparapi.internal.model;

import com.amd.aparapi.internal.exception.AparapiException;
import com.amd.aparapi.internal.exception.ClassParseException;
import com.amd.aparapi.internal.instruction.ExpressionList;
import com.amd.aparapi.internal.instruction.Instruction;
import com.amd.aparapi.internal.instruction.InstructionSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MethodModel {
   String getName();

   boolean isGetter();

   boolean isSetter();

   boolean methodUsesPutfield();

   ClassModel.ClassModelMethod getMethod();

   ClassModel.ConstantPool.FieldEntry getAccessorVariableFieldEntry();

   Set<MethodModelRaw> getCalledMethods();

   void checkForRecursion(Set<MethodModelRaw> transitiveCalledMethods) throws AparapiException;

   void setRequiredPragmas(Instruction instruction);

   boolean requiresDoublePragma();

   boolean requiresByteAddressableStorePragma();

   Map<Integer, Instruction> createListOfInstructions() throws ClassParseException;

   void buildBranchGraphs(Map<Integer, Instruction> pcMap);

   void deoptimizeReverseBranches();

   void txFormDups(ExpressionList _expressionList, Instruction _instruction) throws ClassParseException;

   void foldExpressions() throws ClassParseException;

   void applyTransformations(ExpressionList _expressionList, Instruction _instruction, Instruction _operandStart)
         throws ClassParseException;

   void checkForGetter(Map<Integer, Instruction> pcMap) throws ClassParseException;

   void checkForSetter(Map<Integer, Instruction> pcMap) throws ClassParseException;

   void init(ClassModel.ClassModelMethod _method) throws AparapiException;

   ClassModel.LocalVariableTableEntry<ClassModel.LocalVariableInfo> getLocalVariableTableEntry();

   ClassModel.ConstantPool getConstantPool();

   ClassModel.LocalVariableInfo getLocalVariable(int _pc, int _index);

   String getSimpleName();

   String getReturnType();

   List<InstructionSet.MethodCall> getMethodCalls();

   Instruction getPCHead();

   Instruction getExprHead();
}
