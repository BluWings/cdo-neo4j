package com.buschmais.xo.api.bootstrap;

import com.buschmais.xo.api.XOException;
import com.buschmais.xo.api.XOManagerFactory;

import java.util.ServiceLoader;

/**
 * Provides methods for bootstrapping XO.
 */
public final class XO {

    private XO() {
    }

    /**
     * Create a {@link com.buschmais.xo.api.XOManagerFactory} for the XO unit identified by name.
     * <p>XO units are defined in XML descriptors located as classpath resources with the name "/META-INF/xo.xml".</p>
     *
     * @param name The name of the XO unit.
     * @return The {@link com.buschmais.xo.api.XOManagerFactory}.
     */
    public static XOManagerFactory createXOManagerFactory(String name) {
        ServiceLoader<XOBootstrapService> serviceLoader = ServiceLoader.load(XOBootstrapService.class);
        for (XOBootstrapService xoBootstrapService : serviceLoader) {
            XOManagerFactory xoManagerFactory = xoBootstrapService.createXOManagerFactory(name);
            if (xoManagerFactory != null) {
                return xoManagerFactory;
            }
        }
        throw new XOException("Cannot bootstrap XO implementation.");
    }

    /**
     * Create a {@link com.buschmais.xo.api.XOManagerFactory} for the given XO unit.
     *
     * @param xoUnit The XO unit.
     * @return The {@link com.buschmais.xo.api.XOManagerFactory}.
     */
    public static XOManagerFactory createXOManagerFactory(XOUnit xoUnit) {
        ServiceLoader<XOBootstrapService> serviceLoader = ServiceLoader.load(XOBootstrapService.class);
        for (XOBootstrapService xoBootstrapService : serviceLoader) {
            XOManagerFactory xoManagerFactory = xoBootstrapService.createXOManagerFactory(xoUnit);
            if (xoManagerFactory != null) {
                return xoManagerFactory;
            }
        }
        throw new XOException("Cannot bootstrap XO implementation.");
    }

}
