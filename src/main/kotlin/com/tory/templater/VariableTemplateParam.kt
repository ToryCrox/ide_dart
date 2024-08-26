package com.tory.templater

// The name the named constructor parameter has - needed when called from a copy/fromJson method
val VariableTemplateParam.namedConstructorParamName: String
    get() = publicVariableName

// To make the reading and writing from the map consistent
val VariableTemplateParam.mapKeyString: String
    get() = variableName

data class VariableTemplateParam(
    val variableName: String,
    val isNullable: Boolean,
    val type: String,
    val dartType: ParamDartType,
    val publicVariableName: String,
    val jsonKey: String? = null,
    val isEnum: Boolean = false,
    val enumVariableList: List<String> = emptyList()
)

/**
 * 参数类型
 */
data class ParamDartType(
    val fullTypeName: String,
    val typeName: String,
    val isNullable: Boolean,
    val argumentTypeList: List<ParamDartType>,
)

val dynamicDartType = ParamDartType(
    fullTypeName = "dynamic",
    typeName = "dynamic",
    isNullable = true,
    argumentTypeList = emptyList()
)

val equalsDefaultTemplateParam = VariableTemplateParam(
    variableName = "runtimeType",
    isNullable = false,
    type = "Type",
    dartType = ParamDartType(fullTypeName = "", typeName = "", isNullable = false, argumentTypeList = emptyList()),
    publicVariableName = "runtimeType",
    isEnum = false,
    enumVariableList = emptyList()
)

/// 默认值
val VariableTemplateParam.defaultValue: String
    get() = when (type) {
        "String" -> "''"
        "int" -> "0"
        "double" -> "0"
        "bool" -> "false"
        else -> when {
            type.startsWith("List") -> "const []"
            type.startsWith("Map") -> "const {}"
            type.startsWith("Set") -> "const {}"
            isEnum -> "$type.${enumVariableList.first()}"
            else -> "const $type()"
        }
    }
