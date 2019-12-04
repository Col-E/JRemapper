package me.coley.jremapper.util;

import java.lang.reflect.*;

/**
 * Reflection utilities.
 * 
 * @author Matt
 */
public class Reflect {

	/**
	 * Get all fields belonging to the given class.
	 * 
	 * @param clazz
	 *            Class containing fields.
	 * @return Array of class's fields.
	 */
	public static Field[] fields(Class<?> clazz) {
		try {
			Field[] ff = clazz.getDeclaredFields();
			for (Field f : ff)
				f.setAccessible(true);
			return ff;
		} catch (Exception e) {
			Logging.fatal(e);
			return null;
		}
	}

	/**
	 * Get first field matching one of the given names.
	 * 
	 * @param clazz
	 *            Owner with fields.
	 * @param aliases
	 *            Field names.
	 * @return Field matching name in alias set. May be null.
	 */
	public static Field getField(Class<?> clazz, String... aliases) {
		Field field = null;
		for (String alias : aliases) {
			try {
				field = clazz.getDeclaredField(alias);
				if (field != null) break;
			} catch (Exception e) {
				Logging.fatal(e);
			}
		}
		return field;
	}

	/**
	 * Get the value of the field by its name in the given object instance.
	 * 
	 * @param instance
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @return Field value. {@code null} if could not be reached.
	 */
	public static <T> T get(Object instance, String fieldName) {
		return get(instance, instance.getClass(), fieldName);
	}

	/**
	 * Get the value of the field by its name in the given object instance.
	 * 
	 * @param instance
	 *            Object instance.
	 * @param clazz
	 *            Class that holds the field. Allows for specifying fields of
	 *            super-class of instance.
	 * @param fieldName
	 *            Field name.
	 * @return Field value. {@code null} if could not be reached.
	 */
	public static <T> T get(Object instance, Class<?> clazz, String fieldName) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			return get(instance, field);
		} catch (NoSuchFieldException | SecurityException e) {
			Logging.fatal(e);
			return null;
		}
	}

	/**
	 * Get the value of the field in the given object instance.
	 * 
	 * @param instance
	 *            Object instance.
	 * @param field
	 *            Field, assumed to be accessible.
	 * @return Field value. {@code null} if could not be reached.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(Object instance, Field field) {
		try {
			return (T) field.get(instance);
		} catch (Exception e) {
			Logging.fatal(e);
			return null;
		}
	}

	/**
	 * Sets the value of the field in the given object instance.
	 * 
	 * @param instance
	 *            Object instance.
	 * @param field
	 *            Field, assumed to be accessible.
	 * @param value
	 *            Value to set.
	 */
	public static void set(Object instance, Field field, Object value) {
		try {
			field.set(instance, value);
		} catch (Exception e) {
			Logging.fatal(e);
		}
	}
}