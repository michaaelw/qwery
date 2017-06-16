package com.github.ldaniels528.qwery.ops.sql

import com.github.ldaniels528.qwery.ops.{Condition, Executable, Field, ResultSet, Scope}
import com.github.ldaniels528.qwery.sources.DataResource
import com.github.ldaniels528.qwery.util.ResourceHelper._

/**
  * Represents an UPSERT statement
  * @author lawrence.daniels@gmail.com
  */
case class Upsert(target: DataResource, fields: Seq[Field], source: Executable, condition: Condition) extends Executable {

  override def execute(scope: Scope): ResultSet = {
    var count = 0L
    val outputSource = target.getOutputSource(scope)
      .getOrElse(throw new IllegalStateException(s"No output source found for '${target.path}'"))
    outputSource.open(scope)
    outputSource use { device =>
      source.execute(scope) foreach { row =>
        device.write(fields zip row map { case (field, (_, value)) =>
          field.name -> value
        })
        count += 1
      }
    }
    ResultSet.inserted(count, statistics = outputSource.getStatistics)
  }

}