/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.execution.datasources.parquet

import org.apache.spark.sql.{DataFrame, GlutenSQLTestsBaseTrait}

class GlutenParquetColumnIndexSuite extends ParquetColumnIndexSuite with GlutenSQLTestsBaseTrait {
  private val actions: Seq[DataFrame => DataFrame] = Seq(
    "_1 = 500",
    "_1 = 500 or _1 = 1500",
    "_1 = 500 or _1 = 501 or _1 = 1500",
    "_1 = 500 or _1 = 501 or _1 = 1000 or _1 = 1500",
    "_1 >= 500 and _1 < 1000",
    "(_1 >= 500 and _1 < 1000) or (_1 >= 1500 and _1 < 1600)"
  ).map(f => (df: DataFrame) => df.filter(f))

  testGluten("test reading unaligned pages - test all types") {
    val df = spark
      .range(0, 2000)
      .selectExpr(
        "id as _1",
        "cast(id as short) as _3",
        "cast(id as int) as _4",
        "cast(id as float) as _5",
        "cast(id as double) as _6",
        "cast(id as decimal(20,0)) as _7",
        // We changed 1618161925000 to 1618161925 to avoid reaching the limitation of Velox:
        // Timepoint is outside of supported year range.
        "cast(cast(1618161925 + id * 60 * 60 * 24 as timestamp) as date) as _9"
      )
    checkUnalignedPages(df)(actions: _*)
  }

  testGluten("test reading unaligned pages - test all types (dict encode)") {
    val df = spark
      .range(0, 2000)
      .selectExpr(
        "id as _1",
        "cast(id % 10 as byte) as _2",
        "cast(id % 10 as short) as _3",
        "cast(id % 10 as int) as _4",
        "cast(id % 10 as float) as _5",
        "cast(id % 10 as double) as _6",
        "cast(id % 10 as decimal(20,0)) as _7",
        "cast(id % 2 as boolean) as _8",
        "cast(cast(1618161925 + (id % 10) * 60 * 60 * 24 as timestamp) as date) as _9",
        "cast(1618161925 + (id % 10) as timestamp) as _10"
      )
    checkUnalignedPages(df)(actions: _*)
  }
}
