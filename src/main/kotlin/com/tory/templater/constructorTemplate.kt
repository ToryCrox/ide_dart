package com.tory.templater

import com.tory.ext.*
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager

data class ConstructorTemplateParams(
    val className: String,
    val publicVariables: List<VariableTemplateParam>,
    val privateVariables: List<VariableTemplateParam>,
    val addRequiredAnnotation: Boolean,
    val addConstQualifier: Boolean,
    val nullSafety: Boolean
)

/**
 * 创建构造方法
 */
fun createConstructorTemplate(
    templateManager: TemplateManager,
    params: ConstructorTemplateParams
): Template {
    val (
        className,
        publicVariableNames,
        privateVariables,
        addRequiredAnnotation,
        addConstQualifier,
        nullSafety
    ) = params

    return templateManager.createTemplate(
        TemplateType.NamedParameterConstructor.templateKey,
        TemplateConstants.DART_TEMPLATE_GROUP
    ).apply {
        isToReformat = true

        if (addConstQualifier) {
            addTextSegment("const")
            addTextSegment(" ")
        }

        addTextSegment(className)
        withParentheses {
            if (publicVariableNames.isNotEmpty() || privateVariables.isNotEmpty()) {
                withCurlyBraces {
                    addNewLine()

                    publicVariableNames.forEach {
                        if (!it.isNullable) {
                            addTextSegment("required")
                            addSpace()
                        }

                        addTextSegment("this.")
                        addTextSegment(it.variableName)
//                        if (!it.isNullable) {
//                            addTextSegment(" = ")
//                            addTextSegment(it.defaultValue)
//                        }
                        addComma()
                        addNewLine()
                    }

                    privateVariables.forEach {
                        if (addRequiredAnnotation) {
                            addTextSegment("@required")
                            addSpace()
                        }
                        if (nullSafety) {
                            addTextSegment("required")
                            addSpace()
                        }

                        // No this
                        addTextSegment(it.type)
                        if (nullSafety && it.isNullable) {
                            addTextSegment("?")
                        }
                        addSpace()
                        addTextSegment(it.publicVariableName)
                        addComma()
                        addNewLine()
                    }
                }
            }
        }

        if (privateVariables.isNotEmpty()) {
            addSpace()
            addTextSegment(":")
            addSpace()

            privateVariables.forEachIndexed { index, it ->
                addTextSegment(it.variableName)
                addSpace()
                addTextSegment("=")
                addSpace()
                // Same as used for function input
                addTextSegment(it.publicVariableName)

                if (index != privateVariables.lastIndex) {
                    addComma()
                }
            }
        }

        addSemicolon()

        // Line break helps formatting
        addSpace()
    }
}