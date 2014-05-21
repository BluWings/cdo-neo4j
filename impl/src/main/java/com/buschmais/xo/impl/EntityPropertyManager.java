package com.buschmais.xo.impl;

import com.buschmais.xo.api.XOException;
import com.buschmais.xo.spi.datastore.DatastoreRelationManager;
import com.buschmais.xo.spi.datastore.DatastoreRelationMetadata;
import com.buschmais.xo.spi.metadata.method.*;
import com.buschmais.xo.spi.metadata.type.RelationTypeMetadata;

import java.util.Iterator;

/**
 * Contains methods for reading and creating relationships specified by the given metadata.
 * <p/>
 * <p>For each provided method the direction of the relationships is handled transparently for the caller.</p>
 */
public class EntityPropertyManager<Entity, Relation> extends AbstractPropertyManager<Entity, Entity, Relation> {

    /**
     * Constructor.
     *
     * @param sessionContext The {@link SessionContext}.
     */
    public EntityPropertyManager(SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?> sessionContext) {
        super(sessionContext);
    }

    @Override
    public void setProperty(Entity entity, PrimitivePropertyMethodMetadata metadata, Object value) {
        SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?> sessionContext = getSessionContext();
        sessionContext.getDatastoreSession().getDatastoreEntityManager().setProperty(entity, metadata, value);
        sessionContext.getEntityInstanceManager().updateInstance(entity);
    }

    @Override
    public Object getProperty(Entity entity, PrimitivePropertyMethodMetadata metadata) {
        return getSessionContext().getDatastoreSession().getDatastoreEntityManager().getProperty(entity, metadata);
    }

    @Override
    public boolean hasProperty(Entity entity, PrimitivePropertyMethodMetadata metadata) {
        return getSessionContext().getDatastoreSession().getDatastoreEntityManager().hasProperty(entity, metadata);
    }

    @Override
    public void removeProperty(Entity entity, PrimitivePropertyMethodMetadata metadata) {
        SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?> sessionContext = getSessionContext();
        sessionContext.getDatastoreSession().getDatastoreEntityManager().removeProperty(entity, metadata);
        sessionContext.getEntityInstanceManager().updateInstance(entity);
    }

    public <T> T createEntityReference(Entity sourceEntity, AbstractRelationPropertyMethodMetadata<?> metadata, Object target) {
        AbstractInstanceManager<?, Entity> instanceManager = getSessionContext().getEntityInstanceManager();
        Entity targetEntity = target != null ? instanceManager.getDatastoreType(target) : null;
        Relation relation = createRelation(sourceEntity, metadata, targetEntity, null);
        return relation != null ? (T) instanceManager.updateInstance(getReferencedEntity(relation, metadata.getDirection())) : null;
    }

    public <T> T createRelationReference(Entity sourceEntity, AbstractRelationPropertyMethodMetadata<?> fromProperty, Object target, AbstractRelationPropertyMethodMetadata<?> toProperty) {
        SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?> sessionContext = getSessionContext();
        AbstractInstanceManager<?, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        if (target != null) {
            Entity targetEntity = entityInstanceManager.getDatastoreType(target);
            Relation relation = createRelation(sourceEntity, fromProperty, targetEntity, toProperty);
            entityInstanceManager.updateInstance(targetEntity);
            T instance = sessionContext.getRelationInstanceManager().createInstance(relation);
            return instance;
        }
        return null;
    }

    public Object getEntityReference(Entity entity, EntityReferencePropertyMethodMetadata metadata) {
        Relation singleRelation = getSingleRelation(entity, metadata.getRelationshipMetadata(), metadata.getDirection());
        if (singleRelation != null) {
            Entity target = getReferencedEntity(singleRelation, metadata.getDirection());
            return getSessionContext().getEntityInstanceManager().readInstance(target);
        }
        return null;
    }

    public Iterator<Entity> getEntityCollection(Entity entity, final EntityCollectionPropertyMethodMetadata<?> metadata) {
        Iterable<Relation> relations = getSessionContext().getDatastoreSession().getDatastoreRelationManager().getRelations(entity, metadata.getRelationshipMetadata(), metadata.getDirection());
        final Iterator<Relation> iterator = relations.iterator();
        return new Iterator<Entity>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Entity next() {
                Relation next = iterator.next();
                return getReferencedEntity(next, metadata.getDirection());
            }

            @Override
            public void remove() {
            }
        };
    }

    public Object getRelationReference(Entity entity, RelationReferencePropertyMethodMetadata<?> metadata) {
        Relation singleRelation = getSingleRelation(entity, metadata.getRelationshipMetadata(), metadata.getDirection());
        if (singleRelation != null) {
            return getSessionContext().getRelationInstanceManager().readInstance(singleRelation);
        }
        return null;
    }

    public Iterator<Relation> getRelationCollection(Entity entity, RelationCollectionPropertyMethodMetadata<?> metadata) {
        Iterable<Relation> relations = getSessionContext().getDatastoreSession().getDatastoreRelationManager().getRelations(entity, metadata.getRelationshipMetadata(), metadata.getDirection());
        return relations.iterator();
    }

    public void removeEntityReferences(Entity entity, EntityCollectionPropertyMethodMetadata metadata) {
        Iterable<Relation> relations = getSessionContext().getDatastoreSession().getDatastoreRelationManager().getRelations(entity, metadata.getRelationshipMetadata(), metadata.getDirection());
        for (Relation relation : relations) {
            removeRelation(entity, relation, metadata);
        }
    }

    public boolean removeEntityReference(Entity entity, EntityCollectionPropertyMethodMetadata<?> metadata, Object target) {
        Iterable<Relation> relations = getSessionContext().getDatastoreSession().getDatastoreRelationManager().getRelations(entity, metadata.getRelationshipMetadata(), metadata.getDirection());
        Entity targetEntity = getSessionContext().getEntityInstanceManager().getDatastoreType(target);
        for (Relation relation : relations) {
            Entity referencedEntity = getReferencedEntity(relation, metadata.getDirection());
            if (referencedEntity.equals(targetEntity)) {
                removeRelation(entity, relation, metadata);
                return true;
            }
        }
        return false;
    }

    private void removeRelation(Entity source, Relation relation, AbstractRelationPropertyMethodMetadata<?> metadata) {
        SessionContext<?, Entity, ?, ?, ?, Relation, ?, ?> sessionContext = getSessionContext();
        AbstractInstanceManager<?, Entity> entityInstanceManager = sessionContext.getEntityInstanceManager();
        entityInstanceManager.updateInstance(source);
        entityInstanceManager.updateInstance(getReferencedEntity(relation, metadata.getDirection()));
        sessionContext.getDatastoreSession().getDatastoreRelationManager().deleteRelation(relation);
        AbstractInstanceManager<?, Relation> relationInstanceManager = getSessionContext().getRelationInstanceManager();
        if (metadata.getRelationshipMetadata().getAnnotatedType() != null) {
            Object instance = relationInstanceManager.readInstance(relation);
            relationInstanceManager.removeInstance(instance);
            relationInstanceManager.closeInstance(instance);
        }

    }

    private Entity getReferencedEntity(Relation relation, RelationTypeMetadata.Direction direction) {
        DatastoreRelationManager<Entity, ?, Relation, ? extends DatastoreRelationMetadata<?>, ?, ?> relationManager = getSessionContext().getDatastoreSession().getDatastoreRelationManager();
        switch (direction) {
            case FROM:
                return relationManager.getTo(relation);
            case TO:
                return relationManager.getFrom(relation);
            default:
                throw new XOException("Unsupported direction: " + direction);
        }
    }

    private Relation createRelation(Entity sourceEntity, AbstractRelationPropertyMethodMetadata<?> fromProperty, Entity targetEntity, AbstractRelationPropertyMethodMetadata<?> toProperty) {
        Relation relation;
        if (fromProperty instanceof EntityReferencePropertyMethodMetadata || fromProperty instanceof RelationReferencePropertyMethodMetadata) {
            relation = createSingleReference(sourceEntity, fromProperty, targetEntity);
        } else if (toProperty instanceof EntityReferencePropertyMethodMetadata || toProperty instanceof RelationReferencePropertyMethodMetadata) {
            relation = createSingleReference(targetEntity, toProperty, sourceEntity);
        } else if (fromProperty instanceof EntityCollectionPropertyMethodMetadata || fromProperty instanceof RelationCollectionPropertyMethodMetadata) {
            relation = createReference(sourceEntity, fromProperty.getRelationshipMetadata(), fromProperty.getDirection(), targetEntity);
        } else {
            throw new XOException("Unsupported relation type " + fromProperty.getClass().getName());
        }
        return relation;
    }

    private Relation createSingleReference(Entity sourceEntity, AbstractRelationPropertyMethodMetadata<?> metadata, Entity targetEntity) {
        DatastoreRelationManager<Entity, ?, Relation, ? extends DatastoreRelationMetadata<?>, ?, ?> relationManager = getSessionContext().getDatastoreSession().getDatastoreRelationManager();
        if (relationManager.hasSingleRelation(sourceEntity, metadata.getRelationshipMetadata(), metadata.getDirection())) {
            Relation relation = relationManager.getSingleRelation(sourceEntity, metadata.getRelationshipMetadata(), metadata.getDirection());
            removeRelation(sourceEntity, relation, metadata);
        }
        return targetEntity != null ? relationManager.createRelation(sourceEntity, metadata.getRelationshipMetadata(), metadata.getDirection(), targetEntity) : null;
    }

    private Relation createReference(Entity sourceEntity, RelationTypeMetadata metadata, RelationTypeMetadata.Direction direction, Entity targetEntity) {
        return getSessionContext().getDatastoreSession().getDatastoreRelationManager().createRelation(sourceEntity, metadata, direction, targetEntity);
    }
}
