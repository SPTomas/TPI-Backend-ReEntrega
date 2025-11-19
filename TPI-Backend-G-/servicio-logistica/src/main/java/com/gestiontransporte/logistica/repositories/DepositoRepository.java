package com.gestiontransporte.logistica.repositories;

import com.gestiontransporte.logistica.models.Deposito;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DepositoRepository extends JpaRepository<Deposito, Long> {}