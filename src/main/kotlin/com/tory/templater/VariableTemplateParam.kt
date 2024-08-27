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
    val isEnum: Boolean = false,
    val enumVariableList: List<String> = emptyList()
)

val dynamicDartType = ParamDartType(
    fullTypeName = "dynamic",
    typeName = "dynamic",
    isNullable = true,
    argumentTypeList = emptyList()
)

val stringDartType = ParamDartType(
    fullTypeName = "String",
    typeName = "String",
    isNullable = false,
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
    get() = when (dartType.typeName) {
        "String" -> "''"
        "num" -> "0"
        "int" -> "0"
        "double" -> "0"
        "bool" -> "false"
        "List" -> "const []"
        "Map" -> "const {}"
        "Set" -> "const {}"
        else -> when {
            isEnum -> "$type.${enumVariableList.first()}"
            else -> "const $type()"
        }
    }
