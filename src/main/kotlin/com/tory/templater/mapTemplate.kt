package com.tory.templater

import com.tory.configuration.ParseWrapper
import com.tory.ext.*
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager

data class MapTemplateParams(
    val className: String,
    val variables: List<VariableTemplateParam>,
    val useNewKeyword: Boolean,
    val addKeyMapper: Boolean,
    val noImplicitCasts: Boolean,
    /**
     * 是否使用下划线的Json名称
     */
    val useUnderlineJsonName: Boolean,
    val parseWrapper: ParseWrapper
)

// The 2 will be generated with the same function
fun createMapTemplate(
    templateManager: TemplateManager,
    params: MapTemplateParams
): Template {

    return templateManager.createTemplate(
        TemplateType.MapTemplate.templateKey,
        TemplateConstants.DART_TEMPLATE_GROUP
    ).apply {
        addFromMap(params)
        addNewLine()
        addNewLine()
        addToMap(params)
    }
}

private fun Template.addAssignKeyMapperIfNotValid() {
    addTextSegment(TemplateConstants.KEYMAPPER_VARIABLE_NAME)
    addSpace()
    addTextSegment("??=")
    addSpace()
    withParentheses {
        addTextSegment(TemplateConstants.KEY_VARIABLE_NAME)
    }
    addSpace()
    addTextSegment("=>")
    addSpace()
    addTextSegment(TemplateConstants.KEY_VARIABLE_NAME)
    addSemicolon()
    addNewLine()
    addNewLine()
}

private fun Template.addToMap(params: MapTemplateParams) {
    val (_, variables, _, addKeyMapper, _) = params
    val useUnderlineJsonName = params.useUnderlineJsonName

    isToReformat = true

    addTextSegment("Map<String, dynamic>")
    addSpace()
    addTextSegment(TemplateConstants.TO_MAP_METHOD_NAME)
    withParentheses {
        if (addKeyMapper) {
            withCurlyBraces {
                addNewLine()
                addTextSegment("String Function(String key)? ${TemplateConstants.KEYMAPPER_VARIABLE_NAME}")
                addComma()
                addNewLine()
            }
        }
    }
    addSpace()
    withCurlyBraces {

        if (addKeyMapper) {
            addAssignKeyMapperIfNotValid()
        }

        addTextSegment("return")
        addSpace()
        withCurlyBraces {
            addNewLine()

            variables.forEach {
                val jsonKey = it.jsonKey ?: if (useUnderlineJsonName) camelCaseToSnakeCase(it.mapKeyString) else it.mapKeyString
                "'$jsonKey'".also { keyParam ->
                    if (addKeyMapper) {
                        addTextSegment(TemplateConstants.KEYMAPPER_VARIABLE_NAME)
                        withParentheses {
                            addTextSegment(keyParam)
                        }
                    } else {
                        addTextSegment(keyParam)
                    }
                }

                addTextSegment(":")
                addSpace()
                toJsonMapValue(it.dartType, it.variableName, isEnum = it.isEnum)
                addComma()
                addNewLine()
            }
        }
        addSemicolon()
    }
}

private val DART_BASIC_TYPES = setOf("num", "int", "double", "bool", "String", "List", "Map", "Set", "color")
private fun Template.toJsonMapValue(dartType: ParamDartType, variableName: String, isEnum: Boolean = false) {
    if (dartType.typeName in setOf("List", "Set")) {
        val subType = dartType.argumentTypeList.firstOrNull() ?: dynamicDartType
        if (subType.typeName in DART_BASIC_TYPES) {
            addTextSegment(variableName)
        } else {
            addTextSegment(variableName)
            addTextSegment(".map((e)=> ")
            toJsonMapValue(subType, "e")
            addTextSegment(").toList()")
        }
    } else if (dartType.typeName in DART_BASIC_TYPES) {
        addTextSegment(variableName)
    } else if (isEnum) {
        addTextSegment("$variableName.name")
    } else {
        addTextSegment("$variableName.toMap()")
    }
}

private fun Template.addFromMap(
    params: MapTemplateParams
) {
    val (className, variables, useNewKeyword, addKeyMapper, noImplicitCasts) = params
    val useUnderlineJsonName = params.useUnderlineJsonName

    isToReformat = true

    addTextSegment("factory")
    addSpace()
    addTextSegment(className)
    addTextSegment(".")
    addTextSegment(TemplateConstants.FROM_MAP_METHOD_NAME)
    withParentheses {
        if (addKeyMapper) {
            addNewLine()
            // New line does not format, no matter what is in this if statement
            addSpace()
        }
        addTextSegment("Map<String, dynamic>")
        addSpace()
        addTextSegment(TemplateConstants.MAP_VARIABLE_NAME)

        if (addKeyMapper) {
            addComma()
            addSpace()
            withCurlyBraces {
                addNewLine()
                addTextSegment("String Function(String ${TemplateConstants.KEY_VARIABLE_NAME})?")
                addSpace()
                addTextSegment(TemplateConstants.KEYMAPPER_VARIABLE_NAME)
                addComma()
                addNewLine()
            }
        }
    }
    addSpace()
    withCurlyBraces {

        if (addKeyMapper) {
            addAssignKeyMapperIfNotValid()
        }

        addTextSegment("return")
        addSpace()
        if (useNewKeyword) {
            addTextSegment("new")
            addSpace()
        }
        addTextSegment(className)
        withParentheses {
            addNewLine()
            variables.forEach {
                addTextSegment(it.publicVariableName)
                addTextSegment(":")
                addSpace()

                // 添加map[key]
                val addMapValue = {
                    addTextSegment(TemplateConstants.MAP_VARIABLE_NAME)
                    withBrackets {
                        val jsonKey = it.jsonKey ?: if (useUnderlineJsonName) camelCaseToSnakeCase(it.mapKeyString) else it.mapKeyString
                        "'$jsonKey'".also { keyParam ->
                            if (addKeyMapper) {
                                addTextSegment(TemplateConstants.KEYMAPPER_VARIABLE_NAME)
                                withParentheses {
                                    addTextSegment(keyParam)
                                }
                            } else {
                                addTextSegment(keyParam)
                            }
                        }
                    }
                }

                val isWrapped =
                    withParseWrapper(dartType = it.dartType, parseWrapper = params.parseWrapper, isEnum = it.isEnum) {
                        addMapValue()
                    }

                // 非空设置默认值
                if (!isWrapped && !it.isNullable) {
                    addSpace()
                    //addTextSegment("as")
                    addTextSegment("??")
                    addSpace()
                    addTextSegment(it.defaultValue)
                }

                addComma()
                addNewLine()
            }
        }
        addSemicolon()
    }
}

// 包裹自定义装换
fun Template.withParseWrapper(
    dartType: ParamDartType,
    parseWrapper: ParseWrapper,
    isEnum: Boolean = false,
    action: Template.() -> Unit
): Boolean {
    val parseWrapperMethod = when (dartType.fullTypeName) {
        "String" -> "parseString"
        "int" -> "parseInt"
        "double" -> "parseDouble"
        "bool" -> "parseBool"
        "Map" -> "parseMap"
        "Map<String, dynamic>" -> "parseMap"
        "List<Int>" -> "parseIntList"
        "List<String>" -> "parseStringList"
        else -> ""
    }
    if (parseWrapperMethod.isNotBlank()) {
        this.addTextSegment("${parseWrapper.parseClassName}.")
        this.addTextSegment(parseWrapperMethod)
        this.addTextSegment("(")
        this.action()
        this.addTextSegment(")")
        return true
    } else if (dartType.typeName == "List") {
        this.addTextSegment("${parseWrapper.parseClassName}.")
        this.addTextSegment("parseList")
        this.addTextSegment("(")
        this.action()
        this.addTextSegment(", (e) => ")
        this.withParseWrapper(dartType.argumentTypeList.firstOrNull() ?: dynamicDartType, parseWrapper) {
            this.addTextSegment("e")
        }
        this.addTextSegment(")")
        return true
    } else if (dartType.typeName == "Set") {
        this.withParseWrapper(dartType.copy(typeName = "List", fullTypeName = "List"), parseWrapper) {
            action()
        }
        addTextSegment(".toSet()")
        return true
    } else if (dartType.typeName == "dynamic") {
        action()
        return false
    } else if (isEnum) {
        this.addTextSegment(dartType.typeName)
        this.addTextSegment(".values.asNameMap()[")
        this.addTextSegment("${parseWrapper.parseClassName}.parseString")
        this.withParentheses {
            action()
        }
        this.addTextSegment("]")
        this.addTextSegment(" ?? ${dartType.typeName}.values[0]")

        return true
    } else {
        this.addTextSegment(dartType.typeName)
        this.addTextSegment(".fromMap")
        this.withParentheses {
            this.addTextSegment("${parseWrapper.parseClassName}.")
            this.addTextSegment("parseMap")
            this.withParentheses {
                action()
            }
        }
        return true
    }
}

/**
 * 驼峰换成下划线的形式
 */
fun camelCaseToSnakeCase(input: String): String {
    val result = StringBuilder()
    var prevCharIsUpperCase = false

    for (char in input) {
        if (char.isUpperCase()) {
            if (!prevCharIsUpperCase && result.isNotEmpty()) {
                result.append('_')
            }
            result.append(char.toLowerCase())
            prevCharIsUpperCase = true
        } else {
            result.append(char)
            prevCharIsUpperCase = false
        }
    }

    return result.toString()
}