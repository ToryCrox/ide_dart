package com.tory.action

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiComment
import com.jetbrains.lang.dart.psi.*
import com.tory.action.data.GenerationData
import com.tory.action.data.PerformAction
import com.tory.action.data.combineAll
import com.tory.action.init.ActionData
import com.tory.action.my.MyEqualsAction
import com.tory.action.my.MyHashCodeAction
import com.tory.action.my.MyToStringAction
import com.tory.action.utils.*
import com.tory.declaration.dartTypeName
import com.tory.declaration.variableList
import com.tory.ext.addNewLine
import com.tory.ext.addSpace
import com.tory.ext.createDartTemplate
import com.tory.ext.psi.commentContent
import com.tory.ext.psi.findChildrenByType
import com.tory.ext.psi.isTopClassLevel
import com.tory.templater.TemplateConstants
import com.tory.templater.TemplateType
import com.tory.traversal.TraversalType

class FullDataAction : BaseAnAction() {

    override fun processAction(
        event: AnActionEvent,
        actionData: ActionData,
        dartClass: DartClassDefinition
    ): PerformAction? {
        val project = actionData.project
        val declarations = selectFieldsWithDialog(project, dartClass)
            ?: return null

        val generationData = GenerationData(actionData, dartClass, declarations)

        val processActions = listOf(
            NamedArgumentConstructorAction,
            IsEmptyAction,
            DartCopyWithAction,
            MapAction,
            JsonAction,
            MyEqualsAction,
            MyHashCodeAction,
            MyToStringAction
        ).map { it.processAction(generationData) }

        return listOf(
            // 开始注释
            //processCommentBeginAction(dartClass, templateManager),
            *processActions.toTypedArray()
            //processCommentEndAction(templateManager)
        )
            .combineAll()

        // transactionguard
    }

    companion object {

        private fun processCommentBeginAction(
            dartClass: DartClassDefinition,
            templateManager: TemplateManager
        ): PerformAction {
            return PerformAction(
                createDeleteCall(dartClass),
                templateManager.createCommentBeginTemplate()
            )
        }

        private fun processCommentEndAction(
            templateManager: TemplateManager
        ): PerformAction {
            return PerformAction(
                // The begin has already deleted the items
                null,
                templateManager.createCommentEndTemplate()
            )
        }

        private fun createDeleteCall(dartClass: DartClassDefinition): (() -> Unit)? {
            val comments = extractSectionComments(
                dartClass,
                createCommentBeginContent(),
                createCommentEndContent()
            )

            if (comments.isEmpty()) {
                return null
            }

            return { comments.forEach { it.delete() } }
        }

        /**
         * Tries to extract the matching comments and in case of missing comments it returns empty
         */
        private fun extractSectionComments(
            dartClass: DartClassDefinition,
            commentBeginContent: String,
            commentEndContent: String
        ): List<PsiComment> {

            // Breadth does not give the correct order - the end comment might be higher level in the hierarchy
            val comments = dartClass.findChildrenByType<PsiComment>(traversalType = TraversalType.Depth)
                // Top level only
                .filter { it.isTopClassLevel }
                .toList()

            val beginCommentIndex = comments.indexOfFirst { it.commentContent == commentBeginContent }

            val beginComment = comments.getOrNull(beginCommentIndex) ?: return emptyList()

            // End comment will be searched after the first one (the retrieval of these children are in order)
            val endComment = comments
                .subList(beginCommentIndex, comments.size)
                .firstOrNull { it.commentContent == commentEndContent }
                ?: return emptyList()

            return listOf(beginComment, endComment)
        }

        private fun TemplateManager.createCommentBeginTemplate(): Template {
            return createDartTemplate(TemplateType.Comment)
                .apply {
                    addNewLine()
                    addSpace()
                    addSpace()
                    addTextSegment("///-----")
                    addTextSegment(createCommentBeginContent())
                    addTextSegment("-------")
                    addNewLine()
                }
        }

        private fun TemplateManager.createCommentEndTemplate(): Template {
            return createDartTemplate(TemplateType.Comment)
                .apply {
                    addNewLine()
                    addSpace()
                    addSpace()
                    addTextSegment("///-----")
                    addTextSegment(createCommentEndContent())
                    addTextSegment("-------")
                    addNewLine()
                }
        }

        private fun createCommentBeginContent(): String =
            "<editor-fold desc=\"${TemplateConstants.EDITOR_FOLD_DATA_CLASS_NAME}\">"

        private fun createCommentEndContent(): String = "</editor-fold>"

    }
}