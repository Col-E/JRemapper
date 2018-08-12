package me.coley.jremapper.util;

import java.net.URL;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import me.coley.jremapper.asm.Access;
import me.coley.jremapper.asm.AccessContext;

/**
 * Icons.
 * 
 * @author Matt
 */
public class Icons {
	// access images
	public static final Image CL_PACKAGE, CL_ENUM, CL_ANNOTATION, CL_INTERFACE, CL_CLASS;
	public static final Image MOD_ABSTRACT, MOD_FINAL, MOD_NATIVE, MOD_STATIC, MOD_SYNTHETIC, MOD_TRANSIENT, MOD_VOLATILE;
	public static final Image F_DEFAULT, M_DEFAULT, F_PRIVATE, M_PRIVATE, F_PROTECTED, M_PROTECTED, F_PUBLIC, M_PUBLIC;
	// misc
	public static final Image  FIND;


	static {
		// access images
		CL_PACKAGE = load("class/package");
		CL_ENUM = load("class/enum");
		CL_ANNOTATION = load("class/annotation");
		CL_INTERFACE = load("class/interface");
		CL_CLASS = load("class/class");
		//
		MOD_ABSTRACT = load("modifier/abstract");
		MOD_FINAL = load("modifier/final");
		MOD_NATIVE = load("modifier/native");
		MOD_STATIC = load("modifier/static");
		MOD_SYNTHETIC = load("modifier/synthetic");
		MOD_TRANSIENT = load("modifier/transient");
		MOD_VOLATILE = load("modifier/volatile");
		//
		F_DEFAULT = load("modifier/field_default");
		F_PRIVATE = load("modifier/field_private");
		F_PROTECTED = load("modifier/field_protected");
		F_PUBLIC = load("modifier/field_public");
		M_DEFAULT = load("modifier/method_default");
		M_PRIVATE = load("modifier/method_private");
		M_PROTECTED = load("modifier/method_protected");
		M_PUBLIC = load("modifier/method_public");
		// misc
		FIND = load("find");
	}

	/**
	 * Get image representation of a class by its access flags.
	 * 
	 * @param access
	 *            Flags <i>(Modifiers)</i>
	 * @return Image for flags.
	 */
	public static Group getClass(int access) {
		Image base = null;
		if (Access.isInterface(access)) {
			base = CL_INTERFACE;
		} else if (Access.isEnum(access)) {
			base = CL_ENUM;
		} else if (Access.isAnnotation(access)) {
			base = CL_ANNOTATION;
		} else {
			base = CL_CLASS;
		}
		Group g = new Group(new ImageView(base));
		if (!Access.isInterface(access) && Access.isAbstract(access)) {
			g.getChildren().add(new ImageView(MOD_ABSTRACT));
		}
		if (!Access.isEnum(access) && Access.isFinal(access)) {
			g.getChildren().add(new ImageView(MOD_FINAL));
		}
		if (Access.isNative(access)) {
			g.getChildren().add(new ImageView(MOD_NATIVE));
		}
		if (Access.isStatic(access)) {
			g.getChildren().add(new ImageView(MOD_STATIC));
		}
		if (Access.isSynthetic(access)) {
			g.getChildren().add(new ImageView(MOD_SYNTHETIC));
		}
		return g;
	}


	/**
	 * Get image representation of a class by its access flags. Shows additional
	 * flags such as {@code public, private, etc.}.
	 * 
	 * @param access
	 *            Flags <i>(Modifiers)</i>
	 * @return Image for flags.
	 */
	public static Group getClassExtended(int access) {
		Group g = getClass(access);
		if (Access.isPublic(access)) {
			g.getChildren().add(new ImageView(F_PUBLIC));
		}
		if (Access.isProtected(access)) {
			g.getChildren().add(new ImageView(F_PROTECTED));
		}
		if (Access.isPrivate(access)) {
			g.getChildren().add(new ImageView(F_PRIVATE));
		}
		if (Access.isSynthetic(access)) {
			g.getChildren().add(new ImageView(MOD_SYNTHETIC));
		}
		return g;
	}

	/**
	 * Get single image for access. Intended usage for individual modifiers.
	 * 
	 * @param access
	 *            Single modifier flag.
	 * @param context
	 * @return ImageView of flag.
	 */
	public static Node getAccess(int access, AccessContext context) {
		if (Access.isEnum(access)) {
			return new ImageView(CL_ENUM);
		}
		if (Access.isInterface(access)) {
			return new ImageView(CL_INTERFACE);
		}
		if (Access.isAnnotation(access)) {
			return new ImageView(CL_ANNOTATION);
		}
		if (Access.isPublic(access)) {
			return new ImageView(F_PUBLIC);
		}
		if (Access.isProtected(access)) {
			return new ImageView(F_PROTECTED);
		}
		if (Access.isPrivate(access)) {
			return new ImageView(F_PRIVATE);
		}
		if (Access.isAbstract(access)) {
			return new ImageView(MOD_ABSTRACT);
		}
		if (Access.isFinal(access)) {
			return new ImageView(MOD_FINAL);
		}
		if (Access.isNative(access)) {
			return new ImageView(MOD_NATIVE);
		}
		if (Access.isStatic(access)) {
			return new ImageView(MOD_STATIC);
		}
		if (Access.isSynthetic(access)) {
			return new ImageView(MOD_SYNTHETIC);
		}
		if (Access.isTransient(access)) {
			return new ImageView(MOD_TRANSIENT);
		}
		if (Access.isNative(access)) {
			return new ImageView(MOD_NATIVE);
		}
		if (Access.isBridge(access)) {
			if (context == AccessContext.METHOD) {
				return new ImageView(MOD_SYNTHETIC);
			} else {
				return new ImageView(MOD_VOLATILE);
			}
		}
		return new ImageView(F_DEFAULT);
	}

	/**
	 * Load icon from name.
	 * 
	 * @param name
	 *            File name.
	 * @return Image by name.
	 */
	private static Image load(String name) {
		try {
			String file = "resources/icons/" + name + ".png";
			URL url = Thread.currentThread().getContextClassLoader().getResource(file);
			return new Image(url.openStream());
		} catch (Exception e) {
			Logging.fatal(e);
			return null;
		}
	}
}