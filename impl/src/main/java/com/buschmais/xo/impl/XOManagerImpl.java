package com.buschmais.xo.impl;

import com.buschmais.xo.api.*;
import com.buschmais.xo.impl.query.XOQueryImpl;
import com.buschmais.xo.impl.transaction.TransactionalResultIterable;
import com.buschmais.xo.spi.datastore.DatastoreEntityMetadata;
import com.buschmais.xo.spi.datastore.DatastoreRelationMetadata;
import com.buschmais.xo.spi.datastore.DatastoreSession;
import com.buschmais.xo.spi.datastore.TypeMetadataSet;
import com.buschmais.xo.spi.metadata.method.AbstractRelationPropertyMethodMetadata;
import com.buschmais.xo.spi.metadata.type.EntityTypeMetadata;

import javax.validation.ConstraintViolation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static com.buschmais.xo.api.Query.Result.CompositeRowObject;
import static com.buschmais.xo.spi.metadata.type.RelationTypeMetadata.Direction.FROM;
import static com.buschmais.xo.spi.metadata.type.RelationTypeMetadata.Direction.TO;

/**
 * Generic implementation of a {@link com.buschmais.xo.api.XOManager}.
 *
 * @param <EntityId>              The type of entity ids as provided by the datastore.
 * @param <Entity>                The type entities as provided by the datastore.
 * @param <EntityMetadata>        The type of entity metadata as provided by the datastore.
 * @param <EntityDiscriminator>   The type of discriminators as provided by the datastore.
 * @param <RelationId>            The type of relation ids as provided by the datastore.
 * @param <Relation>              The type of relations as provided by the datastore.
 * @param <RelationMetadata>      The type of relation metadata as provided by the datastore.
 * @param <RelationDiscriminator> The type of relation discriminators as provided by the datastore.
 */
public class XOManagerImpl<EntityId, Entity, EntityMetadata extends DatastoreEntityMetadata<EntityDiscriminator>, EntityDiscriminator, RelationId, Relation, RelationMetadata extends DatastoreRelationMetadata<RelationDiscriminator>, RelationDiscriminator> implements XOManager {

    private final SessionContext<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> sessionContext;

    /**
     * Constructor.
     *
     * @param sessionContext The associated {@link SessionContext}.
     */
    public XOManagerImpl(SessionContext<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> sessionContext) {
        this.sessionContext = sessionContext;
    }

    @Override
    public XOTransaction currentTransaction() {
        return sessionContext.getXOTransaction();
    }

    @Override
    public Set<ConstraintViolation<Object>> validate() {
        return sessionContext.getInstanceValidationService().validate();
    }

    @Override
    public <T> ResultIterable<T> find(final Class<T> type, final Object value) {
        EntityTypeMetadata<EntityMetadata> entityTypeMetadata = sessionContext.getMetadataProvider().getEntityMetadata(type);
        EntityDiscriminator entityDiscriminator = entityTypeMetadata.getDatastoreMetadata().getDiscriminator();
        if (entityDiscriminator == null) {
            throw new XOException("Type " + type.getName() + " has no discriminator (i.e. cannot be identified in datastore).");
        }
        final ResultIterator<Entity> iterator = sessionContext.getDatastoreSession().findEntity(entityTypeMetadata, entityDiscriminator, value);
        return new TransactionalResultIterable<>(new AbstractResultIterable<T>() {
            @Override
            public ResultIterator<T> iterator() {
                return new ResultIterator<T>() {

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        Entity entity = iterator.next();
                        AbstractInstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
                        return entityInstanceManager.readInstance(entity);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Cannot remove instance.");
                    }

                    @Override
                    public void close() {
                        iterator.close();
                    }
                };
            }
        }, sessionContext.getXOTransaction());
    }

    @Override
    public CompositeObject create(Class type, Class<?>... types) {
        TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> effectiveTypes = getEffectiveTypes(type, types);
        Set<EntityDiscriminator> entityDiscriminators = sessionContext.getMetadataProvider().getEntityDiscriminators(effectiveTypes);
        DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> datastoreSession = sessionContext.getDatastoreSession();
        Entity entity = datastoreSession.createEntity(effectiveTypes, entityDiscriminators);
        AbstractInstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        CompositeObject instance = entityInstanceManager.createInstance(entity);
        sessionContext.getInstanceListenerService().postCreate(instance);
        return instance;
    }

    public <T> T create(Class<T> type) {
        return create(type, new Class<?>[0]).as(type);
    }

    @Override
    public <S, R, T> R create(S from, Class<R> relationType, T to) {
        MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataProvider = sessionContext.getMetadataProvider();
        AbstractRelationPropertyMethodMetadata<?> fromProperty = metadataProvider.getPropertyMetadata(from.getClass(), relationType, FROM);
        AbstractRelationPropertyMethodMetadata<?> toProperty = metadataProvider.getPropertyMetadata(to.getClass(), relationType, TO);
        Entity entity = sessionContext.getEntityInstanceManager().getDatastoreType(from);
        R instance = sessionContext.getEntityPropertyManager().createRelationReference(entity, fromProperty, to, toProperty);
        sessionContext.getInstanceListenerService().postCreate(instance);
        return instance;
    }

    @Override
    public <T, Id> Id getId(T instance) {
        AbstractInstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        AbstractInstanceManager<RelationId, Relation> relationInstanceManager = sessionContext.getRelationInstanceManager();
        if (entityInstanceManager.isInstance(instance)) {
            Entity entity = entityInstanceManager.getDatastoreType(instance);
            return (Id) sessionContext.getDatastoreSession().getEntityId(entity);
        } else if (relationInstanceManager.isInstance(instance)) {
            Relation relation = relationInstanceManager.getDatastoreType(instance);
            return (Id) sessionContext.getDatastoreSession().getRelationId(relation);
        }
        throw new XOException(instance + " is not a managed XO instance.");
    }

    @Override
    public <T, M> CompositeObject migrate(T instance, MigrationStrategy<T, M> migrationStrategy, Class<M> targetType, Class<?>... targetTypes) {
        AbstractInstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        Entity entity = entityInstanceManager.getDatastoreType(instance);
        DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> datastoreSession = sessionContext.getDatastoreSession();
        Set<EntityDiscriminator> entityDiscriminators = datastoreSession.getEntityDiscriminators(entity);
        MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataProvider = sessionContext.getMetadataProvider();
        TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> types = metadataProvider.getTypes(entityDiscriminators);
        TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> effectiveTargetTypes = getEffectiveTypes(targetType, targetTypes);
        Set<EntityDiscriminator> targetEntityDiscriminators = metadataProvider.getEntityDiscriminators(effectiveTargetTypes);
        datastoreSession.migrateEntity(entity, types, entityDiscriminators, effectiveTargetTypes, targetEntityDiscriminators);
        entityInstanceManager.removeInstance(instance);
        CompositeObject migratedInstance = entityInstanceManager.updateInstance(entity);
        if (migrationStrategy != null) {
            migrationStrategy.migrate(instance, migratedInstance.as(targetType));
        }
        entityInstanceManager.closeInstance(instance);
        return migratedInstance;
    }

    @Override
    public <T, M> CompositeObject migrate(T instance, Class<M> targetType, Class<?>... targetTypes) {
        return migrate(instance, null, targetType, targetTypes);
    }

    @Override
    public <T, M> M migrate(T instance, MigrationStrategy<T, M> migrationStrategy, Class<M> targetType) {
        return migrate(instance, migrationStrategy, targetType, new Class<?>[0]).as(targetType);
    }

    @Override
    public <T, M> M migrate(T instance, Class<M> targetType) {
        return migrate(instance, null, targetType);
    }

    @Override
    public <T> void delete(T instance) {
        AbstractInstanceManager<EntityId, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        AbstractInstanceManager<RelationId, Relation> relationInstanceManager = sessionContext.getRelationInstanceManager();
        DatastoreSession<EntityId, Entity, EntityMetadata, EntityDiscriminator, RelationId, Relation, RelationMetadata, RelationDiscriminator> datastoreSession = sessionContext.getDatastoreSession();
        if (entityInstanceManager.isInstance(instance)) {
            Entity entity = entityInstanceManager.getDatastoreType(instance);
            sessionContext.getInstanceListenerService().preDelete(instance);
            datastoreSession.deleteEntity(entity);
            entityInstanceManager.removeInstance(instance);
            entityInstanceManager.closeInstance(instance);
            sessionContext.getInstanceListenerService().postDelete(instance);
        } else if (relationInstanceManager.isInstance(instance)) {
            Relation relation = relationInstanceManager.getDatastoreType(instance);
            sessionContext.getInstanceListenerService().preDelete(instance);
            datastoreSession.getDatastorePropertyManager().deleteRelation(relation);
            relationInstanceManager.removeInstance(instance);
            relationInstanceManager.closeInstance(instance);
            sessionContext.getInstanceListenerService().postDelete(instance);
        } else {
            throw new XOException(instance + " is not a managed XO instance.");
        }
    }

    @Override
    public Query<CompositeRowObject> createQuery(String query) {
        XOQueryImpl<CompositeRowObject, String, Entity, Relation> xoQuery = new XOQueryImpl<>(sessionContext, query, CompositeRowObject.class, Collections.<Class<?>>emptyList());
        return sessionContext.getInterceptorFactory().addInterceptor(xoQuery);
    }

    @Override
    public <T> Query<T> createQuery(String query, Class<T> type) {
        XOQueryImpl<T, String, Entity, Relation> xoQuery = new XOQueryImpl<>(sessionContext, query, type, Arrays.asList(type));
        return sessionContext.getInterceptorFactory().addInterceptor(xoQuery);
    }

    @Override
    public Query<CompositeRowObject> createQuery(String query, Class<?> type, Class<?>... types) {
        XOQueryImpl<CompositeRowObject, String, Entity, Relation> xoQuery = new XOQueryImpl<>(sessionContext, query, type, Arrays.asList(types));
        return sessionContext.getInterceptorFactory().addInterceptor(xoQuery);
    }

    @Override
    public <T> Query<T> createQuery(Class<T> query) {
        XOQueryImpl<T, Class<T>, Entity, Relation> xoQuery = new XOQueryImpl<>(sessionContext, query, query, Arrays.asList(query));
        return sessionContext.getInterceptorFactory().addInterceptor(xoQuery);
    }

    @Override
    public <Q> Query<CompositeRowObject> createQuery(Class<Q> query, Class<?>... types) {
        XOQueryImpl<CompositeRowObject, Class<Q>, Entity, Relation> xoQuery = new XOQueryImpl<>(sessionContext, query, query, Arrays.asList(types));
        return sessionContext.getInterceptorFactory().addInterceptor(xoQuery);
    }

    @Override
    public void close() {
        sessionContext.getEntityInstanceManager().close();
        sessionContext.getRelationInstanceManager().close();
    }

    @Override
    public <DS> DS getDatastoreSession(Class<DS> sessionType) {
        return sessionType.cast(sessionContext.getDatastoreSession());
    }

    @Override
    public void flush() {
        sessionContext.getCacheSynchronizationService().flush();
    }

    @Override
    public <I> void registerInstanceListener(I instanceListener) {
        sessionContext.getInstanceListenerService().registerInstanceListener(instanceListener);
    }

    private TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> getEffectiveTypes(Class<?> type, Class<?>... types) {
        MetadataProvider<EntityMetadata, EntityDiscriminator, RelationMetadata, RelationDiscriminator> metadataProvider = sessionContext.getMetadataProvider();
        TypeMetadataSet<EntityTypeMetadata<EntityMetadata>> effectiveTypes = new TypeMetadataSet<>();
        effectiveTypes.add(metadataProvider.getEntityMetadata(type));
        for (Class<?> otherType : types) {
            effectiveTypes.add(metadataProvider.getEntityMetadata(otherType));
        }
        return effectiveTypes;
    }
}
