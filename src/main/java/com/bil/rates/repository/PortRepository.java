package com.bil.rates.repository;

import com.bil.rates.domain.Port;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PortRepository extends JpaRepository<Port, String> {

    @Query("""
            select p from Port p
            where upper(p.unloc) like upper(concat('%', :q, '%'))
               or upper(p.name)  like upper(concat('%', :q, '%'))
            order by p.unloc
            """)
    List<Port> search(@Param("q") String q);
}

