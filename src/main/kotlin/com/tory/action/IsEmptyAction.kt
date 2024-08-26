package com.tory.action

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.DartClassDefinition
import com.jetbrains.lang.dart.psi.DartGetterDeclaration
import com.tory.action.data.GenerationData
import com.tory.action.data.PerformAction
import com.tory.action.init.ActionData
import com.tory.action.utils.selectFieldsWithDialog
import com.tory.configuration.ConfigurationDataManager
import com.tory.declaration.toVariableTemplateParam
import com.tory.ext.psi.extractClassName
import com.tory.ext.psi.findChildrenByType
import com.tory.ext.psi.findMethodsByName
import com.tory.templater.EmptyTemplateParams
import com.tory.templater.TemplateConstants
import com.tory.templater.createEqualsTemplate
import com.tory.templater.createIsEmptyTemplate

class IsEmptyAction : BaseAnAction() {

    override fun processAction(
        event: AnActionEvent,
        actionData: ActionData,
        dartClass: DartClassDefinition
    ): PerformAction? {
        val project = actionData.project
        val declarations = selectFieldsWithDialog(project, dartClass)
            ?: return null

        val generationData = GenerationData(actionData, dartClass, declarations)

        return Companion.processAction(generationData)
    }

    companion object : StaticActionProcessor  {

        override fun processAction(generationData: GenerationData): PerformAction? {
            val (actionData, dartClass, declarations) = generationData
            val (project, _, _, _) = actionData

            val configuration = ConfigurationDataManager.retrieveData(project)
            val templateManager = TemplateManager.getInstance(project)
            val dartClassName = dartClass.extractClassName()

            val template = createIsEmptyTemplate(
                templateManager,
                configuration.parseWrapper,
                EmptyTemplateParams(
                    className = dartClassName,
                    variables = declarations.map {
                        println("${it.name}, ${it.dartType?.referenceExpression?.text}, ${it.dartType?.typeArguments?.typeList?.typeList?.map { it.text }}")
                        it.toVariableTemplateParam()
                    }
                )
            )

            return PerformAction(
                createDeleteCall(dartClass),
                template
            )
        }

        private fun createDeleteCall(dartClass: DartClassDefinition): (() -> Unit)? {
            val equals = dartClass.findChildrenByType<DartGetterDeclaration>()
                .filter { it.name == TemplateConstants.IS_EMPTY_METHOD_NAME }
                .firstOrNull()
                ?: return null

            return { equals.delete() }
        }
    }
}