/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.griffin.measure.datasource.connector.batch

import java.util.Properties

import org.apache.spark.sql.{DataFrame, SparkSession}

import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.utils.ParamUtil._

case class JdbcBatchDataConnector(@transient sparkSession: SparkSession,
                                  dcParam: DataConnectorParam,
                                  timestampStorage: TimestampStorage
                                 ) extends BatchDataConnector {

  val config = dcParam.getConfig

  val Database = "database"
  val TableName = "table.name"
  val JdbcUrl = "jdbc.url"
  val User = "jdbc.user"
  val Password = "jdbc.password"

  val database = config.getString(Database, "")
  val tableName = config.getString(TableName, "")
  val jdbcUrl = config.getString(JdbcUrl, "")
  val user = config.getString(User, "")
  val password = config.getString(Password, "")

  val concreteTableName = s"${database}.${tableName}"

  def data(ms: Long): (Option[DataFrame], TimeRange) = {
    val dfOpt = try {
      val prop = new Properties()
      prop.put("user", user)
      prop.put("password", password)
      val df = sparkSession.read.jdbc(jdbcUrl, tableName, prop)
      val dfOpt = Some(df)
      val preDfOpt = preProcess(dfOpt, ms)
      preDfOpt
    } catch {
      case e: Throwable =>
        error(s"load hive table ${concreteTableName} fails: ${e.getMessage}", e)
        None
    }
    val tmsts = readTmst(ms)
    (dfOpt, TimeRange(ms, tmsts))
  }

}
