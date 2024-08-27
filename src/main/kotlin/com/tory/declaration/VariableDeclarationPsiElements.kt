package com.tory.declaration

import com.tory.DartFileNotWellFormattedException
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.lang.dart.psi.*
import com.tory.ext.psi.findFirstChildByType
import com.tory.templater.ParamDartType
import com.tory.templater.VariableTemplateParam

// DartVarAccessDeclaration can not be null
// DartType can be null
interface VariableDeclarationPsiElements {
    val modifiers: List<LeafPsiElement>

    // DartType is null if initialized and has a final or const modifier
    val dartType: DartType?
    val name: DartComponentName
    val initializer: DartVarInit?
}

class VariableDeclarationPsiElementsImpl(
    override val modifiers: List<LeafPsiElement>,
    override val dartType: DartType?,
    override val name: DartComponentName,
    override val initializer: DartVarInit?
) : VariableDeclarationPsiElements

interface PublicNamedVariable {
    // The name of the variable that can be used for constructor parameter
    // (unique among all parameter names and has no starting underscore sign)
    val publicVariableName: String
}

class PublicNamedVariableImpl(
    override val publicVariableName: String
) : PublicNamedVariable

// Class constructor will only be used from the 2 given types
class VariableDeclaration(
    psiElements: VariableDeclarationPsiElements,
    publicNamedVariable: PublicNamedVariable
) : VariableDeclarationPsiElements by psiElements,
    PublicNamedVariable by publicNamedVariable

fun VariableDeclarationPsiElements.hasModifier(modifier: DeclarationModifier): Boolean {
    return modifiers.find { it.text == modifier.text } !== null
}

val VariableDeclarationPsiElements.fullTypeName: String?
    get() = dartType?.text?.substringBeforeLast("?")

val VariableDeclarationPsiElements.hasInitializer: Boolean
    get() = initializer !== null

val VariableDeclarationPsiElements.variableName: String
    get() = name.name ?: throw DartFileNotWellFormattedException("Encountered a variable which does not have a name.")

/**
 * 获取成员变量的dartClass 类型
 * DartEnumDefinitionImpl  枚举
 * DartClassDefinitionImpl 普通类
 */
val DartType.dartClass: DartClass?
    get() {
        val target = referenceExpression?.resolve()?.parent
        return if (target is DartClass) target else null
    }

/// 是否为枚举
val DartType.isEnum: Boolean
    get() = dartClass?.isEnum == true

/**
 * 是否是枚举
 */
val VariableDeclarationPsiElements.isEnum: Boolean
    get() = dartType?.dartClass?.isEnum == true

/// 枚举值
val DartType.enumVariableList: List<String>
    get() = dartClass?.enumConstantDeclarationList?.map { it.componentName?.name ?: "" } ?: emptyList()

/**
 * 枚举列表
 */
val VariableDeclarationPsiElements.enumVariableList: List<String>
    get() = dartType?.dartClass?.enumConstantDeclarationList?.map { it.componentName?.name ?: "" } ?: emptyList()

/**
 * 注解相关
 */
val VariableDeclarationPsiElements.metadataList: List<DartMetadata>
    get() {
        val declaration = name.parent
        return if (declaration is DartVarAccessDeclaration) declaration.metadataList else emptyList()
    }

/**
 * 注解类型列表
 */
val VariableDeclarationPsiElements.metadataDartTypeList: List<String>
    get() {
        return metadataList.map { it.dartTypeName }.filterNotNull()
    }

/**
 * 变量是否应该被忽略
 */
val VariableDeclarationPsiElements.isJsonIgnore: Boolean
    get() = metadataDartTypeList.contains("JsonIgnore")


/// 注解类型
val DartMetadata.dartTypeName: String?
    get() = children.filterIsInstance<DartReferenceExpression>().firstOrNull()?.text

/**
 * 注解的参数
 */
val DartMetadata.variableList: List<String>
    get() {
        val argumentList = children.filterIsInstance<DartArguments>().firstOrNull()?.argumentList ?: return emptyList()
        return argumentList.expressionList.map { it.text } + argumentList.namedArgumentList.map { it.expression.text }
    }

/**
 * 获取注解JsonKey上的自定义key
 */
val VariableDeclarationPsiElements.customJsonKey: String?
    get() {
        val meta = metadataList.firstOrNull { it.dartTypeName == "JsonKey" }
        return meta?.variableList?.firstOrNull()?.trim('\'', '\"')
    }

fun isVariableNamePrivate(variableName: String): Boolean =
    variableName.startsWith("_")

val VariableDeclarationPsiElements.isPrivate: Boolean
    get() = isVariableNamePrivate(variableName)

val VariableDeclarationPsiElements.isPublic: Boolean
    get() = !isPrivate

val VariableDeclarationPsiElements.isStatic: Boolean
    get() = hasModifier(DeclarationModifier.Static)

val VariableDeclarationPsiElements.isMember: Boolean
    get() = !isStatic

val VariableDeclarationPsiElements.isFinal: Boolean
    get() = hasModifier(DeclarationModifier.Final)

val VariableDeclarationPsiElements.isNullable: Boolean
    get() = dartType?.text?.endsWith("?") ?: false

val VariableDeclarationPsiElements.canBeAssignedFromConstructor: Boolean
    get() {
        val isStatic = hasModifier(DeclarationModifier.Static)
        // Static or static const makes no difference
        if (isStatic) {
            return false
        }

        val isFinal = hasModifier(DeclarationModifier.Final)
        if (isFinal && hasInitializer) {
            return false
        }

        return true
    }

fun Iterable<VariableDeclarationPsiElements>.allMembersFinal(): Boolean {
    return this.asSequence()
        .filter { it.isMember }
        .all { it.isFinal }
}

fun DartType.resolveDartType(): ParamDartType {
    return ParamDartType(
        fullTypeName = text?.substringBeforeLast("?") ?: "",
        typeName = referenceExpression?.text ?: "",
        isNullable = text?.endsWith("?") ?: false,
        argumentTypeList = typeArguments?.typeList?.typeList?.map { it.resolveDartType() } ?: emptyList(),
        isEnum = isEnum,
        enumVariableList = enumVariableList,
    )
}

/**
 * 转换成需要使用的 VariableTemplateParam 类
 */
fun VariableDeclaration.toVariableTemplateParam(): VariableTemplateParam {
    return VariableTemplateParam(
        variableName = variableName,
        type = fullTypeName
            ?: throw RuntimeException("No type is available - this variable should not be assignable from constructor"),
        publicVariableName = publicVariableName,
        isNullable = isNullable,
        jsonKey = customJsonKey,
        isEnum = isEnum,
        enumVariableList = enumVariableList,
        dartType = dartType?.resolveDartType()
            ?: throw RuntimeException("No type is available - this variable should not be assignable from constructor"),
    )
}
