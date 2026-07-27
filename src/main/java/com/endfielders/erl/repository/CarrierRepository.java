package com.endfielders.erl.repository;

import com.endfielders.erl.model.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {

    List<Carrier> findByActiveStatusTrue();

    List<Carrier> findByModeAndActiveStatusTrue(String mode);

    List<Carrier> findByCategoryInAndActiveStatusTrue(List<String> categories);
}
