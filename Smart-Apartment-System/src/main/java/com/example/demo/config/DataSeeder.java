package com.example.demo.config;

import com.example.demo.model.Unit;
import com.example.demo.repository.UnitRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UnitRepository units;

    public DataSeeder(UnitRepository units) { this.units = units; }

    @Override
    public void run(String... args) {
        if (units.count() > 0) return;
        List<Unit> list = new ArrayList<>();
        for (String block : List.of("A", "B", "C"))
            for (int floor = 1; floor <= 5; floor++)
                for (int n = 1; n <= 4; n++)
                    list.add(new Unit(block + "-" + floor + String.format("%02d", n)));
        units.saveAll(list);
    }
}