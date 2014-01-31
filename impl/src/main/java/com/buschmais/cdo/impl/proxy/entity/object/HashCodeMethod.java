package com.buschmais.cdo.impl.proxy.entity.object;

import com.buschmais.cdo.impl.SessionContext;
import com.buschmais.cdo.api.proxy.ProxyMethod;

public class HashCodeMethod<Entity> implements ProxyMethod<Entity> {

    private SessionContext<?, Entity, ?, ?, ?, ?, ?, ?> sessionContext;

    public HashCodeMethod(SessionContext<?, Entity, ?, ?, ?, ?, ?, ?> sessionContext) {
        this.sessionContext = sessionContext;
    }

    @Override
    public Object invoke(Entity entity, Object instance, Object[] args) {
        return sessionContext.getDatastoreSession().getId(entity).hashCode();
    }
}