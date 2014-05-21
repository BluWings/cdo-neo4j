package com.buschmais.xo.test.trace.impl;

import com.buschmais.xo.api.ResultIterator;
import com.buschmais.xo.spi.datastore.DatastoreEntityManager;
import com.buschmais.xo.spi.datastore.DatastoreEntityMetadata;
import com.buschmais.xo.spi.datastore.TypeMetadataSet;
import com.buschmais.xo.spi.metadata.method.PrimitivePropertyMethodMetadata;
import com.buschmais.xo.spi.metadata.type.EntityTypeMetadata;

import java.util.Set;

/**
 * Created by dimahler on 5/21/2014.
 */
public class TraceDatastoreEntityManager<EntityId, Entity, EntityMetadata extends DatastoreEntityMetadata<EntityDiscriminator>, EntityDiscriminator, PrimitivePropertyMeta> implements DatastoreEntityManager<EntityId, Entity, EntityMetadata, EntityDiscriminator, PrimitivePropertyMeta> {

    private DatastoreEntityManager<EntityId, Entity, EntityMetadata, EntityDiscriminator, PrimitivePropertyMeta> delegate;

    public TraceDatastoreEntityManager(DatastoreEntityManager<EntityId, Entity, EntityMetadata, EntityDiscriminator, PrimitivePropertyMeta> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isEntity(Object o) {
        return delegate.isEntity(o);
    }

    public Set<EntityDiscriminator> getEntityDiscriminators(Entity entity) {
        return delegate.getEntityDiscriminators(entity);
    }

    public EntityId getEntityId(Entity entity) {
        return delegate.getEntityId(entity);
    }

    public Entity createEntity(TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> types, Set<EntityDiscriminator> entityDiscriminators) {
        return delegate.createEntity(types, entityDiscriminators);
    }

    public void deleteEntity(Entity entity) {
        delegate.deleteEntity(entity);
    }

    public ResultIterator<Entity> findEntity(EntityTypeMetadata<EntityMetadata> type, EntityDiscriminator entityDiscriminator, Object value) {
        return delegate.findEntity(type, entityDiscriminator, value);
    }

    public void migrateEntity(Entity entity, TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> types, Set<EntityDiscriminator> entityDiscriminators, TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> targetTypes, Set<EntityDiscriminator> targetDiscriminators) {
        delegate.migrateEntity(entity, types, entityDiscriminators, targetTypes, targetDiscriminators);
    }

    public void flushEntity(Entity entity) {
        delegate.flushEntity(entity);
    }

    public void setProperty(Entity entity, PrimitivePropertyMethodMetadata<PrimitivePropertyMeta> metadata, Object value) {
        delegate.setProperty(entity, metadata, value);
    }

    public boolean hasProperty(Entity entity, PrimitivePropertyMethodMetadata<PrimitivePropertyMeta> metadata) {
        return delegate.hasProperty(entity, metadata);
    }

    public void removeProperty(Entity entity, PrimitivePropertyMethodMetadata<PrimitivePropertyMeta> metadata) {
        delegate.removeProperty(entity, metadata);
    }

    public Object getProperty(Entity entity, PrimitivePropertyMethodMetadata<PrimitivePropertyMeta> metadata) {
        return delegate.getProperty(entity, metadata);
    }
}
