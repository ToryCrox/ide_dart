package com.tory.action

import com.tory.action.data.GenerationData
import com.tory.action.data.PerformAction
import com.tory.action.init.ActionData
import com.tory.action.utils.createCopyWithDeleteCall
import com.tory.action.utils.selectFieldsWithDialog
import com.tory.configuration.ConfigurationDataManager
import com.tory.declaration.allMembersFinal
import com.tory.declaration.fullTypeName
import com.tory.declaration.isNullable
import com.tory.declaration.variableName
import com.tory.ext.psi.extractClassName
import com.tory.templater.AliasedVariableTemplateParam
import com.tory.templater.AliasedVariableTemplateParamImpl
import com.tory.templater.CopyWithTemplateParams
import com.tory.templater.createCopyWithConstructorTemplate
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.DartClassDefinition

class DartCopyWithAction : BaseAnAction() {

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

        override fun processAction(generationData: GenerationData): PerformAction {
            val (actionData, dartClass, declarations) = generationData

            val (project, _, _, _) = actionData

            val variableNames: List<AliasedVariableTemplateParam> = declarations
                .map {
                    AliasedVariableTemplateParamImpl(
                        variableName = it.variableName,
                        type = it.fullTypeName
                            ?: throw RuntimeException("No type is available - this variable should not be assignable from constructor"),
                        publicVariableName = it.publicVariableName,
                        isNullable = it.isNullable
                    )
                }

            val templateManager = TemplateManager.getInstance(project)
            val configuration = ConfigurationDataManager.retrieveData(project)
            val dartClassName = dartClass.extractClassName()
            val generateOptimizedCopy = configuration.optimizeConstCopy && declarations.allMembersFinal()

            val template = createCopyWithConstructorTemplate(
                templateManager,
                CopyWithTemplateParams(
                    className = dartClassName,
                    variables = variableNames,
                    copyWithMethodName = configuration.copyWithMethodName,
                    useNewKeyword = configuration.useNewKeyword,
                    generateOptimizedCopy = generateOptimizedCopy,
                    nullSafety = configuration.nullSafety
                )
            )

            val copyWithDeleteCall = createCopyWithDeleteCall(
                dartClass,
                configuration.copyWithMethodName
            )

            return PerformAction(
                copyWithDeleteCall,
                template
            )
        }

    }
}