package com.tory.action

import com.tory.action.data.GenerationData
import com.tory.action.data.PerformAction
import com.tory.action.init.ActionData
import com.tory.action.utils.createConstructorDeleteCallWithUserPrompt
import com.tory.action.utils.selectFieldsWithDialog
import com.tory.configuration.ConfigurationDataManager
import com.tory.declaration.*
import com.tory.ext.psi.extractClassName
import com.tory.templater.*
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.DartClassDefinition

class NamedArgumentConstructorAction : BaseAnAction() {

    override fun processAction(
        event: AnActionEvent,
        actionData: ActionData,
        dartClass: DartClassDefinition
    ): PerformAction? {
        val declarations = selectFieldsWithDialog(actionData.project, dartClass) ?: return null

        return Companion.processAction(
            GenerationData(actionData, dartClass, declarations)
        )
    }

    companion object : StaticActionProcessor {

        override fun processAction(generationData: GenerationData): PerformAction? {
            val (actionData, dartClass, declarations) = generationData
            val (project, _, _, _) = actionData

            val publicVariables: List<VariableTemplateParam> = declarations
                .filter { it.isPublic }
                .map {
                    it.toVariableTemplateParam()
                }

            val privateVariables: List<VariableTemplateParam> = declarations
                .filter { it.isPrivate }
                .map {
                    it.toVariableTemplateParam()
                }

            val templateManager = TemplateManager.getInstance(project)
            val configuration = ConfigurationDataManager.retrieveData(project)
            val dartClassName = dartClass.extractClassName()
            val addConstQualifier = configuration.useConstForConstructor && declarations.allMembersFinal()

            val template = createConstructorTemplate(
                templateManager,
                ConstructorTemplateParams(
                    className = dartClassName,
                    publicVariables = publicVariables,
                    privateVariables = privateVariables,
                    addRequiredAnnotation = configuration.useRequiredAnnotation
                            && !configuration.nullSafety,
                    addConstQualifier = addConstQualifier,
                    nullSafety = configuration.nullSafety
                )
            )

            val constructorDeleteCall = createConstructorDeleteCallWithUserPrompt(project, dartClass)

            return PerformAction(
                constructorDeleteCall,
                template
            )
        }

    }
}