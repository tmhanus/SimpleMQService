package com.messagingservice

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

// in progress

class LoggerConnectionFactory {
//    companion object {
//        private var dataSource: BasicDataSource? = null
//        private var conn : Connection? = null
//
//        var connection: Connection? {
//            if (conn == null) {
//                val connectionProps = Properties()
//                connectionProps["user"] = "root"
//                connectionProps["user"] = "root"
//                try {
//                    Class.forName("con.mysql.jdbc.Driver").newInstance()
//                    conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/messsageservice", connectionProps)
//                } catch (ex: SQLException) {
//                    ex.printStackTrace()
//                } catch (ex: Exception) {
//                    ex.printStackTrace()
//                }
//            }
//            return conn
//
//
////            if (dataSource == null) {
////                dataSource = BasicDataSource()
////                dataSource!!.url = "jdbc:mysql://localhost:3306/messsageservice?useSSL=false"
////                dataSource!!.driverClassName = "com.mysql.cj.jdbc.Driver"
////                dataSource!!.username = "root"
////                dataSource!!.password = "root"
////            }
////            return dataSource!!.connection
//        }
//    }

//    private val dataSource: DataSource
//    private val connection: Connection

    private object Holder { val INSTANCE = LoggerConnectionFactory() }

//    init {
//
//        try {
//            Class.forName("com.mysql.jdbc.Driver")
//        } catch (e: ClassNotFoundException) {
//            e.printStackTrace()
//            System.exit(0)
//        }
//
//        val properties = Properties()
//        properties.setProperty("user", "root")
//        properties.setProperty("password", "root")
//
//        this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/messsageservice", properties)
//    }



    companion object {
//        val connection: Connection
//            @Throws(SQLException::class)
//            get() = Holder.INSTANCE.connection

        fun getConnection() : Connection {
            try {
                Class.forName("com.mysql.jdbc.Driver")
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                System.exit(0)
            }

            val properties = Properties()
            properties.setProperty("user", "root")
            properties.setProperty("password", "root")

            return DriverManager.getConnection("jdbc:mysql://localhost:3306/messsageservice", properties)
        }

    }
}