/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.adapter.hive;


import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.type.SqlReturnTypeInference;

import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * A HiveSqlUDFReturnTypeInference can infer the result type of a Hive UDF through
 * {@link GenericUDF#initialize(ObjectInspector[])}
 */
public class HiveSqlUDFReturnTypeInference implements SqlReturnTypeInference {

  public static final HiveSqlUDFReturnTypeInference INSTANCE =
      new HiveSqlUDFReturnTypeInference();

  @Override public RelDataType inferReturnType(
      final SqlOperatorBinding opBinding) {
    try {
      SqlOperator sqlOperator = opBinding.getOperator();
      List<RelDataTypeHolder> argsType = new ArrayList<>();
      for (int i = 0; i < opBinding.getOperandCount(); i++) {
        RelDataTypeHolder relDataTypeHolder;
        boolean isSqlCallBinding = opBinding instanceof SqlCallBinding;
        if (isSqlCallBinding) {
          List<SqlNode> operands = ((SqlCallBinding) opBinding).operands();
          if (operands.get(i) instanceof SqlLiteral) {
            relDataTypeHolder = new RelDataTypeHolder(
                opBinding.getOperandType(i), true,
                ((SqlLiteral) operands.get(i)).getValue());
          } else {
            relDataTypeHolder = new RelDataTypeHolder(
                opBinding.getOperandType(i));
          }

        } else {
          relDataTypeHolder = new RelDataTypeHolder(
              opBinding.getOperandType(i));
        }
        argsType.add(relDataTypeHolder);
      }
      String opName = sqlOperator.getName();
      GenericUDF udfInstance = HiveUDFImplementor.newGenericUDF(
          opName, sqlOperator.getSyntax());
      ObjectInspector[] inputObjectInspector =
          TypeInferenceUtil.getObjectInspector(
              argsType.toArray(new RelDataTypeHolder[0]));
      ObjectInspector outputObjectInspector = udfInstance.initialize(
          inputObjectInspector);
      RelDataType resultType = TypeInferenceUtil.getRelDataType(
          outputObjectInspector,
          opBinding.getTypeFactory());
      return resultType;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

// End HiveSqlUDFReturnTypeInference.java
