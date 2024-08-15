package com.tory.action.my

import com.tory.action.StaticActionProcessor
import com.tory.action.data.GenerationData
import com.tory.action.data.PerformAction
import com.tory.configuration.ConfigurationDataManager
import com.tory.declaration.fullTypeName
import com.tory.declaration.isNullable
import com.tory.declaration.variableName
import com.tory.ext.psi.extractClassName
import com.tory.ext.psi.findMethodsByName
import com.tory.templater.*
import com.intellij.codeInsight.template.TemplateManager
import com.jetbrains.lang.dart.psi.DartClassDefinition

class MyEqualsAction {

    companion object : StaticActionProcessor {

        private fun createDeleteCall(dartClass: DartClassDefinition): (() -> Unit)? {
            val equals = dartClass.findMethodsByName(TemplateConstants.EQUALS_OPERATOR_METHOD_NAME)
                .firstOrNull()
                ?: return null

            return { equals.delete() }
        }

        override fun processAction(generationData: GenerationData): PerformAction? {
            val (actionData, dartClass, declarations) = generationData

            val project = actionData.project

            val configuration = ConfigurationDataManager.retrieveData(project)
            val templateManager = TemplateManager.getInstance(project)
            val dartClassName = dartClass.extractClassName()

            val template = createEqualsTemplate(
                templateManager,
                configuration.parseWrapper,
                EqualsTemplateParams(
                    className = dartClassName,
                    variables = declarations.map {
                        AliasedVariableTemplateParamImpl(
                            variableName = it.variableName,
                            isNullable = it.isNullable,
                            publicVariableName = it.publicVariableName,
                            type = it.fullTypeName ?: throw RuntimeException("No type is available - this variable should not be assignable from constructor")
                        )
                    }
                )
            )

            return PerformAction(
                createDeleteCall(dartClass),
                template
            )
        }
    }

}