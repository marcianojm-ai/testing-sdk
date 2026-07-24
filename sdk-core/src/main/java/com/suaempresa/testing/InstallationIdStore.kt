package com.suaempresa.testing
import java.util.UUID
object InstallationIdStore {
 fun create():String = UUID.randomUUID().toString()
}