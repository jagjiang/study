package com.mintlolly.spark_mysql

import java.util.Properties

import com.mintlolly.utils.Utils
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.slf4j.{Logger, LoggerFactory}

/**
 *
 * @description
 * @date 2019/12/5 15:11 
 * @author jiangbo
 *
 */
object Write2Mysql{
  val logging:Logger = LoggerFactory.getLogger(Write2Mysql.getClass)
  def main(args: Array[String]): Unit = {

    val spark =SparkSession.builder()
      .appName("Write2Mysql")
      .master("local[6]")
      .getOrCreate()
    val sc = spark.sparkContext
    val schemaString = "msid taz h_longitude h_latitude h_staytime o_longitude o_latitude o_staytime"
    val fields = schemaString.split(" ")
      .map(fieldName =>StructField(fieldName,StringType,nullable = true))
    val schema = StructType(fields)
    logging.info("开始读取HDFS数据")
    val rowRDD = sc.textFile("hdfs://192.168.200.150:8020/data/wuxi/hoOutput/*")
      .map(_.split("[ ;_]"))
      .map(attibutes =>Row(attibutes(0),attibutes(1),attibutes(2),attibutes(3),attibutes(4),attibutes(5),attibutes(6),attibutes(7)))
    val hoDF = spark.createDataFrame(rowRDD,schema)
    logging.info("构建临时表")
    hoDF.createOrReplaceTempView("ho")
    val table = "wuxi_ho"
    val propertiesPath = "learn-scala\\src\\main\\resources\\mysql.properties"
    logging.info("配置文件路径:{}",System.getProperty("user.dir")+propertiesPath)
    val properties: Properties = Utils.getProPerties(propertiesPath)
    val connectionProperties = new Properties()
    connectionProperties.setProperty("user",properties.getProperty("mysql.username"))
    connectionProperties.setProperty("password",properties.getProperty("mysql.password"))
    connectionProperties.setProperty("url",properties.getProperty("mysql.url"))
    val url = properties.getProperty("mysql.url")
    hoDF.write.mode(SaveMode.Append).jdbc(url,table,connectionProperties)

  }

}
