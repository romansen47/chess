package demo.chess.init;

import template.annotation.initializer.SpringAnnotationContextInitializer;

public class Initializer extends SpringAnnotationContextInitializer {

	public static final String BASE_PACKAGES = "demo..*";

	/**
	 * Returns the base packages.
	 * @return the base packages
	 */
	@Override
	public String getBasePackages() {
		return BASE_PACKAGES;
	}
}