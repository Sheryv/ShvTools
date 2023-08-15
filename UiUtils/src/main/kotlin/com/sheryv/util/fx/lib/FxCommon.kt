package com.sheryv.util.fx.lib

import javafx.application.Platform
import javafx.event.EventTarget
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.SubScene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import java.util.concurrent.CountDownLatch

object FxUtils {
  
  fun runInUiThreadAndWait(action: () -> Unit) {
    if (Platform.isFxApplicationThread()) {
      action()
      return
    }
    
    val doneLatch = CountDownLatch(1)
    Platform.runLater {
      try {
        action()
      } finally {
        doneLatch.countDown()
      }
    }
    
    try {
      doneLatch.await()
    } catch (e: InterruptedException) {
      // ignore exception
    }
  }
  
}

inline fun <T : Node> T.attachTo(parent: Parent, op: T.() -> Unit = {}): T {
  parent.addChildIfPossible(this)
  op(this)
  return this
}

/**
 * Attaches the node to the pane and invokes the node operation.
 * Because the framework sometimes needs to setup the node, another lambda can be provided
 */
inline fun <T : Node> T.attachTo(
  parent: Parent,
  after: T.() -> Unit,
  before: (T) -> Unit
) = this.also(before).attachTo(parent, after)


fun Node.getChildList(): List<Node>? = when (this) {
  is SplitPane -> items
  is ToolBar -> items
  is Pane -> children
  is Group -> children
  is Control -> (skin as? SkinBase<*>)?.children ?: this.childrenUnmodifiable.toList()
  is Parent -> this.childrenUnmodifiable.toList()
  else -> null
}

fun Node.addChildIfPossible(node: Node, index: Int? = null) {
  
  when (this) {
    is SubScene -> {
      root = node as Parent
    }

//    is UIComponent -> root?.addChildIfPossible(node)
    is ScrollPane -> content = node
    is Tab -> {
      // Map the tab to the UIComponent for later retrieval. Used to close tab with UIComponent.close()
      // and to connect the onTabSelected callback
//      node.uiComponent<UIComponent>()?.properties?.set("shvtools.tab", this)
      content = node
    }
    
    is ButtonBase -> {
      graphic = node
    }
    
    is BorderPane -> {
      children.add(node)
    } // Either pos = builder { or caught by builderTarget above
    is TabPane -> {
//      val uicmp = node.uiComponent<UIComponent>()
//      val tab = if (uicmp != null) {
//        Tab().apply {
//          node.uiComponent<UIComponent>()?.properties?.set("shvtools.tab", this)
//          content = node
//          textProperty().bind(uicmp.titleProperty)
//          closableProperty().bind(uicmp.closeable)
//        }
//      } else {
//        Tab(node.toString(), node)
//      }
      if (node is Tab) {
        tabs.add(node)
      } else {
        Tab(node.toString(), node)
      }
    }
    
    is TitledPane -> {
      if (content is Pane) {
        content.addChildIfPossible(node, index)
      } else if (content is Node) {
        val container = VBox()
        container.children.addAll(content, node)
        content = container
      } else {
        content = node
      }
    }
    
    is CustomMenuItem -> {
      content = node
    }
    
    is MenuItem -> {
      graphic = node
    }
    
    else -> {
      when (this) {
        is SplitPane -> items
        is ToolBar -> items
        is Pane -> children
        is Group -> children
        is Control -> (skin as? SkinBase<*>)?.children
        else -> null
      }?.also {
        if (!it.contains(node)) {
          if (index != null && index < it.size)
            it.add(index, node)
          else
            it.add(node)
        }
      }
    }
  }
}

fun EventTarget.removeFromParent() {
  when (this) {
    is Tab -> tabPane?.tabs?.remove(this)
    is Node -> {
      val p = parent
      (p?.parent as? ToolBar)?.items?.remove(this) ?: when (p) {
        is SplitPane -> p.items
        is ToolBar -> p.items
        is Pane -> p.children
        is Group -> p.children
        is Control -> (p.skin as? SkinBase<*>)?.children
        else -> null
      }?.remove(this) ?: throw RuntimeException("Cannot find parent with exposed children for $this")
    }
    
    is TreeItem<*> -> this.parent.children.remove(this)
    is MenuItem -> this.parentMenu.items.remove(this)
    else -> throw RuntimeException("removeFromParent() is not supported for $this")
  }
}
