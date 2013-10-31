package org.energyos.espi.thirdparty.repository.impl;

import org.energyos.espi.thirdparty.domain.BatchList;
import org.energyos.espi.thirdparty.repository.BatchListRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
@Transactional
public class BatchListRepositoryImpl implements BatchListRepository {

    @PersistenceContext
    protected EntityManager em;

    @Override
    public void persist(BatchList batchList) {
        em.persist(batchList);
    }

    @Override
    public List<BatchList> findAll() {
        String selectAll = "select list from BatchList list";
        return (List<BatchList>)this.em.createQuery(selectAll).getResultList();
    }
}
