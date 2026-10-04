package com.sigecin.business.repository;

import com.sigecin.business.entity.BusinessCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, Integer> {

    List<BusinessCategory> findAllByOrderByNameAsc();
}
