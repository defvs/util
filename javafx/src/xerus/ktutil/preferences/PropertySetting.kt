package xerus.ktutil.preferences

import javafx.beans.InvalidationListener
import javafx.beans.property.ObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import xerus.ktutil.SystemUtils.suppressErr
import xerus.ktutil.collections.WeakCollection
import xerus.ktutil.javafx.properties.Listeners
import xerus.ktutil.javafx.properties.bindSoft
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.prefs.Preferences
import kotlin.reflect.KClass

open class SettingsNode(val preferences: Preferences) {
	constructor(path: String) : this(getPreferences(path))
	
	/** Creates a new [PropertySetting] using the parameters and [preferences]
	 * and adds it to the [settings] so they can all be cleared at once */
	fun <T> create(key: String, default: T, parser: (String) -> T) =
		PropertySetting(key, default, preferences, parser).also { settings.add(it) }
	
	inline fun <reified T : Enum<T>> create(key: String, default: T) =
		create(key, default) { enumValueOf(it) }
	
	fun create(key: String, default: String = "") = create(key, default) { it }
	fun create(key: String, default: Boolean) = create(key, default) { it.toBoolean() }
	fun create(key: String, default: Int) = create(key, default) { it.toInt() }
	fun create(key: String, default: Long) = create(key, default) { it.toLong() }
	fun create(key: String, default: Double) = create(key, default) { it.toDouble() }
	
	fun create(key: String, default: File) = create(key, default) { File(it) }
	fun create(key: String, default: Path) = create(key, default) { Paths.get(it) }
	
	val settings = WeakCollection<PropertySetting<*>>()
	
	/** Removes all data from [preferences] and resets all created settings to their default and finally executes [flush] */
	fun clear() {
		preferences.clear()
		settings.forEach { it.clear() }
		flush()
	}
	
	/** Writes all pending changes to disk */
	fun flush() = preferences.flush()
	
	/** Reloads each Setting created by this [SettingsNode] from the [preferences] */
	fun refresh() {
		settings.forEach { it.refresh() }
	}
	
	companion object {
		fun getPreferences(clazz: KClass<*>): Preferences = getPreferences(clazz.java.`package`.name.replace('.', '/'))
		fun getPreferences(path: String): Preferences = suppressErr { Preferences.userRoot().node(path) }
	}
}

/**
 * Caches the Setting as an [ObjectProperty] to minimise I/O.
 *
 * It is recommended to use a [SettingsNode] to ease the creation, but it can also be used independently.
 */
open class PropertySetting<T>(private val key: String, private val default: T, val preferences: Preferences, private val parser: (String) -> T) : ObjectProperty<T>(), ISetting {
	
	override var value: String
		get() = get().toString()
		set(value) = set(parser(value))
	
	private var _value = loadValue()
	
	override fun get() = _value
	
	override fun set(value: T) {
		val old = _value
		_value = value
		listeners.notifyChange(old, value)
		preferences.put(key, value.toString())
	}
	
	/** Reloads the [value] from the [preferences] */
	fun refresh() = set(loadValue())
	
	private fun loadValue() = preferences.get(key, null)?.let {
		try {
			parser(it)
		} catch(e: Exception) {
			null
		}
	} ?: default
	
	/** Clears the entry in [preferences] and resets the value to the default */
	fun clear() {
		preferences.remove(key)
		set(default)
	}
	
	// Listeners
	private val listeners = Listeners(this)
	
	override fun addListener(listener: InvalidationListener) = listeners.add(listener)
	override fun addListener(listener: ChangeListener<in T>) = listeners.add(listener)
	override fun removeListener(listener: InvalidationListener) = listeners.remove(listener)
	override fun removeListener(listener: ChangeListener<in T>) = listeners.remove(listener)
	
	override fun getName() = key
	override fun getBean() = preferences
	
	// Bindings
	private var observable: ObservableValue<out T>? = null
	private var listener: InvalidationListener? = null
	/** calls [unbind] and then softly binds this [PropertySetting] to the given [ObservableValue] via a listener */
	override fun bind(observable: ObservableValue<out T>) {
		unbind()
		listener = bindSoft({ observable.value }, observable)
		this.observable = observable
	}
	
	/** stops listening to changes of [observable]. No-op if not bound. */
	override fun unbind() {
		observable?.removeListener(listener)
		observable = null
	}
	
	override fun isBound() = observable != null
	
	operator fun invoke() = get()
}
