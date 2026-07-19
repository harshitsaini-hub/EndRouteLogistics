package com.endfielders.erl.repository;

import com.endfielders.erl.model.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, Long> {

    List<Carrier> findByActiveStatusTrue();

    List<Carrier> findByModeAndActiveStatusTrue(String mode);
}
