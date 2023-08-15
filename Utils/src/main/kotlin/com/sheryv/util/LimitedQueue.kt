package com.sheryv.util

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.*


class LimitedQueue<E>(initSize: Int = 32) : AbstractCollection<E>(), Queue<E>, Serializable {
  /** Underlying storage array.  */
  @Transient
  private var elements: Array<E?>
  
  /** Array index of first (oldest) queue element.  */
  @Transient
  private var start = 0
  
  /**
   * Index mod maxElements of the array position following the last queue
   * element.  Queue elements start at elements[start] and "wrap around"
   * elements[maxElements-1], ending at elements[decrement(end)].
   * For example, elements = {c,a,b}, start=1, end=1 corresponds to
   * the queue [a,b,c].
   */
  @Transient
  private var end = 0
  
  /** Flag to indicate if the queue is currently full.  */
  @Transient
  private var full = false
  
  /** Capacity of the queue.  */
  private val maxElements: Int
  /**
   * Constructor that creates a queue with the specified size.
   *
   * @param size  the size of the queue (cannot be changed)
   * @throws IllegalArgumentException  if the size is &lt; 1
   */
  /**
   * Constructor that creates a queue with the default size of 32.
   */
  init {
    require(initSize > 0) { "The size must be greater than 0" }
    elements = arrayOfNulls<Any>(initSize) as Array<E?>
    maxElements = elements.size
  }
  
  /**
   * Constructor that creates a queue from the specified collection.
   * The collection size also sets the queue size.
   *
   * @param coll  the collection to copy into the queue, may not be null
   * @throws NullPointerException if the collection is null
   */
  constructor(coll: Collection<E>) : this(coll.size) {
    addAll(coll)
  }
  
  /**
   * Write the queue out using a custom routine.
   *
   * @param out  the output stream
   * @throws IOException if an I/O error occurs while writing to the output stream
   */
  @Throws(IOException::class)
  private fun writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject()
    out.writeInt(size)
    for (e in this) {
      out.writeObject(e)
    }
  }
  
  /**
   * Read the queue in using a custom routine.
   *
   * @param in  the input stream
   * @throws IOException if an I/O error occurs while writing to the output stream
   * @throws ClassNotFoundException if the class of a serialized object can not be found
   */
  @Throws(IOException::class, ClassNotFoundException::class)
  private fun readObject(`in`: ObjectInputStream) {
    `in`.defaultReadObject()
    elements = arrayOfNulls<Any>(maxElements) as Array<E?>
    val size = `in`.readInt()
    for (i in 0 until size) {
      elements[i] = `in`.readObject() as E
    }
    start = 0
    full = size == maxElements
    end = if (full) {
      0
    } else {
      size
    }
  }
  
  /**
   * Returns the number of elements stored in the queue.
   *
   * @return this queue's size
   */
  override val size: Int
    get() {
      var size = 0
      size = if (end < start) {
        maxElements - start + end
      } else if (end == start) {
        if (full) maxElements else 0
      } else {
        end - start
      }
      return size
    }
  
  /**
   * Returns true if this queue is empty; false otherwise.
   *
   * @return true if this queue is empty
   */
  override fun isEmpty(): Boolean {
    return size == 0
  }
  
  /**
   * {@inheritDoc}
   *
   *
   * A `CircularFifoQueue` can never be full, thus this returns always
   * `false`.
   *
   * @return always returns `false`
   */
  fun isFull(): Boolean {
    return false
  }
  
  val isAtFullCapacity: Boolean
    /**
     * Returns `true` if the capacity limit of this queue has been reached,
     * i.e. the number of elements stored in the queue equals its maximum size.
     *
     * @return `true` if the capacity limit has been reached, `false` otherwise
     * @since 4.1
     */
    get() = size == maxElements
  
  /**
   * Gets the maximum size of the collection (the bound).
   *
   * @return the maximum number of elements the collection can hold
   */
  fun maxSize(): Int {
    return maxElements
  }
  
  /**
   * Clears this queue.
   */
  override fun clear() {
    full = false
    start = 0
    end = 0
    Arrays.fill(elements, null)
  }
  
  /**
   * Adds the given element to this queue. If the queue is full, the least recently added
   * element is discarded so that a new element can be inserted.
   *
   * @param element  the element to add
   * @return true, always
   * @throws NullPointerException  if the given element is null
   */
  override fun add(element: E): Boolean {
    Objects.requireNonNull(element, "element")
    if (isAtFullCapacity) {
      remove()
    }
    elements[end++] = element
    if (end >= maxElements) {
      end = 0
    }
    if (end == start) {
      full = true
    }
    return true
  }
  
  /**
   * Returns the element at the specified position in this queue.
   *
   * @param index the position of the element in the queue
   * @return the element at position `index`
   * @throws NoSuchElementException if the requested position is outside the range [0, size)
   */
  operator fun get(index: Int): E {
    val sz = size
    if (index < 0 || index >= sz) {
      throw NoSuchElementException(String.format("The specified index %1\$d is outside the available range [0, %2\$d)", index, sz))
    }
    val idx = (start + index) % maxElements
    return elements[idx]!!
  }
  
  /**
   * Adds the given element to this queue. If the queue is full, the least recently added
   * element is discarded so that a new element can be inserted.
   *
   * @param element  the element to add
   * @return true, always
   * @throws NullPointerException  if the given element is null
   */
  override fun offer(element: E): Boolean {
    return add(element)
  }
  
  override fun poll(): E? {
    return if (isEmpty()) {
      null
    } else remove()
  }
  
  override fun element(): E {
    if (isEmpty()) {
      throw NoSuchElementException("queue is empty")
    }
    return peek()!!
  }
  
  override fun peek(): E? {
    return if (isEmpty()) {
      null
    } else elements[start]
  }
  
  override fun remove(): E {
    if (isEmpty()) {
      throw NoSuchElementException("queue is empty")
    }
    val element = elements[start]
    if (null != element) {
      elements[start++] = null
      if (start >= maxElements) {
        start = 0
      }
      full = false
    }
    return element!!
  }
  
  /**
   * Increments the internal index.
   *
   * @param index  the index to increment
   * @return the updated index
   */
  private fun increment(index: Int): Int {
    var index = index
    index++
    if (index >= maxElements) {
      index = 0
    }
    return index
  }
  
  /**
   * Decrements the internal index.
   *
   * @param index  the index to decrement
   * @return the updated index
   */
  private fun decrement(index: Int): Int {
    var index = index
    index--
    if (index < 0) {
      index = maxElements - 1
    }
    return index
  }
  
  /**
   * Returns an iterator over this queue's elements.
   *
   * @return an iterator over this queue's elements
   */
  override fun iterator(): MutableIterator<E> {
    return object : MutableIterator<E> {
      private var index = start
      private var lastReturnedIndex = -1
      private var isFirst = full
      override fun hasNext(): Boolean {
        return isFirst || index != end
      }
      
      override fun next(): E {
        if (!hasNext()) {
          throw NoSuchElementException()
        }
        isFirst = false
        lastReturnedIndex = index
        index = increment(index)
        return elements[lastReturnedIndex]!!
      }
      
      override fun remove() {
        check(lastReturnedIndex != -1)
        
        // First element can be removed quickly
        if (lastReturnedIndex == start) {
          this@LimitedQueue.remove()
          lastReturnedIndex = -1
          return
        }
        var pos = lastReturnedIndex + 1
        if (start < lastReturnedIndex && pos < end) {
          // shift in one part
          System.arraycopy(elements, pos, elements, lastReturnedIndex, end - pos)
        } else {
          // Other elements require us to shift the subsequent elements
          while (pos != end) {
            if (pos >= maxElements) {
              elements[pos - 1] = elements[0]
              pos = 0
            } else {
              elements[decrement(pos)] = elements[pos]
              pos = increment(pos)
            }
          }
        }
        lastReturnedIndex = -1
        end = decrement(end)
        elements[end] = null
        full = false
        index = decrement(index)
      }
    }
  }
  
  companion object {
    /** Serialization version.  */
    private const val serialVersionUID = -8423413834657610406L
  }
}
