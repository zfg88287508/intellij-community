// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy.lang.resolve.processors

import com.intellij.psi.PsiElement
import com.intellij.psi.scope.ElementClassHint
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult
import org.jetbrains.plugins.groovy.lang.resolve.GrResolverProcessor
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil.filterSameSignatureCandidates
import org.jetbrains.plugins.groovy.lang.resolve.singleOrValid

abstract class AccessorAwareResolverProcessor(
  name: String,
  place: PsiElement,
  kinds: Set<GroovyResolveKind>
) : KindsResolverProcessor(name, place, kinds),
    GrResolverProcessor<GroovyResolveResult>,
    ElementClassHint,
    DynamicMembersHint,
    MultiProcessor {

  init {
    @Suppress("LeakingThis") hint(ElementClassHint.KEY, this)
    @Suppress("LeakingThis") hint(DynamicMembersHint.KEY, this)
  }

  final override fun shouldProcessProperties(): Boolean = true

  final override fun shouldProcessMethods(): Boolean = false

  final override fun shouldProcess(kind: ElementClassHint.DeclarationKind): Boolean {
    return kind != ElementClassHint.DeclarationKind.METHOD && kinds.any { kind in it.declarationKinds }
  }

  final override fun getProcessors(): Collection<PsiScopeProcessor> = listOf(this) + accessorProcessors

  protected abstract val accessorProcessors: Collection<GrResolverProcessor<*>>

  private val accessorCandidates get() = accessorProcessors.flatMap { it.results }

  private fun getCandidates(kind: GroovyResolveKind): List<GroovyResolveResult> {
    val result = getCandidate(kind)?.let(::listOf) ?: emptyList()
    return if (kind == GroovyResolveKind.PROPERTY) result + accessorCandidates else result
  }

  final override val results: List<GroovyResolveResult>
    get() {
      val variables = getCandidates(GroovyResolveKind.VARIABLE)
      if (variables.isNotEmpty()) {
        return variables
      }

      val properties = singleOrValid(getCandidates(GroovyResolveKind.PROPERTY))
      if (!properties.isEmpty()) {
        val property = properties.singleOrNull()
        if (property != null) {
          if (property.isStaticsOK) {
            return properties
          }
        }
        else {
          return ContainerUtil.newSmartList(properties[0])
        }
      }

      val fields = getCandidates(GroovyResolveKind.FIELD)
      if (!fields.isEmpty()) {
        return fields
      }

      if (properties.isNotEmpty()) {
        return properties
      }

      val bindings = getCandidates(GroovyResolveKind.BINDING)
      if (bindings.isNotEmpty()) {
        return bindings
      }

      // TODO this is used to choose between two same methods from some class and its superclass, which is questionable
      return getAllCandidates() + filterSameSignatureCandidates(accessorCandidates)
    }
}
