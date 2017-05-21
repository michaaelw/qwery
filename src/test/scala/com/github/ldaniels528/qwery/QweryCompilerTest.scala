package com.github.ldaniels528.qwery

import com.github.ldaniels528.qwery.ops.Implicits._
import com.github.ldaniels528.qwery.ops._
import com.github.ldaniels528.qwery.ops.builtins.Case.When
import com.github.ldaniels528.qwery.ops.builtins.{Case, Cast, Concat}
import com.github.ldaniels528.qwery.sources._
import org.scalatest.FunSpec

/**
  * Qwery Compiler Test
  * @author lawrence.daniels@gmail.com
  */
class QweryCompilerTest extends FunSpec {

  describe("QweryCompiler") {

    it("should support creating named aliases") {
      val sql = "SELECT 1234 AS number"
      assert(QweryCompiler(sql) ==
        Select(fields = List(NamedExpression(name = "number", 1234))))
    }

    it("should CAST values from one type to another") {
      val sql = "SELECT CAST('1234' AS Double) AS number"
      assert(QweryCompiler(sql) ==
        Select(fields = List(NamedExpression(name = "number", Cast("1234", "Double")))))
    }

    it("should support CASE statement") {
      val sql =
        """
          |SELECT
          |   CASE 'Hello World'
          |     WHEN 'HelloWorld' THEN '1'
          |     WHEN 'Hello' || ' ' || 'World' THEN '2'
          |     ELSE '3'
          |   END AS ItemNo
        """.stripMargin
      assert(QweryCompiler(sql) ==
        Select(fields = List(
          NamedExpression("ItemNo", Case(conditions = List(
            When(EQ("Hello World", "HelloWorld"), "1"),
            When(EQ("Hello World", Concat(Concat("Hello", " "), "World")), "2")),
            otherwise = Some("3"))))
        ))
    }

    it("should compile SELECT queries") {
      val sql =
        """
          |SELECT Symbol, Name, Sector, Industry, `Summary Quote`
          |FROM 'companylist.csv'
          |WHERE Industry = 'Oil/Gas Transmission'""".stripMargin
      assert(QweryCompiler(sql) ==
        Select(
          fields = List("Symbol", "Name", "Sector", "Industry", "Summary Quote").map(Field.apply),
          source = Option(DataResource("companylist.csv")),
          condition = Some(EQ(Field("Industry"), "Oil/Gas Transmission"))
        ))
    }

    it("should compile SELECT queries for all (*) fields") {
      val sql =
        """
          |SELECT * FROM 'companylist.csv'
          |WHERE Industry = 'Oil/Gas Transmission'""".stripMargin
      assert(QweryCompiler(sql) ==
        Select(
          fields = List(AllFields),
          source = Option(DataResource("companylist.csv")),
          condition = Some(EQ(Field("Industry"), "Oil/Gas Transmission"))
        ))
    }

    it("should compile SELECT queries with ORDER BY clauses") {
      val sql =
        """
          |SELECT * FROM 'companylist.csv'
          |WHERE Industry = 'Oil/Gas Transmission'
          |ORDER BY Symbol ASC""".stripMargin
      assert(QweryCompiler(sql) ==
        Select(
          fields = List(AllFields),
          source = Option(DataResource("companylist.csv")),
          condition = Some(EQ(Field("Industry"), "Oil/Gas Transmission")),
          orderedColumns = List(OrderedColumn("Symbol", ascending = true))
        ))
    }

    it("should compile SELECT queries with GROUP BY and ORDER BY clauses") {
      val sql =
        """
          |SELECT Symbol, Name, Sector, Industry, `Summary Quote` FROM 'companylist.csv'
          |WHERE Industry = 'Oil/Gas Transmission'
          |GROUP BY Symbol
          |ORDER BY Symbol DESC""".stripMargin
      assert(QweryCompiler(sql) ==
        Select(
          fields = List("Symbol", "Name", "Sector", "Industry", "Summary Quote").map(Field.apply),
          source = Option(DataResource("companylist.csv")),
          condition = Some(EQ(Field("Industry"), "Oil/Gas Transmission")),
          groupFields = List(Field("Symbol")),
          orderedColumns = List(OrderedColumn("Symbol", ascending = false))
        ))
    }

    it("should compile INSERT-SELECT statements") {
      val sql =
        """
          |INSERT OVERWRITE 'test2.csv' (Symbol, Sector, Industry, LastSale)
          |SELECT Symbol, Sector, Industry, LastSale FROM 'companylist.csv'
          |WHERE Industry = 'Precious Metals'""".stripMargin
      assert(QweryCompiler(sql) ==
        Insert(
          fields = List("Symbol", "Sector", "Industry", "LastSale").map(Field.apply),
          target = DataResource("test2.csv"),
          append = false,
          source = Select(
            fields = List("Symbol", "Sector", "Industry", "LastSale").map(Field.apply),
            source = Option(DataResource("companylist.csv")),
            condition = Some(EQ(Field("Industry"), "Precious Metals")),
            limit = None),
          hints = Hints()))
    }

    it("should compile INSERT statements") {
      val sql =
        """
          |INSERT INTO 'test3.csv' (Symbol, Sector, Industry, LastSale)
          |VALUES ('ACU', 'Capital Goods', 'Industrial Machinery/Components', 29)
          |VALUES ('EMX', 'Basic Industries', 'Precious Metals', 0.828)""".stripMargin
      assert(QweryCompiler(sql) ==
        Insert(
          fields = List("Symbol", "Sector", "Industry", "LastSale").map(Field.apply),
          target = DataResource("test3.csv"),
          append = true,
          source = InsertValues(
            fields = List("Symbol", "Sector", "Industry", "LastSale").map(Field.apply),
            dataSets = List(
              List[Expression]("ACU", "Capital Goods", "Industrial Machinery/Components", 29.0),
              List[Expression]("EMX", "Basic Industries", "Precious Metals", 0.828)
            )),
          hints = Hints()
        ))
    }

  }

}
