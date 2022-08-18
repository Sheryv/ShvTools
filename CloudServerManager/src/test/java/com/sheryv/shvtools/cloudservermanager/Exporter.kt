package com.sheryv.shvtools.cloudservermanager

import com.querydsl.sql.codegen.MetaDataExporter
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.DriverManager




class Exporter {
  @Test
  fun name() {
    val e = MetaDataExporter()
    e.setTargetFolder(File("src/main/generated/"))
    e.setPackageName("com.sheryv.shvtools.cloudservermanager")
    e.setNamePrefix("")
    e.setNameSuffix("Table")
    
    DriverManager.getConnection("jdbc:postgresql://localhost:5432/mgmt", "postgres", "postgres").use { conn -> e.export(conn.metaData) }
  }
}
