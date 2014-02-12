package com.buschmais.cdo.neo4j.test.embedded.implementedby.composite;

import com.buschmais.cdo.api.annotation.ImplementedBy;
import com.buschmais.cdo.neo4j.api.annotation.Relation;

import static com.buschmais.cdo.neo4j.api.annotation.Relation.Incoming;
import static com.buschmais.cdo.neo4j.api.annotation.Relation.Outgoing;

@Relation("A2B")
public interface A2B {

    @Outgoing
    A getA();

    @Incoming
    B getB();

    int getValue();

    void setValue(int i);

    @ImplementedBy(SetMethod.class)
    void setCustomValue(String usingHandler);

    @ImplementedBy(GetMethod.class)
    String getCustomValue();

    @ImplementedBy(RelationIncrementValueMethod.class)
    int incrementValue();

    void unsupportedOperation();
}