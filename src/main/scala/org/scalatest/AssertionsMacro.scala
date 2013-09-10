/*
 * Copyright 2001-2012 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest

import reflect.macros.Context
import collection.mutable.ListBuffer
import collection.immutable.TreeMap
import reflect.internal.util.{Position, OffsetPosition, RangePosition}

class AssertionsMacro[C <: Context](val context: C) {
  import context.universe._

  def apply(booleanExpr: Expr[Boolean]): Expr[Unit] =
    context.Expr(
      Block(
        valDef("$org_scalatest_assert_macro_expr", transformAst(booleanExpr.tree)),
        macroAssert
      )
    )

  def apply(booleanExpr: Expr[Boolean], clueExpr: Expr[Any]): Expr[Unit] =
    context.Expr(
      Block(
        valDef("$org_scalatest_assert_macro_expr", transformAst(booleanExpr.tree)),
        macroAssertWithClue(clueExpr.tree)
      )
    )

  private def valDef(name: String, rhs: Tree): ValDef =
    ValDef(
      Modifiers(),
      newTermName(name),
      TypeTree(),
      rhs
    )

  private def binaryMacroExpression(select: Select, expressionText: String): Apply = {
    val macroExpressionClass = context.mirror.staticClass(classOf[BinaryMacroExpression].getName)
    Apply(
      Select(
        New(Ident(macroExpressionClass)),
        newTermName("<init>")
      ),
      List(
        Ident(newTermName("$org_scalatest_assert_macro_left")),
        context.literal(select.name.decoded).tree,
        Ident(newTermName("$org_scalatest_assert_macro_right")),
        Apply(
          Select(
            Ident(newTermName("$org_scalatest_assert_macro_left")),
            select.name
          ),
          List(Ident(newTermName("$org_scalatest_assert_macro_right")))
        ),
        context.literal(expressionText).tree
      )
    )
  }

  private def binaryMacroExpression(select: Select, expressionText: String, secondArg: Tree): Apply = {
    val macroExpressionClass = context.mirror.staticClass(classOf[BinaryMacroExpression].getName)
    Apply(
      Select(
        New(Ident(macroExpressionClass)),
        newTermName("<init>")
      ),
      List(
        Ident(newTermName("$org_scalatest_assert_macro_left")),
        context.literal(select.name.decoded).tree,
        Ident(newTermName("$org_scalatest_assert_macro_right")),
        Apply(
          Apply(
            Select(
              Ident("$org_scalatest_assert_macro_left"),
              select.name
            ),
            List(Ident("$org_scalatest_assert_macro_right"))
          ),
          List(secondArg)
        ),
        context.literal(expressionText).tree
      )
    )
  }

  private def simpleMacroExpression(expression: Tree, expressionText: String): Apply = {
    val macroExpressionClass = context.mirror.staticClass(classOf[SimpleMacroExpression].getName)
    Apply(
      Select(
        New(Ident(macroExpressionClass)),
        newTermName("<init>")
      ),
      List(
        expression,
        context.literal(expressionText).tree
      )
    )
  }

  /*def notMacroExpression(target: Tree, expressionText: String): Apply = {
    val macroExpressionClass = context.mirror.staticClass(classOf[NotMacroExpression].getName)
    Apply(
      Select(
        New(Ident(macroExpressionClass)),
        newTermName("<init>")
      ),
      List(
        target.duplicate,
        context.literal("!").tree,
        context.literal(expressionText).tree
      )
    )
  }*/

  private def macroAssert: Apply =
    Apply(
      Select(
        Ident(newTermName("$org_scalatest_AssertionsHelper")),
        newTermName("macroAssert")
      ),
      List(Ident(newTermName("$org_scalatest_assert_macro_expr")))
    )

  private def macroAssertWithClue(clueTree: Tree): Apply =
    Apply(
      Select(
        Ident(newTermName("$org_scalatest_AssertionsHelper")),
        newTermName("macroAssert")
      ),
      List(Ident(newTermName("$org_scalatest_assert_macro_expr")), clueTree.duplicate)
    )

  //val logicOperators = Set("&&", "||", "&", "|")

  private def traverseSelect(select: Select, rightExpr: Tree): (Tree, Tree) =
    /*if (logicOperators.contains(select.name.decoded)) {
      val leftTree =
        select.qualifier match {
          case selectApply: Apply => transformAst(selectApply.duplicate)
          case selectSelect: Select => transformAst(selectSelect.duplicate)
          case _ => select.qualifier.duplicate
        }
      val rightTree =
        rightExpr match {
          case argApply: Apply => transformAst(argApply.duplicate)
          case argSelect: Select => transformAst(argSelect.duplicate)
          case _ => rightExpr.duplicate
        }
      (leftTree, rightTree)
    }
    else*/
      (select.qualifier.duplicate, rightExpr.duplicate)

  private def transformAst(tree: Tree): Tree =
    tree match {
      case apply: Apply if apply.args.size == 1 =>
        apply.fun match {
          case select: Select =>
            val (leftTree, rightTree) =  traverseSelect(select, apply.args(0))
            Block(
              valDef("$org_scalatest_assert_macro_left", leftTree),
              valDef("$org_scalatest_assert_macro_right", rightTree),
              binaryMacroExpression(select.duplicate, getText(tree))
            )
          case funApply: Apply if funApply.args.size == 1 => // For === and !== that takes Equality
            funApply.fun match {
              case select: Select if select.name.decoded == "===" || select.name.decoded == "!==" =>
                val (leftTree, rightTree) = traverseSelect(select, funApply.args(0))
                Block(
                  valDef("$org_scalatest_assert_macro_left", leftTree),
                  valDef("$org_scalatest_assert_macro_right", rightTree),
                  binaryMacroExpression(select.duplicate, getText(tree), apply.args(0).duplicate) // TODO: apply.args(0) should be traverse also
                )
              case _ => simpleMacroExpression(tree.duplicate, getText(tree))
            }
          case _ => simpleMacroExpression(tree.duplicate, getText(tree))
        }
      /*case select: Select if select.name.decoded == "unary_!" => // for !
        val leftTree =
          select.qualifier match {
            case selectApply: Apply => transformAst(selectApply.duplicate)
            case selectSelect: Select => transformAst(selectSelect.duplicate)
            case _ => simpleMacroExpression(select.qualifier.duplicate, getText(select.qualifier))
          }
        notMacroExpression(leftTree.duplicate, getText(tree))*/
      case _ => simpleMacroExpression(tree.duplicate, getText(tree))
    }

  private def getPosition(expr: Tree) = expr.pos.asInstanceOf[scala.reflect.internal.util.Position]

  private def getText(expr: Tree): String = getPosition(expr) match {
    case p: RangePosition => context.echo(expr.pos, "RangePosition found!"); p.lineContent.slice(p.start, p.end).trim
    case p: reflect.internal.util.Position => p.lineContent.trim
  }
}

object AssertionsMacro {
  def apply(context: Context)(condition: context.Expr[Boolean]): context.Expr[Unit] = {
    new AssertionsMacro[context.type](context).apply(condition)
  }
  def applyWithClue(context: Context)(condition: context.Expr[Boolean], clue: context.Expr[Any]): context.Expr[Unit] = {
    new AssertionsMacro[context.type](context).apply(condition, clue)
  }
}