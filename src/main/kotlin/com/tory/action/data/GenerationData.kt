package com.tory.action.data

import com.tory.action.init.ActionData
import com.tory.declaration.VariableDeclaration
import com.jetbrains.lang.dart.psi.DartClassDefinition

data class GenerationData(
    val actionData: ActionData,
    val dartClass: DartClassDefinition,
    val declarations: List<VariableDeclaration>
)