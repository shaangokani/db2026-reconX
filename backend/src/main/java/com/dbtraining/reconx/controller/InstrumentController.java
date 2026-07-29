package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.service.InstrumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping("/{symbol}")
    public Instrument getInstrumentBySymbol(@PathVariable String symbol) {
        return instrumentService.findBySymbol(symbol);
    }
}