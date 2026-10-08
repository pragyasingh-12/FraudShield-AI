package com.fraudshield.dao;

import com.fraudshield.exception.DatabaseOperationException;
import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract implemented by every DAO (generics + interface + polymorphism).
 * @param <T> the model type handled by the DAO
 */
public interface DAOOperations<T> {
    /** Inserts the entity and returns the generated primary key. */
    int create(T entity) throws DatabaseOperationException;

    Optional<T> findById(int id) throws DatabaseOperationException;

    List<T> findAll() throws DatabaseOperationException;

    boolean update(T entity) throws DatabaseOperationException;

    boolean delete(int id) throws DatabaseOperationException;
}
