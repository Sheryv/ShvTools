package com.sheryv.util.fx.lib

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.util.*


fun <T> List<T>.asObservable(): ObservableList<T> = FXCollections.observableList(this)

fun <T> MutableList<T>.move(item: T, newIndex: Int) {
  check(newIndex in 0 until size)
  val currentIndex = indexOf(item)
  if (currentIndex < 0) return
  removeAt(currentIndex)
  add(newIndex, item)
}

/**
 * Moves the given item at the `oldIndex` to the `newIndex`
 */
fun <T> MutableList<T>.moveAt(oldIndex: Int, newIndex: Int) {
  check(oldIndex in 0 until size)
  check(newIndex in 0 until size)
  val item = this[oldIndex]
  removeAt(oldIndex)
  add(newIndex, item)
}

/**
 * Moves all items meeting a predicate to the given index
 */
fun <T> MutableList<T>.moveAll(newIndex: Int, predicate: (T) -> Boolean) {
  check(newIndex in 0 until size)
  val split = partition(predicate)
  clear()
  addAll(split.second)
  addAll(if (newIndex >= size) size else newIndex, split.first)
}

/**
 * Moves the given element at specified index up the **MutableList** by one increment
 * unless it is at the top already which will result in no movement
 */
fun <T> MutableList<T>.moveUpAt(index: Int) {
  if (index == 0) return
  check(index in indices, { "Invalid index $index for MutableList of size $size" })
  val newIndex = index - 1
  val item = this[index]
  removeAt(index)
  add(newIndex, item)
}

/**
 * Moves the given element **T** up the **MutableList** by one increment
 * unless it is at the bottom already which will result in no movement
 */
fun <T> MutableList<T>.moveDownAt(index: Int) {
  if (index == size - 1) return
  check(index in indices, { "Invalid index $index for MutableList of size $size" })
  val newIndex = index + 1
  val item = this[index]
  removeAt(index)
  add(newIndex, item)
}

/**
 * Moves the given element **T** up the **MutableList** by an index increment
 * unless it is at the top already which will result in no movement.
 * Returns a `Boolean` indicating if move was successful
 */
fun <T> MutableList<T>.moveUp(item: T): Boolean {
  val currentIndex = indexOf(item)
  if (currentIndex == -1) return false
  val newIndex = (currentIndex - 1)
  if (currentIndex <= 0) return false
  remove(item)
  add(newIndex, item)
  return true
}

/**
 * Moves the given element **T** up the **MutableList** by an index increment
 * unless it is at the bottom already which will result in no movement.
 * Returns a `Boolean` indicating if move was successful
 */
fun <T> MutableList<T>.moveDown(item: T): Boolean {
  val currentIndex = indexOf(item)
  if (currentIndex == -1) return false
  val newIndex = (currentIndex + 1)
  if (newIndex >= size) return false
  remove(item)
  add(newIndex, item)
  return true
}


/**
 * Moves first element **T** up an index that satisfies the given **predicate**, unless its already at the top
 */
inline fun <T> MutableList<T>.moveUp(crossinline predicate: (T) -> Boolean) = find(predicate)?.let { moveUp(it) }

/**
 * Moves first element **T** down an index that satisfies the given **predicate**, unless its already at the bottom
 */
inline fun <T> MutableList<T>.moveDown(crossinline predicate: (T) -> Boolean) = find(predicate)?.let { moveDown(it) }


/**
 * Swaps the position of two items at two respective indices
 */
fun <T> MutableList<T>.swap(indexOne: Int, indexTwo: Int) {
  if (this is ObservableList<*>) {
    if (indexOne == indexTwo) return
    val min = Math.min(indexOne, indexTwo)
    val max = Math.max(indexOne, indexTwo)
    val o2 = removeAt(max)
    val o1 = removeAt(min)
    add(min, o2)
    add(max, o1)
  } else {
    Collections.swap(this, indexOne, indexTwo)
  }
}

/**
 * Swaps the index position of two items
 */
fun <T> MutableList<T>.swap(itemOne: T, itemTwo: T) = swap(indexOf(itemOne), indexOf(itemTwo))
