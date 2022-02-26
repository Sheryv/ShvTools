package com.sheryv.tools.cloudservermanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class ServerManagerApplication

fun main(args: Array<String>) {
	runApplication<ServerManagerApplication>(*args)
}
