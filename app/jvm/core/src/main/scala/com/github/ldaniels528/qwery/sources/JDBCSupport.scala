package com.github.ldaniels528.qwery.sources

import java.sql.{Connection, DriverManager}
import java.util.{Properties => JProperties}

import com.github.ldaniels528.qwery.ops.{Hints, Scope}

import scala.util.Try

/**
  * JDBC Support
  * @author lawrence.daniels@gmail.com
  */
trait JDBCSupport {

  /**
    * Creates a database connection
    * @param scope the given [[Scope scope]]
    * @param url   the given JDBC URL
    * @param hints the given [[Hints hints]]
    * @return the [[Connection connection]]
    */
  def createConnection(scope: Scope, url: String, hints: Option[Hints]) = Try {
    // load the driver
    for {
      hints <- hints
      jdbcDriver <- hints.jdbcDriver
    } Class.forName(jdbcDriver).newInstance()

    // pass any defined properties
    val properties = hints.flatMap(_.properties) getOrElse new JProperties()

    // open the connection
    DriverManager.getConnection(url, properties)
  }

  /**
    * Returns the column names based on the result set meta data
    * @param rs the given [[java.sql.ResultSet result set]]
    * @return the collection of column names
    */
  def getColumnNames(rs: java.sql.ResultSet): Seq[String] = {
    val metaData = rs.getMetaData
    val count = metaData.getColumnCount
    for (n <- 1 to count) yield metaData.getColumnName(n)
  }

  /**
    * Returns the JDBCOutputSource
    * @param target the given [[DataResource target]]
    * @param scope  the given [[Scope scope]]
    * @return the [[JDBCOutputSource]]
    */
  def getJDBCOutputSource(target: DataResource, scope: Scope): JDBCOutputSource = {
    target.getOutputSource(scope) match {
      case Some(source: JDBCOutputSource) => source
      case Some(_) =>
        throw new IllegalArgumentException("Only JDBC Output Sources support UPDATE")
      case None =>
        throw new IllegalStateException(s"No output source found for '${target.path}'")
    }
  }

}
