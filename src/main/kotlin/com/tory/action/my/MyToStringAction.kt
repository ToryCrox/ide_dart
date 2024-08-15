package com.tory.action.my

import com.tory.action.StaticActionProcessor
import com.tory.action.data.GenerationData
import com.tory.action.data.PerformAction
import com.tory.configuration.ConfigurationDataManager
import com.tory.ext.psi.extractClassName
import com.tory.ext.psi.findMethodsByName
import com.tory.templater.TemplateConstants
import com.tory.templater.ToStringTemplateParams
import com.tory.templater.createToStringTemplate
import com.intellij.codeInsight.template.TemplateManager
import com.jetbrains.lang.dart.psi.DartClassDefinition
import com.tory.declaration.toVariableTemplateParam

class MyToStringAction {
    companion object : StaticActionProcessor {

        private fun createDeleteCall(dartClass: DartClassDefinition): (() -> Unit)? {
            val toString = dartClass.findMethodsByName(TemplateConstants.TO_STRING_METHOD_NAME)
                .firstOrNull()
                ?: return null

            return { toString.delete() }
        }

        override fun processAction(generationData: GenerationData): PerformAction? {
            val (actionData, dartClass, declarations) = generationData

            val project = actionData.project

            val configuration = ConfigurationDataManager.retrieveData(project)
            val templateManager = TemplateManager.getInstance(project)
            val dartClassName = dartClass.extractClassName()

            val template = createToStringTemplate(
                templateManager = templateManager,
                params = ToStringTemplateParams(
                    className = dartClassName,
                    variables = declarations.map {
                        it.toVariableTemplateParam()
                    }
                ),
                configuration = configuration
            )

            return PerformAction(
                createDeleteCall(dartClass),
                template
            )
        }
    }
}
