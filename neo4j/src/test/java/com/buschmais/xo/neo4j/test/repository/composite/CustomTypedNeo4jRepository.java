package com.buschmais.xo.neo4j.test.repository.composite;

import static com.buschmais.xo.api.annotation.ResultOf.Parameter;

import com.buschmais.xo.api.XOManager;
import com.buschmais.xo.api.annotation.ImplementedBy;
import com.buschmais.xo.api.annotation.Repository;
import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.api.proxy.ProxyMethod;
import com.buschmais.xo.neo4j.api.Neo4jRepository;
import com.buschmais.xo.neo4j.api.TypedNeo4jRepository;
import com.buschmais.xo.neo4j.api.annotation.Cypher;

@Repository
public interface CustomTypedNeo4jRepository extends TypedNeo4jRepository<A> {

    @ResultOf
    @Cypher("match (a) where a.name={name} return a")
    A findByName(@Parameter("name") String name);

}
