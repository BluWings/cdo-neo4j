package com.buschmais.xo.neo4j.test.migration;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.buschmais.xo.api.CompositeObject;
import com.buschmais.xo.api.XOManager;
import com.buschmais.xo.api.bootstrap.XOUnit;
import com.buschmais.xo.neo4j.test.AbstractNeo4jXOManagerTest;
import com.buschmais.xo.neo4j.test.migration.composite.A;
import com.buschmais.xo.neo4j.test.migration.composite.B;
import com.buschmais.xo.neo4j.test.migration.composite.C;
import com.buschmais.xo.neo4j.test.migration.composite.D;

@RunWith(Parameterized.class)
public class MigrationTest extends AbstractNeo4jXOManagerTest {

    public MigrationTest(XOUnit xoUnit) {
        super(xoUnit);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getXOUnits() throws URISyntaxException {
        return xoUnits(A.class, B.class, C.class, D.class);
    }

    @Test
    public void downCast() {
        XOManager xoManager = getXoManagerFactory().createXOManager();
        xoManager.currentTransaction().begin();
        A a = xoManager.create(A.class);
        a.setValue("Value");
        xoManager.currentTransaction().commit();
        xoManager.currentTransaction().begin();
        B b = xoManager.migrate(a, B.class);
        assertThat(a == b, equalTo(false));
        assertThat(b.getValue(), equalTo("Value"));
        xoManager.currentTransaction().commit();
        xoManager.close();
    }

    @Test
    public void compositeObject() {
        XOManager xoManager = getXoManagerFactory().createXOManager();
        xoManager.currentTransaction().begin();
        A a = xoManager.create(A.class);
        a.setValue("Value");
        xoManager.currentTransaction().commit();
        xoManager.currentTransaction().begin();
        B b = xoManager.migrate(a, B.class, D.class).as(B.class);
        assertThat(b.getValue(), equalTo("Value"));
        xoManager.currentTransaction().commit();
        xoManager.close();
    }

    @Test
    public void migrationHandler() {
        XOManager xoManager = getXoManagerFactory().createXOManager();
        xoManager.currentTransaction().begin();
        A a = xoManager.create(A.class);
        a.setValue("Value");
        xoManager.currentTransaction().commit();
        xoManager.currentTransaction().begin();
		C c = xoManager.migrate(a, (instance, target) -> target.setName(instance.getValue()), C.class);
        assertThat(c.getName(), equalTo("Value"));
        xoManager.currentTransaction().commit();
        xoManager.close();
    }

    @Test
    public void compositeObjectMigrationHandler() {
        XOManager xoManager = getXoManagerFactory().createXOManager();
        xoManager.currentTransaction().begin();
        A a = xoManager.create(A.class);
        a.setValue("Value");
        xoManager.currentTransaction().commit();
        xoManager.currentTransaction().begin();
		C c = xoManager.migrate(a, (instance, target) -> target.setName(instance.getValue()), C.class, D.class).as(C.class);
        assertThat(c.getName(), equalTo("Value"));
        xoManager.currentTransaction().commit();
        xoManager.close();
    }

    @Test
    public void addType() {
        XOManager xoManager = getXoManager();
        xoManager.currentTransaction().begin();
        A a = xoManager.create(A.class);
        a.setValue("Value");
		assertThat(a, instanceOf(A.class));
		assertThat(a, not(instanceOf(C.class)));
		CompositeObject compositeObject = xoManager.migrate(a).add(C.class);
		assertThat(compositeObject, instanceOf(A.class));
		assertThat(compositeObject, instanceOf(C.class));
		assertThat(compositeObject.as(A.class).getValue(), equalTo("Value"));
        xoManager.currentTransaction().commit();
        xoManager.close();
    }

    @Test
    public void removeType() {
        XOManager xoManager = getXoManager();
        xoManager.currentTransaction().begin();
		CompositeObject compositeObject = xoManager.create(A.class, C.class);
		assertThat(compositeObject, instanceOf(A.class));
		assertThat(compositeObject, instanceOf(C.class));
        compositeObject.as(A.class).setValue("Value");
		A a = xoManager.migrate(compositeObject).remove(C.class).as(A.class);
		assertThat(a, instanceOf(A.class));
		assertThat(a, not(instanceOf(C.class)));
        assertThat(a.getValue(), equalTo("Value"));
        xoManager.currentTransaction().commit();
        xoManager.close();
    }
}
