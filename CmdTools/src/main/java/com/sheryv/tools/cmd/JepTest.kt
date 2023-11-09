package com.sheryv.tools.cmd

import com.sheryv.util.inBackground
import jep.JepConfig
import jep.SharedInterpreter
import kotlinx.coroutines.runBlocking
import kotlin.system.measureNanoTime


fun main() {
  val props = mapOf(
    "PYTHONHOME" to "C:\\temp\\python-3.11.5-embed-amd64",
    "PYTHONPATH" to "C:\\temp\\python-3.11.5-embed-amd64",
    "LD_LIBRARY_PATH" to "C:\\temp\\python-3.11.5-embed-amd64\\Lib\\site-packages\\jep"
  )
  System.getProperties().putAll(props)
  
  println("START")
  val c = JepConfig()
  c.redirectStdErr(System.err)
  c.redirectStdout(System.out)
  
  val contextLogic = """
    __saved_context__ = {}
    __last_saved_context__ = ""
    
    def clear_context():
        import sys
        this = sys.modules[__name__]
        for _ in dir():
            if _[0]!='_': delattr(this, _)
    
    def save_context(id):
        import sys
        __saved_context__[id] = {**sys.modules[__name__].__dict__}
        for n in list(__saved_context__[id].keys()):
            del sys.modules[__name__].__dict__[n]
    
    def restore_context(id):
        import sys
        names = list(sys.modules[__name__].__dict__.keys())
        saved = __saved_context__[id]
        for n in names:
            if n not in saved:
                del sys.modules[__name__].__dict__[n]
            else:
                sys.modules[__name__][n] = saved[n]
  """.trimIndent()
  
  
  SharedInterpreter.setConfig(c)
  val interpreter: SharedInterpreter
  var t = measureNanoTime {
    interpreter = SharedInterpreter()
  } / 1000000.0
  println(">> create $t")
  t = measureNanoTime {
    interpreter.exec("import builtins")
    interpreter.exec("b=2+2")
    println(interpreter.invoke("builtins.max", 123, 345))
    println(interpreter.getValue("b"))
    interpreter.exec("""print(f'inside {b}')""")
    
    interpreter.exec(contextLogic)
    val test = """
      save_context('a')
      try:
          print(f'after save {b}') #1
          b = 12321
      except Exception as e:
          print('Cannot find b', e)
          
      restore_context('a')
      
      print(f'after restore {b}') #2
    """.trimIndent()
    interpreter.exec(test)
    
  } / 1000000.0
  println(">> first $t")
  runBlocking {
    inBackground {
      t = measureNanoTime {
        SharedInterpreter().use { interp ->
          interp.exec("from java.lang import System")
          interp.exec("s = 'Hello World'")
          interp.exec("System.out.println(s)")
          interp.exec("print(s)")
          interp.exec("print(s[1:-1])")
          
        }
      } / 1000000.0
      

      
      println(">> second $t")
    }
  }
  t = measureNanoTime {
    interpreter.use { interp ->
      interp.exec("from java.lang import System")
      interp.exec("s = 'Hello3 World'")
      interp.exec("System.out.println(s)")
      interp.exec("print(s)")
      interp.exec("print(s[1:-1])")
    }
  } / 1000000.0
  println(">> third $t")
  println("END")
}
