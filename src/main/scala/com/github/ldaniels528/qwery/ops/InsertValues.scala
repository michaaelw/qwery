package com.github.ldaniels528.qwery.ops

import com.github.ldaniels528.qwery.ops.ResultSet

import scala.collection.Iterable

/**
  * Represents a collection of insert values
  * @author lawrence.daniels@gmail.com
  */
case class InsertValues(fields: Seq[Field], dataSets: Iterable[Seq[Expression]]) extends Executable {

  override def execute(scope: Scope): ResultSet = {
    dataSets map { dataSet =>
      fields zip dataSet map { case (field, value) =>
        field.name -> value.evaluate(scope)
          .getOrElse(throw new RuntimeException(s"Could not resolve value for '${field.name}': $value"))
      }
    }
  }

  override def toSQL: String = {
    dataSets.map(dataSet => s"VALUES (${dataSet.map(_.toSQL).mkString(", ")})").mkString(" ")
  }

}