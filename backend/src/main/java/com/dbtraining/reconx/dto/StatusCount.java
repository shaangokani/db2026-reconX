package com.dbtraining.reconx.dto;

/** One row of the trades-grouped-by-status aggregate used by /v1/trades/summary. */
public record StatusCount(String status, long count) {}
