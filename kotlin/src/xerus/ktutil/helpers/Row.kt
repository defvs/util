package xerus.ktutil.helpers

import java.util.*

/** A List of Strings similar to an ArrayList that never throws an [IndexOutOfBoundsException].
 *
 * When trying to access an element out of bounds, then it will report an empty String.
 * If an element at an index is set which is out of bounds, then the List will expand with empty Strings.*/
class Row(override var size: Int, vararg data: String) : AbstractList<String>(), RandomAccess {
	constructor(vararg data: String) : this(data.size, *data)
	
	@Suppress("Unchecked_cast")
	private var data: Array<String> = data as Array<String>
	
	override fun get(index: Int): String =
		if(index >= data.size) "" else data[index]
	
	override fun set(index: Int, element: String): String? {
		var old: String? = null
		if(data.size < index) {
			data = Arrays.copyOf(data, index + 2).map { it ?: "" }.toTypedArray()
			size = data.size.coerceAtLeast(size)
		} else
			old = data[index]
		data[index] = element
		return old
	}
	
	override fun toString() = "Row(${Arrays.toString(data)})"
	
}