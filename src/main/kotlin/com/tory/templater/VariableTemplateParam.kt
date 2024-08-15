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
    val publicVariableName: String,
    val isEnum: Boolean,
    val enumVariableList: List<String>
)

val equalsDefaultTemplateParam = VariableTemplateParam(
    variableName = "runtimeType",
    isNullable = false,
    type = "Type",
    publicVariableName = "runtimeType",
    isEnum = false,
    enumVariableList = emptyList()
)

/// 默认值
val VariableTemplateParam.defaultValue: String
    get() = when (type){
        "String" -> "''"
        "int" -> "0"
        "double" -> ""
        "bool" -> "false"
        else -> when {
            type.startsWith("List") -> "const []"
            type.startsWith("Map") -> "const {}"
            type.startsWith("Set") -> "const {}"
            isEnum -> "$type.${enumVariableList.first()}"
            else -> "const $type()"
        }
    }
