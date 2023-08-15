package com.sheryv.util.fx.lib

import javafx.beans.property.*
import javafx.beans.value.*
import javafx.collections.ListChangeListener
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.geometry.Insets


inline fun <T> ChangeListener(crossinline listener: (observable: ObservableValue<out T>?, oldValue: T, newValue: T) -> Unit): ChangeListener<T> =
  javafx.beans.value.ChangeListener<T> { observable, oldValue, newValue -> listener(observable, oldValue, newValue) }

/**
 * Listen for changes to this observable. Optionally only listen x times.
 * The lambda receives the changed value when the change occurs, which may be null,
 */
fun <T> ObservableValue<T>.onChangeTimes(times: Int, op: (T?) -> Unit) {
  var counter = 0
  val listener = object : ChangeListener<T> {
    override fun changed(observable: ObservableValue<out T>?, oldValue: T, newValue: T) {
      if (++counter == times) {
        removeListener(this)
      }
      op(newValue)
    }
  }
  addListener(listener)
}

fun <T> ObservableValue<T>.onChangeOnce(op: (T?) -> Unit) = onChangeTimes(1, op)

fun <T> ObservableValue<T>.onChange(op: (T?) -> Unit) = apply { addListener { _, _, newValue -> op(newValue) } }
fun <T> ObservableValue<T?>.onChangeNotNull(op: (T) -> Unit) =
  apply { addListener { _, _, newValue -> if (newValue != null) op(newValue) } }

fun <T> ObservableValue<T>.onChangeValue(op: (Pair<T?, T?>) -> Unit) = apply { addListener { _, old, newValue -> op(old to newValue) } }
fun ObservableBooleanValue.onChange(op: (Boolean) -> Unit) = apply { addListener { _, _, new -> op(new ?: false) } }
fun ObservableIntegerValue.onChange(op: (Int) -> Unit) = apply { addListener { _, _, new -> op((new ?: 0).toInt()) } }
fun ObservableLongValue.onChange(op: (Long) -> Unit) = apply { addListener { _, _, new -> op((new ?: 0L).toLong()) } }
fun ObservableFloatValue.onChange(op: (Float) -> Unit) = apply {
  addListener { _, _, new ->
    op((new ?: 0f).toFloat())
  }
}

fun ObservableDoubleValue.onChange(op: (Double) -> Unit) = apply {
  addListener { _, _, new ->
    op((new ?: 0.0).toDouble())
  }
}

fun <T> ObservableList<T>.onChange(op: (ListChangeListener.Change<out T>) -> Unit) = apply {
  addListener(op) // Sch.Funtik. Old: addListener(ListChangeListener { op(it) }) - unnecessary lambda
}

fun <K, V> ObservableMap<K, V>.onChange(op: (MapChangeListener.Change<out K, out V>) -> Unit) = apply {
  addListener(op) // addListener(MapChangeListener { op(it) })
}

/**
 * JavaDoc: Note that put operation might remove an element if there was already a value associated with the same key.
 * In this case wasAdded() and wasRemoved() will both return true.
 */
val <K, V> MapChangeListener.Change<out K, out V>.wasUpdated: Boolean
  get() = wasAdded() && wasRemoved()

/**
 * Create a proxy property backed by calculated data based on a specific property. The setter
 * must return the new value for the backed property.
 * The scope of the getter and setter will be the receiver property
 */
fun <R, T> proxyprop(receiver: Property<R>, getter: Property<R>.() -> T, setter: Property<R>.(T) -> R): ObjectProperty<T> =
  object : SimpleObjectProperty<T>() {
    init {
      receiver.onChange {
        fireValueChangedEvent()
      }
    }
    
    override fun invalidated() {
      receiver.value = setter(receiver, super.get())
    }
    
    override fun get() = getter.invoke(receiver)
    override fun set(v: T) {
      receiver.value = setter(receiver, v)
      super.set(v)
    }
  }

/**
 * Create a proxy double property backed by calculated data based on a specific property. The setter
 * must return the new value for the backed property.
 * The scope of the getter and setter will be the receiver property
 */
fun <R> proxypropDouble(receiver: Property<R>, getter: Property<R>.() -> Double, setter: Property<R>.(Double) -> R): DoubleProperty =
  object : SimpleDoubleProperty() {
    init {
      receiver.onChange {
        fireValueChangedEvent()
      }
    }
    
    override fun invalidated() {
      receiver.value = setter(receiver, super.get())
    }
    
    override fun get() = getter.invoke(receiver)
    override fun set(v: Double) {
      receiver.value = setter(receiver, v)
      super.set(v)
    }
  }

fun insets(all: Number) = Insets(all.toDouble(), all.toDouble(), all.toDouble(), all.toDouble())
fun insets(horizontal: Number? = null, vertical: Number? = null) = Insets(
  vertical?.toDouble() ?: 0.0,
  horizontal?.toDouble() ?: 0.0,
  vertical?.toDouble() ?: 0.0,
  horizontal?.toDouble() ?: 0.0
)

fun insets(
  top: Number? = null,
  right: Number? = null,
  bottom: Number? = null,
  left: Number? = null
) = Insets(
  top?.toDouble() ?: 0.0,
  right?.toDouble() ?: 0.0,
  bottom?.toDouble() ?: 0.0,
  left?.toDouble() ?: 0.0
)

fun Insets.copy(
  top: Number? = null,
  right: Number? = null,
  bottom: Number? = null,
  left: Number? = null
) = Insets(
  top?.toDouble() ?: this.top,
  right?.toDouble() ?: this.right,
  bottom?.toDouble() ?: this.bottom,
  left?.toDouble() ?: this.left
)


fun Insets.copy(
  horizontal: Number? = null,
  vertical: Number? = null
) = Insets(
  vertical?.toDouble() ?: this.top,
  horizontal?.toDouble() ?: this.right,
  vertical?.toDouble() ?: this.bottom,
  horizontal?.toDouble() ?: this.left
)

val Insets.horizontal get() = (left + right) / 2
val Insets.vertical get() = (top + bottom) / 2
val Insets.all get() = (left + right + top + bottom) / 4


fun String.isLong() = toLongOrNull() != null
fun String.isInt() = toIntOrNull() != null
fun String.isDouble() = toDoubleOrNull() != null
fun String.isFloat() = toFloatOrNull() != null

///**
// * [forEach] with Map.Entree as receiver.
// */
//inline fun <K, V> Map<K, V>.withEach(action: Map.Entry<K, V>.() -> Unit) = forEach(action)
//
///**
// * [forEach] with the element as receiver.
// */
//inline fun <T> Iterable<T>.withEach(action: T.() -> Unit) = forEach(action)
//
///**
// * [forEach] with the element as receiver.
// */
//inline fun <T> Sequence<T>.withEach(action: T.() -> Unit) = forEach(action)
//
///**
// * [forEach] with the element as receiver.
// */
//inline fun <T> Array<T>.withEach(action: T.() -> Unit) = forEach(action)
//
///**
// * [map] with Map.Entree as receiver.
// */
//inline fun <K, V, R> Map<K, V>.mapEach(action: Map.Entry<K, V>.() -> R) = map(action)
//
///**
// * [map] with the element as receiver.
// */
//inline fun <T, R> Iterable<T>.mapEach(action: T.() -> R) = map(action)
//
///**
// * [map] with the element as receiver.
// */
//fun <T, R> Sequence<T>.mapEach(action: T.() -> R) = map(action)
//
///**
// * [map] with the element as receiver.
// */
//inline fun <T, R> Array<T>.mapEach(action: T.() -> R) = map(action)
//
///**
// * [mapTo] with Map.Entree as receiver.
// */
//inline fun <K, V, R, C : MutableCollection<in R>> Map<K, V>.mapEachTo(destination: C, action: Map.Entry<K, V>.() -> R) = mapTo(destination, action)
//
///**
// * [mapTo] with the element as receiver.
// */
//inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapEachTo(destination: C, action: T.() -> R) = mapTo(destination, action)
//
///**
// * [mapTo] with the element as receiver.
// */
//fun <T, R, C : MutableCollection<in R>> Sequence<T>.mapEachTo(destination: C, action: T.() -> R) = mapTo(destination, action)
//
///**
// * [mapTo] with the element as receiver.
// */
//fun <T, R, C : MutableCollection<in R>> Array<T>.mapEachTo(destination: C, action: T.() -> R) = mapTo(destination, action)
