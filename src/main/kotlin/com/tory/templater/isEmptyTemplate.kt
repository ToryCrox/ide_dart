package com.tory.templater

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.tory.configuration.ParseWrapper
import com.tory.ext.*

data class EmptyTemplateParams(
    val className: String,
    val variables: List<VariableTemplateParam>
)


fun createIsEmptyTemplate(
    templateManager: TemplateManager,
    parseWrapper: ParseWrapper,
    params: EmptyTemplateParams
): Template {
    val (className, variables) = params

    return templateManager.createDartTemplate(TemplateType.IsEmpty)
        .apply {
            addTextSegment("bool")
            addSpace()
            addTextSegment("get")
            addSpace()
            addTextSegment(TemplateConstants.IS_EMPTY_METHOD_NAME)
            addSpace()
            addTextSegment("=>")
            addNewLine()

            if (variables.isEmpty()) {
                addTextSegment("true")
            }

            variables.forEachIndexed { index, variable ->
                addTextSegment(variable.variableName)
                if (variable.isNullable) {
                    addTextSegment(" == null")
                } else {
                    when(variable.dartType.typeName){
                        "int", "double" -> {
                            addTextSegment(" == 0")
                        }
                        "bool" -> {
                            addTextSegment(" == false")
                        }
                        "String", "List", "Set", "Map" -> {
                            addDot()
                            addTextSegment("isEmpty")
                        }
                        else -> {
                            when{
                                variable.isEnum -> {
                                    addTextSegment(" == ")
                                    addTextSegment(variable.defaultValue)
                                }
                                else -> {
                                    addDot()
                                    addTextSegment("isEmpty")
                                }
                            }
                        }
                    }
                }


                if (index != variables.lastIndex) {
                    addSpace()
                    addTextSegment("&&")
                }
            }

            addSemicolon()
            addSpace()
        }

}
