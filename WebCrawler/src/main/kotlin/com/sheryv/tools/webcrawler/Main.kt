@file:JvmName("MainLauncher")

package com.sheryv.tools.webcrawler

import com.formdev.flatlaf.FlatDarkLaf
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
//  System.setProperty("prism.lcdtext", "false");
//  System.setProperty("prism.text", "t2k");
  SwingUtilities.invokeLater {
    FlatDarkLaf.setup()
  }
  MainApplication.start(args)
}
