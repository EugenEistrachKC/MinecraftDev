/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.buildsystem.SourceType
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.GenerationData
import com.demonwav.mcdev.inspection.IsCancelled
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.inspections.sideonly.SidedProxyAnnotator
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.extendsOrImplements
import com.demonwav.mcdev.util.nullable
import com.demonwav.mcdev.util.runWriteTaskLater
import com.demonwav.mcdev.util.waitForAllSmart
import com.intellij.json.JsonFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.annotations.Contract

class ForgeModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    var mcmod by nullable { facet.findFile(ForgeConstants.MCMOD_INFO, SourceType.RESOURCE) }
        private set

    override val moduleType = ForgeModuleType
    override val type = PlatformType.FORGE
    override val icon = PlatformAssets.FORGE_ICON

    override fun init() {
        ApplicationManager.getApplication().executeOnPooledThread {
            waitForAllSmart()
            // Set mcmod.info icon
            runWriteTaskLater {
                FileTypeManager.getInstance().associatePattern(JsonFileType.INSTANCE, ForgeConstants.MCMOD_INFO)
            }

            // Index @SideOnly
            val service = DumbService.getInstance(project)
            service.runReadActionInSmartMode runSmart@ {
                if (service.isDumb || project.isDisposed) {
                    return@runSmart
                }

                val scope = GlobalSearchScope.projectScope(project)
                val sidedProxy = JavaPsiFacade.getInstance(project)
                    .findClass(ForgeConstants.SIDED_PROXY_ANNOTATION, scope) ?: return@runSmart
                val annotatedFields = AnnotatedElementsSearch.searchPsiFields(sidedProxy, scope).findAll()

                var index = 0.0

                for (field in annotatedFields) {
                    if (service.isDumb || project.isDisposed) {
                        return@runSmart
                    }

                    SidedProxyAnnotator.check(field)
                    index++
                }
            }
        }
    }

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?): Boolean {
        if (method == null) {
            return ForgeConstants.FML_EVENT == eventClass.qualifiedName || ForgeConstants.EVENT == eventClass.qualifiedName
        }

        var annotation = method.modifierList.findAnnotation(ForgeConstants.EVENT_HANDLER_ANNOTATION)
        if (annotation != null) {
            return ForgeConstants.FML_EVENT == eventClass.qualifiedName
        }

        annotation = method.modifierList.findAnnotation(ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION)
        if (annotation != null) {
            return ForgeConstants.EVENT == eventClass.qualifiedName
        }

        // just default to true
        return true
    }

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod): String {
        val annotation = method.modifierList.findAnnotation(ForgeConstants.EVENT_HANDLER_ANNOTATION)

        if (annotation != null) {
            return formatWrongEventMessage(ForgeConstants.FML_EVENT, ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION,
                    ForgeConstants.EVENT == eventClass.qualifiedName)
        }

        return formatWrongEventMessage(ForgeConstants.EVENT, ForgeConstants.EVENT_HANDLER_ANNOTATION,
                ForgeConstants.FML_EVENT == eventClass.qualifiedName)
    }

    private fun formatWrongEventMessage(expected: String, suggested: String, wrong: Boolean): String {
        val base = "Parameter is not a subclass of $expected\n"
        if (wrong) {
            return base + "This method should be annotated with $suggested"
        }
        return base + "Compiling and running this listener may result in a runtime exception"
    }

    override fun isStaticListenerSupported(method: PsiMethod) = true

    override fun generateEventListenerMethod(containingClass: PsiClass,
                                             chosenClass: PsiClass,
                                             chosenName: String,
                                             data: GenerationData?): PsiMethod? {
        val isFmlEvent = chosenClass.extendsOrImplements(ForgeConstants.FML_EVENT)

        val method = JavaPsiFacade.getElementFactory(project).createMethod(chosenName, PsiType.VOID)
        val parameterList = method.parameterList

        val qName = chosenClass.qualifiedName ?: return null
        val parameter = JavaPsiFacade.getElementFactory(project)
            .createParameter(
                "event",
                PsiClassType.getTypeByName(qName, project, GlobalSearchScope.allScope(project))
            )

        parameterList.add(parameter)
        val modifierList = method.modifierList

        if (isFmlEvent) {
            modifierList.addAnnotation(ForgeConstants.EVENT_HANDLER_ANNOTATION)
        } else {
            modifierList.addAnnotation(ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION)
        }

        return method
    }

    @Contract(value = "null -> false", pure = true)
    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        if (element !is PsiIdentifier) {
            return false
        }

        if (element.parent !is PsiClass) {
            return false
        }

        val psiClass = element.parent as PsiClass

        val modifierList = psiClass.modifierList
        return modifierList?.findAnnotation(ForgeConstants.MOD_ANNOTATION) != null
    }

    override fun checkUselessCancelCheck(expression: PsiMethodCallExpression): IsCancelled? = null

    override fun dispose() {
        super.dispose()

        mcmod = null
    }
}
