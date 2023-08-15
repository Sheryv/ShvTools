package com.sheryv.util.fx.lib

import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ObservableValue
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Tab
import javafx.scene.control.TabPane

fun Parent.tabpane(op: TabPane.() -> Unit = {}) = TabPane().attachTo(this, op)

fun <T : Node> TabPane.tab(text: String, content: T, op: T.() -> Unit = {}): Tab {
  return tab(tabs.size, text, content, op)
}

fun TabPane.tab(text: String,  op: Tab.() -> Unit = {}): Tab {
  val tab = Tab(text, Group())
  tabs.add(tab)
  op(tab)
  return tab
}

fun <T : Node> TabPane.tab(index: Int, text: String, content: T, op: T.() -> Unit = {}): Tab {
  val tab = Tab(text, content)
  tabs.add(index, tab)
  op(content)
  return tab
}

fun Tab.disableWhen(predicate: ObservableValue<Boolean>) = disableProperty().cleanBind(predicate)
fun Tab.enableWhen(predicate: ObservableValue<Boolean>) {
  val binding = if (predicate is BooleanBinding) predicate.not() else predicate.toBinding().not()
  disableProperty().cleanBind(binding)
}

fun Tab.closeableWhen(predicate: ObservableValue<Boolean>) {
  closableProperty().bind(predicate)
}

fun Tab.visibleWhen(predicate: ObservableValue<Boolean>) {
  val localTabPane = tabPane
  fun updateState() {
    if (predicate.value.not()) localTabPane.tabs.remove(this)
    else if (this !in localTabPane.tabs) localTabPane.tabs.add(this)
  }
  updateState()
  predicate.onChange { updateState() }
}

fun Tab.close() = removeFromParent()


fun Tab.whenSelected(op: () -> Unit) {
  selectedProperty().onChange { if (it) op() }
}

fun Tab.select() = apply { tabPane.selectionModel.select(this) }
