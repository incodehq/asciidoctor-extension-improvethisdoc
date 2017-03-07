package org.incode.asciidoctor.extensions.inclcontrib;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.asciidoctor.extension.spi.ExtensionRegistry;

public class IncludeWithContributeExtension implements ExtensionRegistry {

	@Override
	public void register(Asciidoctor asciidoctor) {
		JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
		javaExtensionRegistry.postprocessor(IncludeWithContributePostprocessor.class);
	}
}
