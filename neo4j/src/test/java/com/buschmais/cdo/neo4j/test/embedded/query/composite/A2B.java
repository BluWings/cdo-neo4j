package com.buschmais.cdo.neo4j.test.embedded.query.composite;

import com.buschmais.cdo.neo4j.api.annotation.Relation;

import static com.buschmais.cdo.neo4j.api.annotation.Relation.Incoming;
import static com.buschmais.cdo.neo4j.api.annotation.Relation.Outgoing;

@Relation("A2B")
public interface A2B {

    @Outgoing
    A getA();

    @Incoming
    B getB();

}
