/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.table.planner.plan.rules.logical

import org.apache.flink.table.api._
import org.apache.flink.table.planner.utils.{BatchTableTestUtil, TableTestBase}

import org.junit.jupiter.api.{BeforeEach, Test}

/** Base test class for [[PruneAggregateCallRule]]. */
abstract class PruneAggregateCallRuleTestBase extends TableTestBase {
  protected val util: BatchTableTestUtil = batchTestUtil()

  @BeforeEach
  def setup(): Unit = {
    util.tableEnv.executeSql(s"""
                                |CREATE TABLE T1 (
                                |  a1 INT PRIMARY KEY NOT ENFORCED,
                                |  b1 INT,
                                |  c1 STRING,
                                |  d1 BIGINT
                                |) WITH (
                                |  'connector' = 'values',
                                |  'bounded' = 'true'
                                |)
                                |""".stripMargin)
    util.addTableSource[(Int, Int, String, Long)]("T2", 'a2, 'b2, 'c2, 'd2)
  }

  @Test
  def testPruneRegularAggCall_WithoutFilter1(): Unit = {
    val sql =
      """
        |SELECT a2, b2, d2 FROM
        | (SELECT a2, b2, COUNT(c2) as c2, SUM(d2) as d2 FROM T2 GROUP BY a2, b2) t
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testPruneRegularAggCall_WithoutFilter2(): Unit = {
    val sql =
      """
        |SELECT b2, a2, d2 FROM
        | (SELECT a2, b2, COUNT(c2) as c2, SUM(d2) as d2 FROM T2 GROUP BY a2, b2) t
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testPruneRegularAggCall_WithoutFilter3(): Unit = {
    val sql =
      """
        |SELECT a2 as a, b2, d2 FROM
        | (SELECT a2, b2, COUNT(c2) as c2, SUM(d2) as d2 FROM T2 GROUP BY a2, b2) t
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testPruneRegularAggCall_WithFilter1(): Unit = {
    val sql =
      """
        |SELECT a2, b2, d2 FROM
        | (SELECT a2, b2, COUNT(c2) as c2, SUM(d2) as d2 FROM T2 GROUP BY a2, b2) t
        |WHERE d2 > 0
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testPruneRegularAggCall_WithFilter2(): Unit = {
    val sql =
      """
        |SELECT b2, a2, d2 FROM
        | (SELECT a2, b2, COUNT(c2) as c2, SUM(d2) as d2 FROM T2 GROUP BY a2, b2) t
        |WHERE d2 > 0
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testPruneAuxGroupAggCall_WithoutFilter1(): Unit = {
    val sql =
      """
        |SELECT a1, c1 FROM
        | (SELECT a1, b1, COUNT(c1) as c1, SUM(d1) as d1 FROM T1 GROUP BY a1, b1) t
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testPruneAuxGroupAggCall_WithoutFilter2(): Unit = {
    val sql =
      """
        |SELECT c1, a1 FROM
        | (SELECT a1, b1, COUNT(c1) as c1, SUM(d1) as d1 FROM T1 GROUP BY a1, b1) t
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testPruneAuxGroupAggCall_WithFilter1(): Unit = {
    val sql =
      """
        |SELECT a1, c1 FROM
        | (SELECT a1, b1, COUNT(c1) as c1, SUM(d1) as d1 FROM T1 GROUP BY a1, b1) t
        |WHERE c1 > 10
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testPruneAuxGroupAggCall_WithFilter2(): Unit = {
    val sql =
      """
        |SELECT c1, a1 FROM
        | (SELECT a1, b1, COUNT(c1) as c1, SUM(d1) as d1 FROM T1 GROUP BY a1, b1) t
        |WHERE c1 > 10
      """.stripMargin
    util.verifyRelPlan(sql)
  }

  @Test
  def testEmptyGroupKey_WithOneAggCall1(): Unit = {
    val sql = "SELECT 1 FROM (SELECT SUM(a1) FROM T1) t"
    util.verifyRelPlan(sql)
  }

  @Test
  def testEmptyGroupKey_WithOneAggCall2(): Unit = {
    val sql = "SELECT * FROM T2 WHERE EXISTS (SELECT COUNT(*) FROM T1)"
    util.verifyRelPlan(sql)
  }

  @Test
  def testEmptyGroupKey_WithOneAggCall3(): Unit = {
    val sql = "SELECT * FROM T2 WHERE EXISTS (SELECT COUNT(*) FROM T1 WHERE 1=2)"
    util.verifyRelPlan(sql)
  }

  @Test
  def testEmptyGroupKey_WithMoreThanOneAggCalls1(): Unit = {
    val sql = "SELECT 1 FROM (SELECT SUM(a1), COUNT(*) FROM T1) t"
    util.verifyRelPlan(sql)
  }

  @Test
  def testEmptyGroupKey_WithMoreThanOneAggCalls2(): Unit = {
    val sql = "SELECT * FROM T2 WHERE EXISTS (SELECT SUM(a1), COUNT(*) FROM T1)"
    util.verifyRelPlan(sql)
  }

  @Test
  def testEmptyGroupKey_WithMoreThanOneAggCalls3(): Unit = {
    val sql = "SELECT * FROM T2 WHERE EXISTS (SELECT SUM(a1), COUNT(*) FROM T1 WHERE 1=2)"
    util.verifyRelPlan(sql)
  }

}
