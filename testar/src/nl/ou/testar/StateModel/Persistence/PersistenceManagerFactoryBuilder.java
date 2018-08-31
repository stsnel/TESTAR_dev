package nl.ou.testar.StateModel.Persistence;

import nl.ou.testar.StateModel.Persistence.OrientDB.DummyManagerFactory;
import nl.ou.testar.StateModel.Persistence.OrientDB.OrientDBManagerFactory;

/**
 * This class creates concrete factory classes following the abstract factory pattern.
 */
public abstract class PersistenceManagerFactoryBuilder {

    public enum ManagerType {ORIENTDB, DUMMY}

    /**
     * This method returns a concrete implementation of the abstract PersistManagerFactory class/interface.
     * @param managerType
     * @return
     */
    public static PersistenceManagerFactory createPersistenceManagerFactory(ManagerType managerType) {
        switch (managerType) {
            case ORIENTDB:
                return new OrientDBManagerFactory();

            case DUMMY:
            default:
                return new DummyManagerFactory();
        }
    }

}
