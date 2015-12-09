package com.avsystem.commons
package macros

/**
  * Author: ghik
  * Created: 04/12/15.
  */
trait TypeClassDerivation extends MacroCommons {

  import c.universe._

  val DeferredInstanceCls = tq"$CommonsPackage.macros.DeferredInstance"

  def typeClass(tpe: Type): Type
  def implementDeferredInstance(tpe: Type): Tree

  def forSingleton(tpe: Type, singleValueTree: Tree): Tree
  def forApplyUnapply(tpe: Type, companion: Symbol, paramsWithInstances: List[(Symbol, Tree)]): Tree
  def forSealedHierarchy(tpe: Type, subtypesWithInstances: List[(Type, Tree)]): Tree
  def forUnknown(tpe: Type): Tree

  def autoDeriveFor(tpe: Type): Tree = {
    val tcTpe = typeClass(tpe)

    def singleTypeTc = singleValueFor(tpe).map(tree => forSingleton(tpe, tree))
    def applyUnapplyTc = applyUnapplyFor(tpe).map {
      case ApplyUnapply(companion, params) =>
        val dependencies = params.map { s =>
          (s, implicitValue(typeClass(s.typeSignature)))
        }
        forApplyUnapply(tpe, companion, dependencies)
    }
    def sealedHierarchyTc = knownSubtypes(tpe).map { subtypes =>
      val dependencies = subtypes.map { depTpe =>
        val depTc = typeClass(depTpe)
        val depTree = c.inferImplicitValue(depTc) match {
          case EmptyTree => autoDeriveFor(depTpe)
          case tree => tree
        }
        (depTpe, depTree)
      }
      forSealedHierarchy(tpe, dependencies)
    }
    val result = singleTypeTc orElse applyUnapplyTc orElse sealedHierarchyTc getOrElse forUnknown(tpe)

    val deferredName = c.freshName(TermName("deferred"))
    lazy val Block(List(deferredVal), deferredIdent) =
      c.typecheck(q"val $deferredName = ${implementDeferredInstance(tpe)}; $deferredName")

    object transformer extends Transformer {
      var changed = false
      override def transform(tree: Tree): Tree = {
        if(tree.symbol != null && tree.tpe != null) {
          if (!tree.isDef && tree.symbol.isImplicit && tree.tpe.widen =:= tcTpe) {
            changed = true
            deferredIdent.duplicate
          } else super.transform(tree)
        } else super.transform(tree)
      }
    }
    val res = Some(transformer.transform(result)).filter(_ => transformer.changed).map { guardedResult =>
      q"""
         $deferredVal
         ${deferredIdent.duplicate}.underlying = $guardedResult
         ${deferredIdent.duplicate}.underlying
         """
    }.getOrElse(result)

    c.echo(c.enclosingPosition, show(res))
    res
  }

  def autoDerive[T: c.WeakTypeTag]: Tree =
    autoDeriveFor(weakTypeOf[T])
}
