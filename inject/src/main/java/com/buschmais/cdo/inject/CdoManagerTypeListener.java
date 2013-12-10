package com.buschmais.cdo.inject;

import java.lang.reflect.Field;
import java.net.MalformedURLException;

import com.buschmais.cdo.impl.AbstractCdoManagerFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buschmais.cdo.api.CdoManagerFactory;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class CdoManagerTypeListener implements TypeListener {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(AbstractCdoManagerFactoryImpl.class);

	@Override
	public <T> void hear(TypeLiteral<T> literal, TypeEncounter<T> encounter) {

		for (Field field : literal.getRawType().getDeclaredFields()) {
			if (field.getType() == CdoManagerFactory.class
					&& field.isAnnotationPresent(CdoContext.class)) {
				CdoContext context = field.getAnnotation(CdoContext.class);
				try {
					encounter.register(new CdoManagerFactoryInjector<T>(field,
							context.unit()));
				} catch (MalformedURLException e) {
					LOGGER.error("", e);
				}
			}
		}
	}
}
