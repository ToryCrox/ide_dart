package com.tory.action.my

import com.tory.action.StaticActionProcessor
import com.tory.action.data.GenerationData
import com.tory.action.data.PerformAction
import com.tory.ext.psi.findChildrenByType
import com.tory.templater.HashCodeTemplateParams
import com.tory.templater.TemplateConstants
import com.tory.templater.createHashCodeTemplate
import com.intellij.codeInsight.template.TemplateManager
import com.jetbrains.lang.dart.psi.DartClassDefinition
import com.jetbrains.lang.dart.psi.DartGetterDeclaration
import com.tory.declaration.toVariableTemplateParam

class MyHashCodeAction {

    companion object : StaticActionProcessor {

        private fun createDeleteCall(dartClass: DartClassDefinition): (() -> Unit)? {
            val hashCode = dartClass.findChildrenByType<DartGetterDeclaration>()
                .filter { it.name == TemplateConstants.HASHCODE_NAME }
                .firstOrNull()
                ?: return null

            return { hashCode.delete() }
        }

        override fun processAction(generationData: GenerationData): PerformAction? {
            val (actionData, dartClass, declarations) = generationData

            val project = actionData.project

            val templateManager = TemplateManager.getInstance(project)

            val template = createHashCodeTemplate(
                templateManager = templateManager,
                params = HashCodeTemplateParams(
                    declarations.map {
                        it.toVariableTemplateParam()
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
