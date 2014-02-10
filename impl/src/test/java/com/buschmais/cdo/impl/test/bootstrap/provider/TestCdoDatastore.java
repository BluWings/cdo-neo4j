package com.buschmais.cdo.impl.test.bootstrap.provider;

import com.buschmais.cdo.api.bootstrap.CdoUnit;
import com.buschmais.cdo.impl.test.bootstrap.provider.metadata.TestEntityMetadata;
import com.buschmais.cdo.impl.test.bootstrap.provider.metadata.TestRelationMetadata;
import com.buschmais.cdo.spi.datastore.Datastore;
import com.buschmais.cdo.spi.datastore.DatastoreMetadataFactory;
import com.buschmais.cdo.spi.datastore.DatastoreSession;
import com.buschmais.cdo.spi.metadata.type.RelationTypeMetadata;
import com.buschmais.cdo.spi.metadata.type.TypeMetadata;
import com.buschmais.cdo.spi.reflection.AnnotatedElement;
import com.buschmais.cdo.spi.reflection.AnnotatedMethod;
import com.buschmais.cdo.spi.reflection.AnnotatedType;
import com.buschmais.cdo.spi.reflection.PropertyMethod;

import java.util.Collection;
import java.util.Map;

public class TestCdoDatastore<D extends DatastoreSession> implements Datastore<D, TestEntityMetadata, String, TestRelationMetadata, String> {

    private final CdoUnit cdoUnit;

    public TestCdoDatastore(CdoUnit cdoUnit) {
        this.cdoUnit = cdoUnit;
    }

    @Override
    public DatastoreMetadataFactory<TestEntityMetadata, String, TestRelationMetadata, String> getMetadataFactory() {
        return new DatastoreMetadataFactory<TestEntityMetadata, String, TestRelationMetadata, String>() {
            @Override
            public TestEntityMetadata createEntityMetadata(AnnotatedType annotatedType, Map<Class<?>, TypeMetadata> metadataByType) {
                return new TestEntityMetadata(annotatedType.getAnnotatedElement().getName());
            }

            @Override
            public <ImplementedByMetadata> ImplementedByMetadata createImplementedByMetadata(AnnotatedMethod annotatedMethod) {
                return null;
            }

            @Override
            public <CollectionPropertyMetadata> CollectionPropertyMetadata createCollectionPropertyMetadata(PropertyMethod propertyMethod) {
                return null;
            }

            @Override
            public <ReferencePropertyMetadata> ReferencePropertyMetadata createReferencePropertyMetadata(PropertyMethod propertyMethod) {
                return null;
            }

            @Override
            public <PrimitivePropertyMetadata> PrimitivePropertyMetadata createPrimitivePropertyMetadata(PropertyMethod propertyMethod) {
                return null;
            }

            @Override
            public <IndexedPropertyMetadata> IndexedPropertyMetadata createIndexedPropertyMetadata(PropertyMethod propertyMethod) {
                return null;
            }

            @Override
            public TestRelationMetadata createRelationMetadata(AnnotatedElement<?> annotatedElement, Map<Class<?>, TypeMetadata> metadataByType) {
                return null;
            }

            @Override
            public RelationTypeMetadata.Direction getRelationDirection(PropertyMethod propertyMethod) {
                return null;
            }
        };
    }

    @Override
    public D createSession() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public void init(Collection<TypeMetadata> registeredMetadata) {
    }

    public CdoUnit getCdoUnit() {
        return cdoUnit;
    }
}
